package com.suseoaa.projectoaa.ui.screen.main

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

// AGSL 真实的边缘畸变 Shader (水滴鱼眼透镜效应)
private const val WATER_DROP_SHADER = """
    uniform shader composable;
    uniform float2 center;
    uniform float2 size; // 气泡的宽高

    half4 main(float2 fragCoord) {
        float2 delta = fragCoord - center;
        // 将坐标标准化到气泡的椭圆空间内 [-1, 1]
        float2 normalized = delta / (size / 2.0);
        float distSq = normalized.x * normalized.x + normalized.y * normalized.y;
        
        // 仅在气泡内部产生折射
        if (distSq < 1.0) {
            if (distSq < 0.0001) {
                return composable.eval(center);
            }
            float dist = sqrt(distSq);
            
            // 基础放大倍数（中心区域平面的恒定放大）
            float centerScale = 1.06;
            float flatDist = dist / centerScale;
            
            // 边缘曲率权重：保证中心区域是平面，只有在非常靠近边缘时才急剧增加到 1.0
            float edgeWeight = pow(dist, 14.0);
            
            // 在中心平面（flatDist）与真实边界（dist）之间平滑混合
            float finalDist = mix(flatDist, dist, edgeWeight);
            
            float distortion = finalDist / dist; 
            float2 newDelta = delta * distortion;
            return composable.eval(center + newDelta);
        }
        return composable.eval(fragCoord);
    }
"""

actual fun Modifier.liquidGlassDistortion(
    isExpanded: Boolean,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    fallbackScaleX: Float,
    fallbackScaleY: Float,
    fallbackPivotX: Float,
    fallbackPivotY: Float
): Modifier = this.graphicsLayer {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isExpanded) {
        val shader = RuntimeShader(WATER_DROP_SHADER)
        shader.setFloatUniform("center", centerX, centerY)
        shader.setFloatUniform("size", width, height)
        
        renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
            shader, "composable"
        ).asComposeRenderEffect()
    } else if (isExpanded) {
        scaleX = fallbackScaleX
        scaleY = fallbackScaleY
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
            pivotFractionX = fallbackPivotX,
            pivotFractionY = fallbackPivotY
        )
    }
}
