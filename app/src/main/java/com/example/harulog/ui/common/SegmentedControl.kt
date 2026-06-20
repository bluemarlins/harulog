package com.example.harulog.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harulog.ui.theme.DarkBackground
import com.example.harulog.ui.theme.DarkSurface

@Composable
fun <T> SegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColorProvider: (@Composable (T) -> Color)? = null,
    labelProvider: @Composable (T) -> String
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    val containerColor = if (isDark) {
        DarkSurface
    } else {
        Color(0xFFEFF1F5) // Soft light gray background
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val totalWidth = maxWidth
            val itemCount = items.size
            val segmentWidth = if (itemCount > 0) totalWidth / itemCount else 0.dp

            // 탱글한 스프링 효과를 이용한 오프셋 애니메이션
            val slideOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "SegmentedControlSlide"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // 1. 가로로 미끄러지는 선택 하이라이터 칩
                if (itemCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = slideOffset)
                            .width(segmentWidth)
                            .fillMaxHeight()
                            .shadow(2.dp, RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF2D3748) else Color.White)
                    )
                }

                // 2. 터치 이벤트를 감지하고 라벨을 그리는 전면부 Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        val isSelected = item == selectedItem
                        val activeColor = activeColorProvider?.invoke(item) ?: MaterialTheme.colorScheme.primary

                        // 텍스트 활성화/비활성화 색상 부드러운 페이드 전환
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            animationSpec = tween(durationMillis = 200),
                            label = "SegmentedTextColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onItemSelect(item)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelProvider(item),
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
