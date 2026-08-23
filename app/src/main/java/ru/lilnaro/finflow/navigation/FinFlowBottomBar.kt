package ru.lilnaro.finflow.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackgroundSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimaryLight
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurface
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextMuted
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextPrimary

internal enum class FinFlowMainTab(
    val destination: FinFlowDestination,
    val label: String,
    val icon: FinFlowBottomIcon,
) {
    HOME(
        destination = FinFlowDestination.Home,
        label = "Главная",
        icon = FinFlowBottomIcon.HOME,
    ),
    TRANSACTIONS(
        destination = FinFlowDestination.Transactions,
        label = "Транзакции",
        icon = FinFlowBottomIcon.TRANSACTIONS,
    ),
    ASSISTANT(
        destination = FinFlowDestination.Assistant,
        label = "ИИ анализ",
        icon = FinFlowBottomIcon.ASSISTANT,
    ),
    ARCHIVE(
        destination = FinFlowDestination.Archive,
        label = "Архив",
        icon = FinFlowBottomIcon.ARCHIVE,
    ),
}

internal enum class FinFlowBottomIcon {
    HOME,
    TRANSACTIONS,
    ASSISTANT,
    ARCHIVE,
}

@Composable
internal fun FinFlowBottomBar(
    currentRoute: String?,
    onTabSelected: (FinFlowDestination) -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = FinFlowBackgroundSecondary.copy(
            alpha = 0.98f,
        ),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            HorizontalDivider(
                color = FinFlowBorder.copy(
                    alpha = 0.85f,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(
                        horizontal = 6.dp,
                    ),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BottomTabItem(
                    tab = FinFlowMainTab.HOME,
                    selected = currentRoute ==
                            FinFlowDestination.Home.route,
                    onClick = {
                        onTabSelected(
                            FinFlowDestination.Home,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                BottomTabItem(
                    tab = FinFlowMainTab.TRANSACTIONS,
                    selected = currentRoute ==
                            FinFlowDestination.Transactions.route,
                    onClick = {
                        onTabSelected(
                            FinFlowDestination.Transactions,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        onClick = onAddTransaction,
                        modifier = Modifier
                            .offset(
                                y = (-9).dp,
                            )
                            .size(58.dp),
                        color = FinFlowPrimary,
                        contentColor = FinFlowTextPrimary,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = FinFlowPrimaryLight.copy(
                                alpha = 0.85f,
                            ),
                        ),
                        shadowElevation = 10.dp,
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.Transparent,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                color = FinFlowTextPrimary,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                }

                BottomTabItem(
                    tab = FinFlowMainTab.ASSISTANT,
                    selected = currentRoute ==
                            FinFlowDestination.Assistant.route,
                    onClick = {
                        onTabSelected(
                            FinFlowDestination.Assistant,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )

                BottomTabItem(
                    tab = FinFlowMainTab.ARCHIVE,
                    selected = currentRoute ==
                            FinFlowDestination.Archive.route,
                    onClick = {
                        onTabSelected(
                            FinFlowDestination.Archive,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: FinFlowMainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
        contentColor = if (selected) {
            FinFlowPrimaryLight
        } else {
            FinFlowTextMuted
        },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 2.dp,
                vertical = 8.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = if (selected) {
                    FinFlowPrimary.copy(
                        alpha = 0.16f,
                    )
                } else {
                    Color.Transparent
                },
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BottomNavigationIcon(
                        icon = tab.icon,
                        color = if (selected) {
                            FinFlowPrimaryLight
                        } else {
                            FinFlowTextMuted
                        },
                    )
                }
            }

            Text(
                text = tab.label,
                modifier = Modifier.padding(
                    top = 3.dp,
                ),
                color = if (selected) {
                    FinFlowTextPrimary
                } else {
                    FinFlowTextMuted
                },
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun BottomNavigationIcon(
    icon: FinFlowBottomIcon,
    color: Color,
) {
    Canvas(
        modifier = Modifier.size(21.dp),
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val stroke = Stroke(
            width = strokeWidth,
        )

        when (icon) {
            FinFlowBottomIcon.HOME -> {
                val path = Path().apply {
                    moveTo(
                        size.width * 0.12f,
                        size.height * 0.48f,
                    )
                    lineTo(
                        size.width * 0.50f,
                        size.height * 0.16f,
                    )
                    lineTo(
                        size.width * 0.88f,
                        size.height * 0.48f,
                    )
                    lineTo(
                        size.width * 0.82f,
                        size.height * 0.48f,
                    )
                    lineTo(
                        size.width * 0.82f,
                        size.height * 0.86f,
                    )
                    lineTo(
                        size.width * 0.59f,
                        size.height * 0.86f,
                    )
                    lineTo(
                        size.width * 0.59f,
                        size.height * 0.63f,
                    )
                    lineTo(
                        size.width * 0.41f,
                        size.height * 0.63f,
                    )
                    lineTo(
                        size.width * 0.41f,
                        size.height * 0.86f,
                    )
                    lineTo(
                        size.width * 0.18f,
                        size.height * 0.86f,
                    )
                    lineTo(
                        size.width * 0.18f,
                        size.height * 0.48f,
                    )
                    close()
                }

                drawPath(
                    path = path,
                    color = color,
                    style = stroke,
                )
            }

            FinFlowBottomIcon.TRANSACTIONS -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        x = size.width * 0.18f,
                        y = size.height * 0.10f,
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width * 0.64f,
                        height = size.height * 0.80f,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        x = 3.dp.toPx(),
                        y = 3.dp.toPx(),
                    ),
                    style = stroke,
                )

                listOf(
                    0.34f,
                    0.50f,
                    0.66f,
                ).forEach { fraction ->
                    drawLine(
                        color = color,
                        start = Offset(
                            x = size.width * 0.32f,
                            y = size.height * fraction,
                        ),
                        end = Offset(
                            x = size.width * 0.68f,
                            y = size.height * fraction,
                        ),
                        strokeWidth = strokeWidth,
                    )
                }
            }

            FinFlowBottomIcon.ASSISTANT -> {
                val center = Offset(
                    x = size.width * 0.50f,
                    y = size.height * 0.48f,
                )

                val path = Path().apply {
                    moveTo(
                        center.x,
                        size.height * 0.08f,
                    )
                    lineTo(
                        size.width * 0.59f,
                        size.height * 0.38f,
                    )
                    lineTo(
                        size.width * 0.90f,
                        center.y,
                    )
                    lineTo(
                        size.width * 0.59f,
                        size.height * 0.58f,
                    )
                    lineTo(
                        center.x,
                        size.height * 0.90f,
                    )
                    lineTo(
                        size.width * 0.41f,
                        size.height * 0.58f,
                    )
                    lineTo(
                        size.width * 0.10f,
                        center.y,
                    )
                    lineTo(
                        size.width * 0.41f,
                        size.height * 0.38f,
                    )
                    close()
                }

                drawPath(
                    path = path,
                    color = color,
                    style = stroke,
                )
            }

            FinFlowBottomIcon.ARCHIVE -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.34f,
                    center = Offset(
                        x = size.width * 0.50f,
                        y = size.height * 0.50f,
                    ),
                    style = stroke,
                )

                drawLine(
                    color = color,
                    start = Offset(
                        x = size.width * 0.50f,
                        y = size.height * 0.50f,
                    ),
                    end = Offset(
                        x = size.width * 0.50f,
                        y = size.height * 0.29f,
                    ),
                    strokeWidth = strokeWidth,
                )

                drawLine(
                    color = color,
                    start = Offset(
                        x = size.width * 0.50f,
                        y = size.height * 0.50f,
                    ),
                    end = Offset(
                        x = size.width * 0.66f,
                        y = size.height * 0.58f,
                    ),
                    strokeWidth = strokeWidth,
                )
            }
        }
    }
}