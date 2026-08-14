package ru.lilnaro.finflow.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import ru.lilnaro.finflow.presentation.home.model.HomeAction
import ru.lilnaro.finflow.presentation.home.model.HomeUiState
import ru.lilnaro.finflow.presentation.home.model.HomeUiStatus
import ru.lilnaro.finflow.presentation.home.style.HomeColors
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeColors.Background),
    ) {
        AuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                ) {
                    HomeHeader(
                        greeting = uiState.greeting,
                        monthLabel = uiState.monthLabel,
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp),
                    )

                    key(uiState.status) {
                        var isVisible by remember {
                            mutableStateOf(false)
                        }

                        LaunchedEffect(uiState.status) {
                            isVisible = true
                        }

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 450,
                                ),
                            ) + slideInVertically(
                                animationSpec = tween(
                                    durationMillis = 450,
                                ),
                                initialOffsetY = { height ->
                                    height / 10
                                },
                            ),
                        ) {
                            when (uiState.status) {
                                HomeUiStatus.LOADING -> {
                                    HomeLoadingState()
                                }

                                HomeUiStatus.CONTENT -> {
                                    HomeContent(
                                        uiState = uiState,
                                        onAction = onAction,
                                    )
                                }

                                HomeUiStatus.NO_ACTIVE_MONTH -> {
                                    NoActiveMonthState(
                                        onCreateMonth = {
                                            onAction(
                                                HomeAction.NewMonthClicked,
                                            )
                                        },
                                        onOpenArchive = {
                                            onAction(
                                                HomeAction.ArchiveClicked,
                                            )
                                        },
                                    )
                                }

                                HomeUiStatus.ERROR -> {
                                    HomeErrorState(
                                        message = uiState.errorMessage,
                                        onRetry = {
                                            onAction(
                                                HomeAction.RetryClicked,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCloseMonthConfirmationVisible) {
            CloseMonthConfirmationDialog(
                isClosing = uiState.isClosingMonth,
                errorMessage = uiState.closeMonthErrorMessage,
                onConfirm = {
                    onAction(
                        HomeAction.CloseMonthConfirmed,
                    )
                },
                onCancel = {
                    onAction(
                        HomeAction.CloseMonthCancelled,
                    )
                },
            )
        }
    }
}

@Composable
private fun CloseMonthConfirmationDialog(
    isClosing: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isClosing) {
                onCancel()
            }
        },
        title = {
            Text(
                text = "Завершить текущий месяц?",
                color = HomeColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text =
                        "Чтобы начать новый финансовый месяц, текущий нужно завершить. Все его транзакции сохранятся и будут доступны в архиве.",
                    color = HomeColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (errorMessage != null) {
                    Spacer(
                        modifier = Modifier.height(12.dp),
                    )

                    Text(
                        text = errorMessage,
                        color = HomeColors.Expense,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isClosing,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HomeColors.Warning,
                    disabledContentColor =
                        HomeColors.TextMuted,
                ),
            ) {
                if (isClosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = HomeColors.Warning,
                        strokeWidth = 2.dp,
                    )

                    Spacer(
                        modifier = Modifier.size(8.dp),
                    )

                    Text(
                        text = "Завершаем...",
                    )
                } else {
                    Text(
                        text = "Завершить и продолжить",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                enabled = !isClosing,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = HomeColors.TextSecondary,
                    disabledContentColor =
                        HomeColors.TextMuted,
                ),
            ) {
                Text(
                    text = "Отмена",
                )
            }
        },
        containerColor = HomeColors.SurfaceElevated,
        titleContentColor = HomeColors.TextPrimary,
        textContentColor = HomeColors.TextSecondary,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun HomeHeader(
    greeting: String,
    monthLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "FinFlow",
                color = HomeColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            Text(
                text = greeting.ifBlank {
                    "Ваш финансовый обзор"
                },
                color = HomeColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (monthLabel.isNotBlank()) {
            Surface(
                color = HomeColors.SurfaceElevated.copy(
                    alpha = 0.88f,
                ),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(
                    width = 1.dp,
                    color = HomeColors.Border.copy(
                        alpha = 0.75f,
                    ),
                ),
            ) {
                Text(
                    text = monthLabel,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp,
                    ),
                    color = HomeColors.TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            16.dp,
        ),
    ) {
        BalanceHeroCard(
            initialBudget = uiState.initialBudget,
            currentBalance = uiState.currentBalance,
            balanceChangePercent =
                uiState.balanceChangePercent,
            budgetRemainingPercent =
                uiState.budgetRemainingPercent,
        )

        FinanceMetrics(
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense,
        )

        QuickActions(
            onAction = onAction,
        )

        Spacer(
            modifier = Modifier.height(12.dp),
        )
    }
}

@Composable
private fun BalanceHeroCard(
    initialBudget: BigDecimal,
    currentBalance: BigDecimal,
    balanceChangePercent: Double,
    budgetRemainingPercent: Double,
) {
    val progressTarget =
        (budgetRemainingPercent / 100.0)
            .coerceIn(
                minimumValue = 0.0,
                maximumValue = 1.0,
            )
            .toFloat()

    var animatedProgressTarget by remember {
        mutableStateOf(0f)
    }

    LaunchedEffect(progressTarget) {
        animatedProgressTarget = progressTarget
    }

    val animatedProgress by animateFloatAsState(
        targetValue = animatedProgressTarget,
        animationSpec = tween(
            durationMillis = 1_200,
        ),
        label = "budgetProgress",
    )

    val shape = MaterialTheme.shapes.extraLarge

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        HomeColors.HeroGradientStart,
                        HomeColors.HeroGradientEnd,
                    ),
                ),
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = HomeColors.Primary.copy(
                        alpha = 0.24f,
                    ),
                ),
                shape = shape,
            )
            .padding(22.dp),
    ) {
        Column {
            Text(
                text = "ДОСТУПНО",
                color = HomeColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            AutoSizingMoneyText(
                text = currentBalance.toRubleText(),
                color = HomeColors.TextPrimary,
                style = MaterialTheme.typography.displayMedium,
                maxFontSize = 36.sp,
                minFontSize = 18.sp,
            )

            Spacer(
                modifier = Modifier.height(6.dp),
            )

            Text(
                text = balanceChangePercent
                    .toBalanceChangeText(),
                color = balanceChangePercent
                    .toChangeColor(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.height(26.dp),
            )

            BudgetProgressBar(
                progress = animatedProgress,
            )

            Spacer(
                modifier = Modifier.height(10.dp),
            )

            Text(
                text = createBudgetProgressText(
                    initialBudget = initialBudget,
                    budgetRemainingPercent =
                        budgetRemainingPercent,
                ),
                color = HomeColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            Text(
                text = "Старт месяца: ${initialBudget.toRubleText()}",
                color = HomeColors.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BudgetProgressBar(
    progress: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(HomeColors.SurfaceSoft),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            HomeColors.Primary,
                            HomeColors.PrimaryLight,
                            HomeColors.Secondary,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun FinanceMetrics(
    totalIncome: BigDecimal,
    totalExpense: BigDecimal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            12.dp,
        ),
    ) {
        MetricCard(
            title = "Доходы",
            value = totalIncome.toIncomeText(),
            accent = HomeColors.Income,
            accentBackground = HomeColors.IncomeSoft,
            modifier = Modifier.weight(1f),
        )

        MetricCard(
            title = "Расходы",
            value = totalExpense.toExpenseText(),
            accent = HomeColors.Expense,
            accentBackground = HomeColors.ExpenseSoft,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    accent: Color,
    accentBackground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = HomeColors.Surface.copy(
            alpha = 0.90f,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = HomeColors.Border.copy(
                alpha = 0.70f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                color = accent,
                shape = CircleShape,
            ) {}

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            AutoSizingMoneyText(
                text = value,
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                maxFontSize = 20.sp,
                minFontSize = 12.sp,
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            Text(
                text = title,
                color = HomeColors.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(
                modifier = Modifier.height(2.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        color = accentBackground,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun QuickActions(
    onAction: (HomeAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            12.dp,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                12.dp,
            ),
        ) {
            QuickActionCard(
                marker = "↗",
                title = "Транзакции",
                subtitle = "Все операции",
                accent = HomeColors.Primary,
                accentBackground = HomeColors.PrimarySoft,
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction(
                        HomeAction.TransactionsClicked,
                    )
                },
            )

            QuickActionCard(
                marker = "✦",
                title = "ИИ-анализ",
                subtitle = "Разбор бюджета",
                accent = HomeColors.Secondary,
                accentBackground = HomeColors.SecondarySoft,
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction(
                        HomeAction.AssistantClicked,
                    )
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                12.dp,
            ),
        ) {
            QuickActionCard(
                marker = "+",
                title = "Новый месяц",
                subtitle = "Начать новый цикл",
                accent = HomeColors.PrimaryLight,
                accentBackground = HomeColors.PrimarySoft,
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction(
                        HomeAction.NewMonthClicked,
                    )
                },
            )

            QuickActionCard(
                marker = "◷",
                title = "Архив",
                subtitle = "История месяцев",
                accent = HomeColors.Warning,
                accentBackground = HomeColors.Warning.copy(
                    alpha = 0.12f,
                ),
                modifier = Modifier.weight(1f),
                onClick = {
                    onAction(
                        HomeAction.ArchiveClicked,
                    )
                },
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    marker: String,
    title: String,
    subtitle: String,
    accent: Color,
    accentBackground: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(122.dp)
            .clickable(
                onClick = onClick,
            ),
        color = HomeColors.Surface.copy(
            alpha = 0.90f,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = HomeColors.Border.copy(
                alpha = 0.70f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement =
                Arrangement.SpaceBetween,
        ) {
            Surface(
                color = accentBackground,
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = marker,
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = HomeColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(3.dp),
                )

                Text(
                    text = subtitle,
                    color = HomeColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun HomeLoadingState() {
    StateCard {
        CircularProgressIndicator(
            modifier = Modifier.size(38.dp),
            color = HomeColors.Primary,
            trackColor = HomeColors.SurfaceSoft,
            strokeWidth = 3.dp,
        )

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text = "Собираем финансовую картину",
            color = HomeColors.TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Text(
            text = "Проверяем текущий месяц и последние операции.",
            color = HomeColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoActiveMonthState(
    onCreateMonth: () -> Unit,
    onOpenArchive: () -> Unit,
) {
    StateCard {
        Surface(
            color = HomeColors.PrimarySoft,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    color = HomeColors.PrimaryLight,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text = "Начните финансовый месяц",
            color = HomeColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Text(
            text = "Задайте стартовый бюджет, и FinFlow начнёт собирать финансовую картину месяца.",
            color = HomeColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        PrimaryButton(
            text = "Создать месяц",
            onClick = onCreateMonth,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        TextButton(
            onClick = onOpenArchive,
            colors = ButtonDefaults.textButtonColors(
                contentColor = HomeColors.PrimaryLight,
            ),
        ) {
            Text(
                text = "Открыть архив",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomeErrorState(
    message: String?,
    onRetry: () -> Unit,
) {
    StateCard {
        Surface(
            color = HomeColors.ExpenseSoft,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = HomeColors.Expense,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text = "Не удалось загрузить данные",
            color = HomeColors.TextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Text(
            text = message
                ?: "Попробуйте повторить загрузку.",
            color = HomeColors.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        PrimaryButton(
            text = "Повторить",
            onClick = onRetry,
        )
    }
}

@Composable
private fun StateCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HomeColors.Surface.copy(
            alpha = 0.92f,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = HomeColors.Border.copy(
                alpha = 0.75f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 26.dp,
                    vertical = 36.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeColors.Primary,
            contentColor = HomeColors.TextPrimary,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 4.dp,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AuroraBackground() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "auroraBackground",
        )

    val movement by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 16_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auroraMovement",
    )

    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val radius =
            size.minDimension * 0.78f

        val primaryCenter = Offset(
            x = size.width *
                    (0.08f + 0.15f * movement),
            y = size.height *
                    (0.10f + 0.05f * movement),
        )

        val secondaryCenter = Offset(
            x = size.width *
                    (0.92f - 0.12f * movement),
            y = size.height *
                    (0.48f + 0.08f * movement),
        )

        val tertiaryCenter = Offset(
            x = size.width *
                    (0.25f + 0.16f * movement),
            y = size.height *
                    (0.92f - 0.08f * movement),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    HomeColors.GlowPrimary,
                    Color.Transparent,
                ),
                center = primaryCenter,
                radius = radius,
            ),
            radius = radius,
            center = primaryCenter,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    HomeColors.GlowSecondary,
                    Color.Transparent,
                ),
                center = secondaryCenter,
                radius = radius,
            ),
            radius = radius,
            center = secondaryCenter,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    HomeColors.GlowTertiary,
                    Color.Transparent,
                ),
                center = tertiaryCenter,
                radius = radius,
            ),
            radius = radius,
            center = tertiaryCenter,
        )
    }
}

@Composable
private fun AutoSizingMoneyText(
    text: String,
    color: Color,
    style: TextStyle,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    var currentFontSize by remember(
        text,
        maxFontSize,
    ) {
        mutableStateOf(maxFontSize)
    }

    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        style = style,
        fontSize = currentFontSize,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (
                textLayoutResult.didOverflowWidth &&
                currentFontSize.value >
                minFontSize.value
            ) {
                currentFontSize =
                    (currentFontSize.value - 1f)
                        .coerceAtLeast(
                            minFontSize.value,
                        )
                        .sp
            }
        },
    )
}

private fun BigDecimal.toRubleText(): String {
    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("ru-RU"),
    ).apply {
        isGroupingUsed = true
        minimumFractionDigits = 0
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }

    return "${formatter.format(this)}\u00A0₽"
}

private fun BigDecimal.toIncomeText(): String {
    if (compareTo(BigDecimal.ZERO) == 0) {
        return "0 ₽"
    }

    return "+${abs().toRubleText()}"
}

private fun BigDecimal.toExpenseText(): String {
    if (compareTo(BigDecimal.ZERO) == 0) {
        return "0 ₽"
    }

    return "−${abs().toRubleText()}"
}

private fun Double.toBalanceChangeText(): String {
    val formattedPercent =
        abs(this).toPercentText()

    return when {
        this > 0 -> {
            "↑ $formattedPercent к началу месяца"
        }

        this < 0 -> {
            "↓ $formattedPercent к началу месяца"
        }

        else -> {
            "Без изменений к началу месяца"
        }
    }
}

private fun Double.toChangeColor(): Color {
    return when {
        this > 0 -> HomeColors.Income
        this < 0 -> HomeColors.Expense
        else -> HomeColors.TextSecondary
    }
}

private fun Double.toPercentText(): String {
    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("ru-RU"),
    ).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
        roundingMode = RoundingMode.HALF_UP
    }

    return "${formatter.format(this)}%"
}

private fun createBudgetProgressText(
    initialBudget: BigDecimal,
    budgetRemainingPercent: Double,
): String {
    if (initialBudget.compareTo(BigDecimal.ZERO) == 0) {
        return "Стартовый бюджет не задан"
    }

    return when {
        budgetRemainingPercent < 0 -> {
            "Бюджет исчерпан"
        }

        budgetRemainingPercent > 100 -> {
            "${budgetRemainingPercent.toPercentText()} от стартового бюджета"
        }

        else -> {
            "${budgetRemainingPercent.toPercentText()} бюджета осталось"
        }
    }
}

@Preview(
    name = "FinFlow Aurora — Home",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun HomeScreenPreview() {
    FinFlowTheme {
        HomeScreen(
            uiState = HomeUiState(
                status = HomeUiStatus.CONTENT,
                greeting = "Добрый вечер",
                monthLabel = "Август",
                initialBudget =
                    BigDecimal("40000.00"),
                currentBalance =
                    BigDecimal("27400.00"),
                totalIncome =
                    BigDecimal("12300.00"),
                totalExpense =
                    BigDecimal("24900.00"),
                balanceChangePercent = -31.5,
                budgetRemainingPercent = 68.5,
            ),
            onAction = {},
        )
    }
}