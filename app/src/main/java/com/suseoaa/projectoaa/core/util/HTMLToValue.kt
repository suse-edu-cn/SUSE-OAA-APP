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

            // 步骤 A: 优先尝试查找 span.fraction
            var details = element.select("span.fraction").text().trim()

            // 步骤 B: 如果没有，尝试 p.list-group-item-text
            if (details.isEmpty()) {
                details = element.select("p.list-group-item-text").text().trim()
            }

            // 步骤 C: 最后的兜底，纯文本替换
            if (details.isEmpty()) {
                val fullText = element.text().trim()
                // 移除标题，移除可能存在的 "考试安排" 等多余标签文本
                details = fullText.replace(title, "")
                    .replace("考试安排", "")
                    .trim()
            }

            // 返回 "课程名###详情" 的格式
            "$title###$details"
        }

        if (strategy3.isNotEmpty()) return strategy3

        return emptyList()
    }
}