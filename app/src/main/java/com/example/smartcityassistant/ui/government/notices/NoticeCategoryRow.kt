package com.example.smartcityassistant.ui.government.notices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcityassistant.data.notices.NoticeCategory
import kotlinx.coroutines.launch

@Composable
fun NoticeCategoryRow(
    selectedCategory: NoticeCategory,
    onCategorySelected: (NoticeCategory) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedCategory) {
        val index = NoticeCategory.entries.indexOf(selectedCategory)
        if (index != -1) {
            listState.animateScrollToItem(index)
        }
    }

    val canScrollBackward = listState.canScrollBackward
    val canScrollForward = listState.canScrollForward

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.CenterStart
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = if (canScrollBackward) 36.dp else 8.dp,
                top = 4.dp,
                end = if (canScrollForward) 36.dp else 8.dp,
                bottom = 4.dp
            )
        ) {
            itemsIndexed(NoticeCategory.entries) { _, category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    enabled = enabled,
                    label = { Text(category.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0D2B4E),
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color(0xFF0D2B4E) else MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }

        if (canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = 8.dp)
                    .width(36.dp)
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0f)
                            )
                        )
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val targetIndex = maxOf(0, listState.firstVisibleItemIndex - 3)
                            listState.animateScrollToItem(targetIndex)
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 8.dp)
                    .width(36.dp)
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            val targetIndex = minOf(NoticeCategory.entries.size - 1, listState.firstVisibleItemIndex + 3)
                            listState.animateScrollToItem(targetIndex)
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "More categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
