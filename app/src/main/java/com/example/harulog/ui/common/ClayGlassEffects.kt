package com.example.harulog.ui.common

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// [1] Shader Sources
private const val GLASS_SHADER_SRC = """
    uniform float2 uSize;
    uniform float4 uColor;

    half4 main(float2 coord) {
        float4 col = uColor;
        
        // 1. Edge Fresnel Refraction Effect
        float2 center = uSize / 2.0;
        float2 distVec = abs(coord - center) / center;
        float edgeFactor = max(distVec.x, distVec.y);
        float fresnel = pow(edgeFactor, 4.0) * 0.15;
        col.rgb += fresnel;

        // 2. 3D Light Bevel Highlight (Top & Left) - Extended to 6.0px for high-res screens
        float highlight = 0.0;
        if (coord.y < 6.0 && coord.x < uSize.x - 6.0) {
            highlight = (1.0 - (coord.y / 6.0)) * 0.50;
        }
        if (coord.x < 6.0 && coord.y < uSize.y - 6.0) {
            highlight += (1.0 - (coord.x / 6.0)) * 0.30;
        }
        col.rgb += highlight;

        // 3. 3D Inner Bevel Shadow (Bottom & Right) - Extended to 6.0px
        float innerShadow = 0.0;
        if (coord.y > uSize.y - 6.0) {
            innerShadow = ((coord.y - (uSize.y - 6.0)) / 6.0) * 0.25;
        }
        if (coord.x > uSize.x - 6.0) {
            innerShadow += ((coord.x - (uSize.x - 6.0)) / 6.0) * 0.18;
        }
        col.rgb -= innerShadow;

        // 4. Cylinder Glass Reflect (Gloss)
        float normY = coord.y / uSize.y;
        float gloss = 0.0;
        if (normY < 0.35) {
            gloss = pow(1.0 - (normY / 0.35), 2.0) * 0.20;
        }
        col.rgb += gloss;

        // 5. Fine frosted noise - Increased to 0.065 to obscure overlapping background text
        float noise = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
        col.rgb += (noise - 0.5) * 0.065;

        return col;
    }
"""

private const val LIQUID_SHADER_SRC = """
    uniform float2 uCenter1;
    uniform float2 uCenter2;
    uniform float2 uHalfSize;
    uniform float uRadius;
    uniform float4 uColor;

    float sdRoundRect(float2 p, float2 b, float r) {
        float2 d = abs(p) - b + float2(r);
        return min(max(d.x, d.y), 0.0) + length(max(d, 0.0)) - r;
    }

    half4 main(float2 coord) {
        float d1 = sdRoundRect(coord - uCenter1, uHalfSize, uRadius);
        float d2 = sdRoundRect(coord - uCenter2, uHalfSize, uRadius);
        
        float g1 = exp(-max(d1, 0.0) * 0.18);
        float g2 = exp(-max(d2, 0.0) * 0.18);
        float total = g1 + g2;
        
        float threshold = 0.68;
        if (total > threshold) {
            float alpha = smoothstep(threshold, threshold + 0.04, total);
            float4 col = uColor * alpha;
            
            // Bevel highlight for liquid jelly capsule
            float d = min(d1, d2);
            if (d < 0.0) {
                float edgeDist = abs(d);
                if (edgeDist < 2.5) {
                    float innerHighlight = (1.0 - (edgeDist / 2.5)) * 0.30;
                    col.rgb += innerHighlight;
                }
            }
            return col;
        }
        return half4(0.0);
    }
"""

/**
 * 1. Neumorphic 2중 그림자 및 좌상단 틴트 글로우 (Style.STROKE를 써서 내부 투명성 보존)
 */
fun Modifier.clayGlassShadows(
    isDarkTheme: Boolean,
    cornerRadius: Dp = 24.dp
): Modifier = composed {
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    
    // 라이트 모드 화이트 배경에서도 유리의 굴절 반사광이 눈에 띄도록 스카이 블루 틴트 글로우 적용
    val glowColor = if (isDarkTheme) {
        android.graphics.Color.argb(160, 255, 255, 255)
    } else {
        android.graphics.Color.argb(220, 180, 215, 255)
    }

    this
        .drawBehind {
            val paint = android.graphics.Paint().apply {
                color = if (isDarkTheme) android.graphics.Color.WHITE else android.graphics.Color.argb(230, 214, 233, 255)
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                setShadowLayer(
                    28f,
                    -5f, -5f,
                    glowColor
                )
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRoundRect(
                    0f, 0f, size.width, size.height,
                    cornerRadiusPx, cornerRadiusPx,
                    paint
                )
            }
        }
        .shadow(
            elevation = 14.dp,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.10f else 0.14f),
            spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.10f else 0.14f)
        )
        .shadow(
            elevation = 5.dp,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.20f else 0.28f),
            spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.20f else 0.28f)
        )
}

/**
 * 2. 실시간 배경 흐림 레이어 (반투명 기본 컬러를 채운 뒤 흐림 효과 적용)
 */
fun Modifier.clayGlassBlurBase(
    isDarkTheme: Boolean,
    cornerRadius: Dp = 24.dp,
    blurRadius: Float = 25f
): Modifier = composed {
    val blurEffect = remember(blurRadius) {
        RenderEffect.createBlurEffect(
            blurRadius, blurRadius, android.graphics.Shader.TileMode.DECAL
        ).asComposeRenderEffect()
    }
    
    val baseColor = if (isDarkTheme) {
        Color(0xFF1E1E1C).copy(alpha = 0.82f)
    } else {
        Color(0xFFECF3FF).copy(alpha = 0.78f) // 라이트 틴트 반투명 강화
    }

    this
        .graphicsLayer {
            renderEffect = blurEffect
        }
        .clip(RoundedCornerShape(cornerRadius))
        .background(baseColor)
}

/**
 * 3. 3D 유리 광택 셰이더 및 극세 그라데이션 보더 레이어 (블러를 타지 않아 칼날같이 선명함)
 */
fun Modifier.clayGlassSpecular(
    isDarkTheme: Boolean,
    cornerRadius: Dp = 24.dp
): Modifier = composed {
    val glassShader = remember { RuntimeShader(GLASS_SHADER_SRC) }
    
    val specColor = if (isDarkTheme) {
        Color(0xFF262622).copy(alpha = 0.45f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.65f)
    }

    val glassBrush = remember(specColor) {
        object : ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size): android.graphics.Shader {
                glassShader.setFloatUniform("uSize", size.width, size.height)
                glassShader.setFloatUniform(
                    "uColor",
                    specColor.red,
                    specColor.green,
                    specColor.blue,
                    specColor.alpha
                )
                return glassShader
            }
        }
    }

    val borderBrush = remember(isDarkTheme) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = if (isDarkTheme) {
                listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.08f),
                    Color.Black.copy(alpha = 0.30f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.30f),
                    Color(0xFF8AB4F8).copy(alpha = 0.50f)
                )
            }
        )
    }

    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(glassBrush)
        .border(
            width = 0.8.dp,
            brush = borderBrush,
            shape = RoundedCornerShape(cornerRadius)
        )
}

/**
 * 하위 호환성을 위한 레거시 Modifier 조합 단축 함수
 */
fun Modifier.clayGlassBackground(
    isDarkTheme: Boolean,
    cornerRadius: Dp = 24.dp,
    blurRadius: Float = 25f
): Modifier {
    return this
        .clayGlassShadows(isDarkTheme, cornerRadius)
        .clayGlassBlurBase(isDarkTheme, cornerRadius, blurRadius)
        .clayGlassSpecular(isDarkTheme, cornerRadius)
}

/**
 * 배경 흐림 이펙트가 자식 콘텐츠(Text, Icons 등)나 3D 광택 셰이더 및 테두리선(Border)까지 침범하지 않도록,
 * 모든 요소를 논리적 드로잉 레이어로 완벽히 격리하여 입체감과 시인성을 극대화한 컨테이너 컴포저블입니다.
 */
@Composable
fun ClayGlassBox(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Float = 25f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
    ) {
        // [Layer 0] Neumorphic 그림자 및 투명 틴트 글로우 (블러 없음, 배경 비침 확보)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clayGlassShadows(isDarkTheme, cornerRadius)
        )

        // [Layer 1] 실시간 배경 블러 레이어 (블러 효과 적용)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clayGlassBlurBase(isDarkTheme, cornerRadius, blurRadius)
        )

        // [Layer 2] 3D 유리 광택 셰이더 및 테두리 레이어 (블러 없음, 선명도 100%)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clayGlassSpecular(isDarkTheme, cornerRadius)
        )
        
        // [Layer 3] 선명함을 100% 보존해야 하는 상위 텍스트/상호작용 콘텐츠 레이어
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

/**
 * 둥근 직사각형 SDF 기반 Metaball 알고리즘이 적용된 Liquid Indicator 캔버스 컴포저블입니다.
 */
@Composable
fun BoxWithConstraintsScope.LiquidIndicator(
    currentTab: Int,
    tabCount: Int,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val liquidShader = remember { RuntimeShader(LIQUID_SHADER_SRC) }
    
    val tabWidthPx = with(density) { (maxWidth / tabCount).toPx() }
    val heightPx = with(density) { maxHeight.toPx() }
    val targetX = tabWidthPx * currentTab + tabWidthPx / 2f
    
    val animatingX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = 220f
        ),
        label = "LiquidIndicatorAnim"
    )
    
    val indicatorColor = if (isDarkTheme) {
        Color(0xFF7E5EFF).copy(alpha = 0.20f)
    } else {
        Color(0xFF5E8BFF).copy(alpha = 0.22f)
    }
    
    val indicatorBrush = remember(animatingX, targetX, isDarkTheme) {
        object : ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size): android.graphics.Shader {
                val centerY = size.height / 2f
                liquidShader.setFloatUniform("uCenter1", animatingX, centerY)
                liquidShader.setFloatUniform("uCenter2", targetX, centerY)
                
                val halfW = (tabWidthPx * 0.80f) / 2f
                val halfH = (heightPx * 0.78f) / 2f
                liquidShader.setFloatUniform("uHalfSize", halfW, halfH)
                liquidShader.setFloatUniform("uRadius", with(density) { 16.dp.toPx() })
                
                liquidShader.setFloatUniform(
                    "uColor",
                    indicatorColor.red,
                    indicatorColor.green,
                    indicatorColor.blue,
                    indicatorColor.alpha
                )
                return liquidShader
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(brush = indicatorBrush)
    }
}
