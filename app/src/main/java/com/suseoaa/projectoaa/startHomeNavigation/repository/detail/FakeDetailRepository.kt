package com.suseoaa.projectoaa.navigation.repository.detail

import com.suseoaa.projectoaa.navigation.viewmodel.DetailBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * 详情仓库的 "Fake" 实现，用于开发和测试。
 * 它模拟了网络延迟和数据生成。
 */
@Singleton
class FakeDetailRepository @Inject constructor() : DetailRepository {

    override suspend fun getTaskDetails(token: String, taskId: String): Result<Pair<String, List<DetailBlock>>> {
        return withContext(Dispatchers.Default) {
            // 1. 模拟网络延迟
            delay(500)

            // 2. 检查假的 token (可选，但好习惯)
            if (token.isBlank()) {
                return@withContext Result.failure(Exception("认证失败：Token为空"))
            }

            // 3. 生成随机数据
            val (title, blocks) = generateRandomData(taskId)

            // 4. 返回成功
            Result.success(Pair(title, blocks))
        }
    }

    /**
     * 为待办事项生成随机的示例数据
     * (这个方法从 ViewModel 迁移到了这里)
     */
    private fun generateRandomData(taskId: String): Pair<String, List<DetailBlock>> {
        val random = Random(taskId.toInt()) // 使用 taskId 作为种子
        val title = "协会事务处理事项 #${taskId.toInt() + 1}"

        val assignees = listOf("张三", "李四", "王五", "赵六")
        val statuses = listOf("待处理", "进行中", "已完成", "已归档")

        val blocks = listOf(
            DetailBlock(
                title = "任务状态",
                content = statuses[random.nextInt(statuses.size)]
            ),
            DetailBlock(
                title = "负责人",
                content = assignees[random.nextInt(assignees.size)]
            ),
            DetailBlock(
                title = "详细描述",
                content = "这是一条随机生成的待办事项描述。\n任务ID: $taskId。\n" +
                        "请相关负责人尽快处理此事务，确保项目按时推进。" +
                        "此内容的长度是随机的，用于测试UI在不同文本长度下的表现。".repeat(
                            random.nextInt(
                                1,
                                4
                            )
                        )
            ),
            DetailBlock(
                title = "截止日期",
                content = "2025-12-31"
            ),
            DetailBlock(
                title = "附件",
                content = if (random.nextBoolean()) "附件${taskId}.pdf" else "无"
            )
        )

        return Pair(title, blocks)
    }
}