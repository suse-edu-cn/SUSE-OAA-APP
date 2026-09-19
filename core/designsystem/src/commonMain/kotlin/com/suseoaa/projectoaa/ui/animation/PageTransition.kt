package com.suseoaa.projectoaa.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * 全局统一的"整页转场"动画：一屏退到另一屏之后、之前，只有这一套构建块。
 *
 * ## 这次改动改了什么、为什么
 *
 * 栈顶正在进/出的那一屏（前景）整屏水平滑动，对应 iOS `UINavigationController`
 * push/pop 里最有辨识度的部分。次顶层被揭示或被盖住的那一屏（背景）**不做任何
 * 水平位移**——早先按 iOS 的视差退让做过一版让它也跟着小幅度左右滑动，但两层
 * 画面同时左右移动会让人觉得晃，所以背景层现在原地不动，只用变暗表示"退到后面
 * 一层"，视觉上更安静，也不会有两层画面各自滑动时对不齐的观感。
 *
 * 时长与缓动曲线：iOS 没有公开精确的贝塞尔控制点，这里用的 350ms + 减速缓动
 * 是业界公认、多方独立文献都引用的近似值（standard "ease" 减速曲线），不是
 * 逐像素还原的逆向工程数值——如果实机看下来速度感还是不对，把 [DurationMillis]
 * 和 [motionEasing] 调整到你想要的手感即可，其余四个方向会自动同步（见下）。
 *
 * ## 怎么解决"时快时慢"
 *
 * 全部改用固定时长的 [tween]，不再用弹簧。弹簧的收敛耗时和"位移振幅 / 判定阈值"
 * 的比值成正比，一次转场里混用缩放（振幅很小）、透明度（振幅到 1.0）、位移
 * （振幅是屏宽的量级）这种量级差很大的分量，各自收敛时间不一样，就会出现某个
 * 分量已经停了、另一个还在动的拖尾感，且这个耗时还会随起始状态（比如上一次转场
 * 有没有被打断）变化，摸不准规律。固定时长的 tween 收敛所需时间只取决于设定的
 * [DurationMillis]，跟振幅、起始状态都无关，同一个方向的转场每次实际耗时完全一致。
 *
 * ## 怎么解决"衔接突兀"
 *
 * `PullUpFeatureDrawer`（"常用功能"卡片）原来用的是另一套独立的弹簧参数
 * （阻尼 0.55、低刚度），跟这里的 tween 是两套完全不同的动画语言，一个用力学
 * 弹簧、一个用固定时长曲线，两者手感天然对不上。已经把那边的弹簧参数换成
 * 更贴近这里节奏的数值（见 `PullUpFeatureDrawer.kt`），但那个组件本身要支持
 * 手指拖拽跟手（需要按当前速度接续动画），所以还是保留弹簧机制而不是也换成
 * tween——tween 不能像弹簧那样把"手指松开时的速度"自然带入动画。
 *
 * ## 怎么复用
 *
 * 这套动画本身不依赖 `NavBackStackEntry`，就是普通的 [EnterTransition]/[ExitTransition]，
 * 任何用得到 `AnimatedVisibility`/`AnimatedContent` 的地方都可以直接调用：
 * - [foregroundEnter]/[foregroundExit]：栈顶正在进/出的那一屏，整屏滑入滑出
 * - [backgroundRecede]/[backgroundReturn]：退到次顶层、被揭示或被盖住的那一屏，
 *   原地不动，只做变暗
 */
object PageTransition {

    /** 整套转场的时长；所有分量共用同一个时长，保证同步收尾，不会有拖尾感。 */
    private const val DurationMillis = 350

    /** 近似 iOS 系统转场的减速曲线：起步快、收尾慢，不做弹簧回弹。 */
    private val motionEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /** 次顶层页面被盖住时的最暗透明度；不会淡到全透明，模拟半透明黑色遮罩的变暗效果。 */
    private const val BackgroundDimAlpha = 0.65f

    private fun <T> motionSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = DurationMillis, easing = motionEasing)

    // ==================== 前景：栈顶正在进 / 出的那一屏 ====================
    // 对应 iOS 里"当前正在跳转的那一屏"：整屏水平滑动，不做缩放也不淡入淡出，
    // 保持始终不透明——这正是 iOS 系统转场里最有辨识度的部分。

    /** push 时新页面从屏幕右侧整屏滑入，成为栈顶 */
    fun foregroundEnter(): EnterTransition =
        slideInHorizontally(animationSpec = motionSpec()) { fullWidth -> fullWidth }

    /** pop 时栈顶页面整屏向右滑出屏幕，离场 */
    fun foregroundExit(): ExitTransition =
        slideOutHorizontally(animationSpec = motionSpec()) { fullWidth -> fullWidth }

    // ==================== 背景：次顶层，被揭示或被盖住的那一屏 ====================
    // 不做任何水平位移，原地不动，只用变暗表示"退到后面一层"，
    // 避免两层画面一起左右移动时産生的晃动感。

    /** push 时原本的栈顶页面被新页面盖住，原地调暗，退到背景层 */
    fun backgroundRecede(): ExitTransition =
        fadeOut(targetAlpha = BackgroundDimAlpha, animationSpec = motionSpec())

    /** pop 时背景层的页面从调暗状态原地恢复到完整不透明度，重新成为栈顶 */
    fun backgroundReturn(): EnterTransition =
        fadeIn(initialAlpha = BackgroundDimAlpha, animationSpec = motionSpec())

    /**
     * 供 [com.suseoaa.projectoaa.ui.navigation.SharedNavHost] 里配合转场一起裁圆角的
     * `animateDp` 复用，避免圆角单独用 Compose 默认弹簧参数、跟这里的滑动/淡入淡出
     * 各走各的时长，又出现"这部分先收尾、那部分还在动"的不同步。
     */
    fun <T> sharedMotionSpec(): FiniteAnimationSpec<T> = motionSpec()
}
