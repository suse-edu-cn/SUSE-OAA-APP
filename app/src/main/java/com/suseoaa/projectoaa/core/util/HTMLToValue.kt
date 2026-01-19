package com.suseoaa.projectoaa.core.util

import org.jsoup.Jsoup

object HtmlParser {
    /**
     * 通用通知解析器
     */
    fun htmlParse(html: String): List<String> {
        val doc = Jsoup.parse(html)

        // 策略 1：获取课程更新信息
        val strategy1 = doc.select("div#kbDiv a.list-group-item span.title")
            .map { it.text().trim() }
        if (strategy1.isNotEmpty()) return strategy1

        // 策略 2：获取调课信息
        val strategy2 = doc.select("div#home a.list-group-item")
            .map { elements ->
                val time = elements.select("span.fraction").text().trim()
                val info = elements.attr("data-tkxx").trim()
                if (time.isNotEmpty()) "$time\n$info" else info
            }
        if (strategy2.isNotEmpty()) return strategy2

        // 策略 3: 获取考试信息
        val strategy3 = doc.select("div#exam a.list-group-item").mapNotNull { element ->
            val title = element.select("span.title").text().trim()
            if (title.isEmpty()) return@mapNotNull null

            var details = element.select("span.fraction").text().trim()
            if (details.isEmpty()) {
                details = element.select("p.list-group-item-text").text().trim()
            }
            if (details.isEmpty()) {
                val fullText = element.text().trim()
                details = fullText.replace(title, "")
                    .replace("考试安排", "")
                    .trim()
            }
            "$title###$details"
        }

        if (strategy3.isNotEmpty()) return strategy3

        return emptyList()
    }

    data class GradeDetail(
        val regular: String = "",
        val regularRatio: String = "",
        val final: String = "",
        val finalRatio: String = "",
        val experiment: String = "",
        val experimentRatio: String = ""
    )

    fun parseGradeDetail(html: String): GradeDetail {
        val doc = Jsoup.parse(html)
        val rows = doc.select("table#subtab tbody tr")

        var regular = ""
        var regularRatio = ""
        var finalScore = ""
        var finalRatio = ""
        var experimentScore = ""
        var experimentRatio = ""

        for (row in rows) {
            val cols = row.select("td")
            if (cols.size >= 3) {
                // 第一列：标题 (【 平时 】 / 【 实验 】)
                val title = cols[0].text().replace("【", "").replace("】", "").trim()
                // 第二列：比例 (40%)
                val ratio = cols[1].text().trim()
                // 第三列：分数 (91)
                val score = cols[2].text().trim()

                when {
                    title.contains("平时") -> {
                        regular = score
                        regularRatio = ratio
                    }
                    // 实验匹配逻辑
                    title.contains("实验") -> {
                        experimentScore = score
                        experimentRatio = ratio
                    }

                    title.contains("期末") || title.contains("补考") -> {
                        finalScore = score
                        finalRatio = ratio
                    }
                }
            }
        }
        return GradeDetail(
            regular, regularRatio,
            finalScore, finalRatio,
            experimentScore, experimentRatio
        )
    }
}