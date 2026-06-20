package com.example.harulog.ui.common

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
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem

                val itemBgColor = if (isSelected) {
                    if (isDark) Color(0xFF2D3748) else Color.White
                } else {
                    Color.Transparent
                }

                val activeColor = activeColorProvider?.invoke(item) ?: MaterialTheme.colorScheme.primary
                val itemTextColor = if (isSelected) {
                    activeColor
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }

                val shadowElevation = if (isSelected) 2.dp else 0.dp

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(shadowElevation, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(itemBgColor)
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
                        color = itemTextColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
