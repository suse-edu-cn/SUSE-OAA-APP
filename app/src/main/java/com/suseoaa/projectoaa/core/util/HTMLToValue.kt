package com.suseoaa.projectoaa.core.util

import org.jsoup.Jsoup

object HtmlParser {
    /**
     * 通用通知解析器
     * 目的：不管教务系统怎么变，我都要尽力挖出文字信息
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

        return emptyList()
    }
}