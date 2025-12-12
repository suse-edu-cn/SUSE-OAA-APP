package com.suseoaa.projectoaa.startHomeNavigation.viewmodel

import java.time.LocalDate
import kotlin.random.Random

// ==========================================
// 数据模型 (DTO)
// ==========================================
data class DailyFortune(
    val luckLevel: String,           // 运势等级
    val goodList: List<FortuneItem>, // 宜 (空=诸事不宜)
    val badList: List<FortuneItem>   // 忌 (空=万事皆宜)
)

data class FortuneItem(
    val title: String,
    val subtitle: String
)

// ==========================================
// 逻辑核心
// ==========================================
object FortuneLogic {

    // 运势等级池
    private val luckLevels = listOf(
        "大吉", "中吉", "小吉", "吉", "半吉", "末吉", "末小吉", // 吉类
        "凶", "小凶", "中凶", "大凶"                         // 凶类
    )

    // =========================================================================
    // 🟢 宜 - 题库 (55+ 条)
    // =========================================================================
    private val goodPool = listOf(
        // --- 💻 编程/开发 ---
        FortuneItem("刷算法", "AC 率 +50%"),
        FortuneItem("写文档", "思如泉涌"),
        FortuneItem("提交代码", "0 Error 0 Warning"),
        FortuneItem("重构", "代码如诗"),
        FortuneItem("CodeReview", "发现关键 Bug"),
        FortuneItem("摸鱼", "带薪休假"),
        FortuneItem("Debug", "一针见血"),
        FortuneItem("备份", "数据无价"),
        FortuneItem("学习新库", "醍醐灌顶"),
        FortuneItem("清理桌面", "心情舒畅"),
        FortuneItem("写注释", "利人利己"),
        FortuneItem("买键盘", "手感极佳"),
        FortuneItem("配环境", "一次成功"),
        FortuneItem("甚至", "没有 Bug"), // 玩梗：甚至
        FortuneItem("开源", "Star +100"),
        FortuneItem("推代码", "绿格子喜人"),
        FortuneItem("写SQL", "索引命中"),

        // --- 🎓 校园/学习 ---
        FortuneItem("复习", "过目不忘"),
        FortuneItem("早起", "精神百倍"),
        FortuneItem("背单词", "记忆力 Max"),
        FortuneItem("刷题", "如有神助"),
        FortuneItem("提问", "老师点赞"),
        FortuneItem("补觉", "梦里啥都有"),
        FortuneItem("泡图书馆", "效率翻倍"),
        FortuneItem("占座", "C位得手"),
        FortuneItem("考证", "必过无疑"),
        FortuneItem("做笔记", "逻辑清晰"),
        FortuneItem("交作业", "老师不看"),
        FortuneItem("体育课", "不用体测"),
        FortuneItem("食堂", "阿姨手不抖"),
        FortuneItem("取快递", "不用排队"),

        // --- 🏠 生活/日常 ---
        FortuneItem("约饭", "有人请客"),
        FortuneItem("减肥", "今日轻二斤"),
        FortuneItem("表白", "成功率激增"),
        FortuneItem("喝奶茶", "半糖去冰"),
        FortuneItem("打游戏", "十连胜"),
        FortuneItem("洗衣服", "阳光明媚"),
        FortuneItem("晒太阳", "合成维生素D"),
        FortuneItem("发呆", "大脑重启"),
        FortuneItem("撸猫", "治愈心灵"),
        FortuneItem("看电影", "剧情神作"),
        FortuneItem("剪头发", "甚至有点帅"),
        FortuneItem("整理", "找到私房钱"),
        FortuneItem("听歌", "随机全是爱"),
        FortuneItem("散步", "偶遇柯基"),
        FortuneItem("早睡", "皮肤变好"),
        FortuneItem("喝水", "吨吨吨"),
        FortuneItem("攒钱", "积少成多"),

        // --- 🔮 玄学/其他 ---
        FortuneItem("抽卡", "单抽出金"),
        FortuneItem("转发", "锦鲤附体"),
        FortuneItem("立Flag", "居然实现了"),
        FortuneItem("买彩票", "相信奇迹"),
        FortuneItem("冥想", "心如止水")
    )

    // =========================================================================
    // 🔴 忌 - 题库 (55+ 条)
    // =========================================================================
    private val badPool = listOf(
        // --- 💻 编程/开发 ---
        FortuneItem("熬夜", "发际线后移"),
        FortuneItem("立Flag", "秒被打脸"),
        FortuneItem("强行上线", "必有回滚"),
        FortuneItem("动老代码", "屎山崩塌"),
        FortuneItem("不写注释", "上帝都看不懂"),
        FortuneItem("重装系统", "环境配不好"),
        FortuneItem("直接推主", "冲突一大堆"),
        FortuneItem("不锁屏", "被改壁纸"),
        FortuneItem("删库", "跑路失败"),
        FortuneItem("写正则", "调一天"),
        FortuneItem("改需求", "越改越乱"),
        FortuneItem("外接屏", "接触不良"),
        FortuneItem("没有备份", "硬盘异响"),
        FortuneItem("拷贝代码", "漏了括号"),
        FortuneItem("演示", "当场翻车"),

        // --- 🎓 校园/学习 ---
        FortuneItem("通宵", "第二天废了"),
        FortuneItem("逃课", "必被点名"),
        FortuneItem("补作业", "全是错的"),
        FortuneItem("借钱", "有去无回"),
        FortuneItem("考试", "全是盲区"),
        FortuneItem("小组合作", "遇到摆烂王"),
        FortuneItem("没带伞", "倾盆大雨"),
        FortuneItem("忘带卡", "进不去门"),
        FortuneItem("上课", "手机响了"),
        FortuneItem("赶DDL", "网断了"),
        FortuneItem("选课", "系统崩了"),
        FortuneItem("吃夜宵", "必长痘"),

        // --- 🏠 生活/日常 ---
        FortuneItem("节食", "越减越肥"),
        FortuneItem("相亲", "遇到奇葩"),
        FortuneItem("迟到", "全勤奖没了"),
        FortuneItem("吵架", "两败俱伤"),
        FortuneItem("冲动消费", "这也是我？"),
        FortuneItem("理发", "丑哭自己"),
        FortuneItem("喝凉水", "塞牙"),
        FortuneItem("剧透", "被人打死"),
        FortuneItem("信星座", "那是巴纳姆效应"),
        FortuneItem("出门", "踩到水坑"),
        FortuneItem("做饭", "黑暗料理"),
        FortuneItem("网购", "买家秀感人"),
        FortuneItem("追剧", "结局烂尾"),
        FortuneItem("自拍", "怎么拍都丑"),
        FortuneItem("穿白鞋", "必被人踩"),
        FortuneItem("换手机", "屏碎人亡"),
        FortuneItem("赖床", "憋出内伤"),

        // --- 🔮 玄学/其他 ---
        FortuneItem("抽卡", "蓝天白云"),
        FortuneItem("硬撑", "身体要紧"),
        FortuneItem("八卦", "引火烧身"),
        FortuneItem("赌博", "倾家荡产")
    )

    /**
     * 生成今日运势
     * 规则：
     * 1. 种子 = 日期 + 用户ID，保证千人千面但单人单日固定。
     * 2. 吉类运势：不能出现"诸事不宜"(goodList不为空)，可出现"万事皆宜"(badList可为空)。
     * 3. 凶类运势：不能出现"万事皆宜"(badList不为空)，可出现"诸事不宜"(goodList可为空)。
     * 4. 列表数量：0-2 之间随机。
     */
    fun generateFortuneForToday(userId: String): DailyFortune {
        val today = LocalDate.now()
        // 核心：基于日期的唯一随机种子，混合用户ID
        val seed = today.toEpochDay() * 31 + userId.hashCode().toLong()
        val random = Random(seed)

        // 1. 随机运势等级
        val luck = luckLevels[random.nextInt(luckLevels.size)]

        // 2. 判断是大吉类还是凶类
        val isBadLuck = luck.contains("凶")

        val goodListCount: Int
        val badListCount: Int

        if (isBadLuck) {
            // === 凶类逻辑 ===
            // 忌列表：必须有内容 (1到2个)，不能"万事皆宜"
            badListCount = random.nextInt(1, 3)
            // 宜列表：可以是0 (诸事不宜)，也可以是1或2
            goodListCount = random.nextInt(0, 3)
        } else {
            // === 吉类逻辑 ===
            // 忌列表：可以是0 (万事皆宜)，也可以是1或2
            badListCount = random.nextInt(0, 3)
            // 宜列表：必须有内容 (1到2个)，不能"诸事不宜"
            goodListCount = random.nextInt(1, 3)
        }

        // 3. 从池中抽取不重复的项
        val finalGoodList = goodPool.shuffled(random).take(goodListCount)
        val finalBadList = badPool.shuffled(random).take(badListCount)

        return DailyFortune(
            luckLevel = luck,
            goodList = finalGoodList,
            badList = finalBadList
        )
    }
}