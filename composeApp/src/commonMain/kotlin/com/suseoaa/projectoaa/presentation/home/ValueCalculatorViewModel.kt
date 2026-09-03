package com.suseoaa.projectoaa.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suseoaa.projectoaa.shared.data.local.TokenManager
import com.suseoaa.projectoaa.shared.data.repository.ValueCalculatorRepository
import com.suseoaa.projectoaa.shared.database.ValueCalculatorItem
import com.suseoaa.projectoaa.shared.util.OaaClock
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

enum class SortType(val label: String) {
    PRICE_ASC("价格(升)"),
    PRICE_DESC("价格(降)"),
    DAYS_ASC("天数(升)"),
    DAYS_DESC("天数(降)"),
    DAILY_COST_ASC("日均(升)"),
    DAILY_COST_DESC("日均(降)")
}

class ValueCalculatorViewModel(
    private val repository: ValueCalculatorRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _sortType = MutableStateFlow(SortType.DAYS_ASC)
    val sortType: StateFlow<SortType> = _sortType.asStateFlow()

    private val _items = MutableStateFlow<List<ValueCalculatorItem>>(emptyList())
    val items: StateFlow<List<ValueCalculatorItem>> = _items.asStateFlow()

    init {
        loadInitialSortType()
        loadItems()
    }

    private fun loadInitialSortType() {
        viewModelScope.launch {
            tokenManager.assetSortTypeFlow.firstOrNull()?.let { savedName ->
                try {
                    _sortType.value = SortType.valueOf(savedName)
                } catch (e: Exception) {
                    // Fallback to default
                }
            }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.getAllItems()
                .catch { e ->
                    // 记录错误或处理
                }
                .combine(_sortType) { itemList, currentSortType ->
                    val today = OaaClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    
                    itemList.sortedWith(Comparator { a, b ->
                        val dateA = Instant.fromEpochMilliseconds(a.purchaseDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val daysA = if (dateA <= today) dateA.daysUntil(today) else 0
                        val actualDaysA = if (daysA == 0) 1 else daysA
                        val costA = a.price / actualDaysA

                        val dateB = Instant.fromEpochMilliseconds(b.purchaseDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val daysB = if (dateB <= today) dateB.daysUntil(today) else 0
                        val actualDaysB = if (daysB == 0) 1 else daysB
                        val costB = b.price / actualDaysB

                        when (currentSortType) {
                            SortType.PRICE_ASC -> a.price.compareTo(b.price)
                            SortType.PRICE_DESC -> b.price.compareTo(a.price)
                            SortType.DAYS_ASC -> daysA.compareTo(daysB)
                            SortType.DAYS_DESC -> daysB.compareTo(daysA)
                            SortType.DAILY_COST_ASC -> costA.compareTo(costB)
                            SortType.DAILY_COST_DESC -> costB.compareTo(costA)
                        }
                    })
                }
                .collect { sortedList ->
                    _items.value = sortedList
                }
        }
    }

    fun updateSortType(newSortType: SortType) {
        _sortType.value = newSortType
        viewModelScope.launch {
            tokenManager.saveAssetSortType(newSortType.name)
        }
    }

    fun saveItem(itemName: String, price: Double, purchaseDateMillis: Long) {
        viewModelScope.launch {
            val createdAtMillis = OaaClock.now().toEpochMilliseconds()
            repository.insertItem(
                itemName = itemName,
                price = price,
                purchaseDateMillis = purchaseDateMillis,
                createdAtMillis = createdAtMillis
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }
}
