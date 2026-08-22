package ru.lilnaro.finflow.presentation.archive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.presentation.archive.model.ArchiveAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveCategoryBreakdownUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsAction
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsUiStatus
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthTransactionUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthUiModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiState
import ru.lilnaro.finflow.presentation.archive.model.ArchiveUiStatus
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowTertiary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowExpense
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowIncome
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimaryLight
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurface
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurfaceElevated
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurfaceSoft
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextMuted
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTheme

@Composable
fun ArchiveScreen(
    uiState: ArchiveUiState,
    onAction: (ArchiveAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                FinFlowBackground,
            ),
    ) {
        ArchiveAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 20.dp,
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(
                            max = 560.dp,
                        ),
                ) {
                    ArchiveHeader(
                        onBackClick = {
                            onAction(
                                ArchiveAction.BackClicked,
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp),
                    )

                    when (uiState.status) {
                        ArchiveUiStatus.LOADING -> {
                            ArchiveLoadingState(
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveUiStatus.EMPTY -> {
                            ArchiveEmptyState(
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveUiStatus.CONTENT -> {
                            ArchiveContent(
                                months =
                                    uiState.months,
                                onMonthClick = { monthId ->
                                    onAction(
                                        ArchiveAction.MonthClicked(
                                            monthId = monthId,
                                        ),
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveUiStatus.ERROR -> {
                            ArchiveErrorState(
                                message =
                                    uiState.errorMessage,
                                onRetry = {
                                    onAction(
                                        ArchiveAction
                                            .RetryClicked,
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveMonthDetailsScreen(
    uiState: ArchiveMonthDetailsUiState,
    onAction: (ArchiveMonthDetailsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                FinFlowBackground,
            ),
    ) {
        ArchiveAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 20.dp,
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
            ) {
                Spacer(
                    modifier = Modifier.height(12.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(
                            max = 560.dp,
                        ),
                ) {
                    ArchiveMonthDetailsHeader(
                        monthLabel =
                            uiState.monthLabel
                                .ifBlank {
                                    "Архивный месяц"
                                },
                        onBackClick = {
                            onAction(
                                ArchiveMonthDetailsAction
                                    .BackClicked,
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp),
                    )

                    when (uiState.status) {
                        ArchiveMonthDetailsUiStatus.LOADING -> {
                            ArchiveLoadingState(
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveMonthDetailsUiStatus.CONTENT -> {
                            ArchiveMonthDetailsContent(
                                uiState = uiState,
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveMonthDetailsUiStatus.NOT_FOUND -> {
                            ArchiveMonthNotFoundState(
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }

                        ArchiveMonthDetailsUiStatus.ERROR -> {
                            ArchiveErrorState(
                                message =
                                    uiState.errorMessage,
                                onRetry = {
                                    onAction(
                                        ArchiveMonthDetailsAction
                                            .RetryClicked,
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveMonthDetailsHeader(
    monthLabel: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp),
            color =
                FinFlowSurfaceElevated.copy(
                    alpha = 0.90f,
                ),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = FinFlowBorder,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    text = "←",
                    modifier = Modifier.offset(
                        x = (-1).dp,
                        y = (-1).dp,
                    ),
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 14.dp,
                ),
        ) {
            Text(
                text = monthLabel,
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Text(
                text = "Финансовый отчёт месяца",
                color = FinFlowTextMuted,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )
        }

        Surface(
            color =
                FinFlowIncome.copy(
                    alpha = 0.12f,
                ),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color =
                    FinFlowIncome.copy(
                        alpha = 0.30f,
                    ),
            ),
        ) {
            Text(
                text = "✓",
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
                color = FinFlowIncome,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveMonthDetailsContent(
    uiState: ArchiveMonthDetailsUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
    ) {
        item(
            key = "month_balance",
        ) {
            ArchiveFinalBalanceCard(
                uiState = uiState,
            )
        }

        item(
            key = "month_income_expense",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp),
            ) {
                ArchiveAmountStatCard(
                    title = "Доходы",
                    amountText =
                        uiState.totalIncome
                            .toIncomeText(),
                    accent = FinFlowIncome,
                    modifier = Modifier.weight(1f),
                )

                ArchiveAmountStatCard(
                    title = "Расходы",
                    amountText =
                        uiState.totalExpense
                            .toExpenseText(),
                    accent = FinFlowExpense,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(
            key = "month_overview",
        ) {
            ArchiveMonthOverviewCard(
                uiState = uiState,
            )
        }

        item(
            key = "income_breakdown",
        ) {
            ArchiveCategoryBreakdownCard(
                title = "Структура доходов",
                totalLabel = "Всего доходов",
                emptyMessage =
                    "В этом месяце не было доходов.",
                categories =
                    uiState.incomeBreakdown,
                totalAmount =
                    uiState.totalIncome,
                type = TransactionType.INCOME,
            )
        }

        item(
            key = "expense_breakdown",
        ) {
            ArchiveCategoryBreakdownCard(
                title = "Структура расходов",
                totalLabel = "Всего расходов",
                emptyMessage =
                    "В этом месяце не было расходов.",
                categories =
                    uiState.expenseBreakdown,
                totalAmount =
                    uiState.totalExpense,
                type = TransactionType.EXPENSE,
            )
        }

        item(
            key = "transactions_title",
        ) {
            Column {
                Text(
                    text = "Операции месяца",
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(3.dp),
                )

                Text(
                    text =
                        uiState.transactionCount
                            .toOperationCountText(),
                    color = FinFlowTextMuted,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }
        }

        if (uiState.transactions.isEmpty()) {
            item(
                key = "transactions_empty",
            ) {
                ArchiveTransactionsEmptyCard()
            }
        } else {
            items(
                items = uiState.transactions,
                key = { transaction ->
                    transaction.id
                },
            ) { transaction ->
                ArchiveTransactionCard(
                    transaction = transaction,
                )
            }
        }

        item(
            key = "month_history_note",
        ) {
            ArchiveMonthHistoryCard()
        }

        item(
            key = "month_bottom_space",
        ) {
            Spacer(
                modifier = Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun ArchiveFinalBalanceCard(
    uiState: ArchiveMonthDetailsUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowPrimary.copy(
                alpha = 0.11f,
            ),
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowPrimary.copy(
                    alpha = 0.30f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            Text(
                text = "ИТОГОВЫЙ БАЛАНС",
                color = FinFlowPrimaryLight,
                style =
                    MaterialTheme.typography
                        .labelSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            ArchiveAutoSizingMoneyText(
                text =
                    uiState.finalBalance
                        .toRubleText(),
                color = FinFlowTextPrimary,
                maxFontSize = 32.sp,
                minFontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.height(14.dp),
            )

            Text(
                text =
                    "Стартовый бюджет: ${uiState.initialBudget.toRubleText()}",
                color = FinFlowTextSecondary,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
            )
        }
    }
}

@Composable
private fun ArchiveAmountStatCard(
    title: String,
    amountText: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color =
            accent.copy(
                alpha = 0.09f,
            ),
        shape =
            MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color =
                accent.copy(
                    alpha = 0.24f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                color = FinFlowTextMuted,
                style =
                    MaterialTheme.typography
                        .labelMedium,
            )

            Spacer(
                modifier = Modifier.height(6.dp),
            )

            ArchiveAutoSizingMoneyText(
                text = amountText,
                color = accent,
                maxFontSize = 18.sp,
                minFontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ArchiveMonthOverviewCard(
    uiState: ArchiveMonthDetailsUiState,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurface.copy(
                alpha = 0.92f,
            ),
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.75f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = "Период и активность",
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(14.dp),
            )

            ArchivePeriodRow(
                title = "Начат",
                value =
                    uiState.startedAtMillis
                        .toDateText(),
                accent = FinFlowPrimaryLight,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            ArchivePeriodRow(
                title = "Завершён",
                value =
                    uiState.closedAtMillis
                        ?.toDateText()
                        ?: "Дата недоступна",
                accent = FinFlowIncome,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            ArchivePeriodRow(
                title = "Операций",
                value =
                    uiState.transactionCount
                        .toString(),
                accent = FinFlowPrimary,
            )
        }
    }
}

@Composable
private fun ArchiveCategoryBreakdownCard(
    title: String,
    totalLabel: String,
    emptyMessage: String,
    categories: List<ArchiveCategoryBreakdownUiModel>,
    totalAmount: BigDecimal,
    type: TransactionType,
) {
    val accent =
        when (type) {
            TransactionType.INCOME -> FinFlowIncome
            TransactionType.EXPENSE -> FinFlowExpense
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurface.copy(
                alpha = 0.92f,
            ),
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.75f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                text = title,
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(3.dp),
            )

            Text(
                text =
                    "$totalLabel: ${totalAmount.toRubleText()}",
                color = FinFlowTextMuted,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )

            Spacer(
                modifier = Modifier.height(14.dp),
            )

            if (categories.isEmpty()) {
                Text(
                    text = emptyMessage,
                    color = FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                )
            } else {
                categories.forEachIndexed {
                        index,
                        category,
                    ->
                    ArchiveCategoryBreakdownRow(
                        category = category,
                        type = type,
                        accent = accent,
                    )

                    if (index != categories.lastIndex) {
                        Spacer(
                            modifier =
                                Modifier.height(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveCategoryBreakdownRow(
    category: ArchiveCategoryBreakdownUiModel,
    type: TransactionType,
    accent: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = category.categoryName,
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text =
                        "${category.transactionCount} · ${category.sharePercent.toPercentText()}",
                    color = FinFlowTextMuted,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }

            Text(
                text =
                    category.amount
                        .toSignedRubleText(
                            type = type,
                        ),
                color = accent,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    FinFlowSurfaceSoft,
                    CircleShape,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction =
                            (category.sharePercent / 100.0)
                                .coerceIn(
                                    0.0,
                                    1.0,
                                )
                                .toFloat(),
                    )
                    .height(5.dp)
                    .background(
                        accent.copy(
                            alpha = 0.72f,
                        ),
                        CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun ArchiveTransactionCard(
    transaction: ArchiveMonthTransactionUiModel,
) {
    val accent =
        when (transaction.type) {
            TransactionType.INCOME -> FinFlowIncome
            TransactionType.EXPENSE -> FinFlowExpense
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurface.copy(
                alpha = 0.90f,
            ),
        shape =
            MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.65f,
                ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color =
                    accent.copy(
                        alpha = 0.12f,
                    ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Text(
                        text =
                            if (
                                transaction.type ==
                                TransactionType.INCOME
                            ) {
                                "+"
                            } else {
                                "−"
                            },
                        color = accent,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(12.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = transaction.categoryName,
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )

                Spacer(
                    modifier = Modifier.height(3.dp),
                )

                Text(
                    text =
                        transaction.createdAtMillis
                            .toTransactionDateText(),
                    color = FinFlowTextMuted,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )

                if (transaction.note.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(4.dp),
                    )

                    Text(
                        text = transaction.note,
                        color = FinFlowTextSecondary,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        maxLines = 2,
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(10.dp),
            )

            Text(
                text =
                    transaction.amount
                        .toSignedRubleText(
                            type = transaction.type,
                        ),
                color = accent,
                style =
                    MaterialTheme.typography
                        .bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ArchiveTransactionsEmptyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurface.copy(
                alpha = 0.90f,
            ),
        shape =
            MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.65f,
                ),
        ),
    ) {
        Text(
            text =
                "В этом финансовом месяце не было операций.",
            modifier = Modifier.padding(16.dp),
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
        )
    }
}

@Composable
private fun ArchivePeriodRow(
    title: String,
    value: String,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(8.dp),
            color = accent,
            shape = CircleShape,
        ) {}

        Spacer(
            modifier = Modifier.size(10.dp),
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
        )

        Text(
            text = value,
            color = FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ArchiveMonthHistoryCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurfaceElevated.copy(
                alpha = 0.82f,
            ),
        shape =
            MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.65f,
                ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                color =
                    FinFlowPrimary.copy(
                        alpha = 0.14f,
                    ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Text(
                        text = "i",
                        color =
                            FinFlowPrimaryLight,
                        style =
                            MaterialTheme.typography
                                .titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(12.dp),
            )

            Column {
                Text(
                    text = "История месяца сохранена",
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(3.dp),
                )

                Text(
                    text =
                        "Эти данные будут доступны FinFlow для будущих отчётов и локального финансового анализа.",
                    color = FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ArchiveMonthNotFoundState(
    modifier: Modifier = Modifier,
) {
    ArchiveCenteredState(
        modifier = modifier,
    ) {
        Surface(
            color =
                FinFlowSurfaceSoft,
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    text = "?",
                    color = FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text = "Месяц не найден",
            color = FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight =
                FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Text(
            text =
                "Этот финансовый месяц отсутствует в архиве.",
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArchiveHeader(
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp),
            color =
                FinFlowSurfaceElevated.copy(
                    alpha = 0.90f,
                ),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = FinFlowBorder,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    text = "←",
                    modifier = Modifier.offset(
                        x = (-1).dp,
                        y = (-1).dp,
                    ),
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 14.dp,
                ),
        ) {
            Text(
                text = "Архив",
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    "Завершённые финансовые месяцы",
                color = FinFlowTextMuted,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )
        }

        Surface(
            color =
                FinFlowIncome.copy(
                    alpha = 0.12f,
                ),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color =
                    FinFlowIncome.copy(
                        alpha = 0.30f,
                    ),
            ),
        ) {
            Text(
                text = "◷",
                modifier = Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
                color = FinFlowIncome,
                style =
                    MaterialTheme.typography
                        .titleMedium,
            )
        }
    }
}

@Composable
private fun ArchiveContent(
    months: List<ArchiveMonthUiModel>,
    onMonthClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
    ) {
        item(
            key = "archive_summary",
        ) {
            ArchiveSummaryCard(
                monthCount = months.size,
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp),
            )
        }

        items(
            items = months,
            key = { month ->
                month.id
            },
        ) { month ->
            ArchiveMonthCard(
                month = month,
                onClick = {
                    onMonthClick(
                        month.id,
                    )
                },
            )
        }

        item(
            key = "bottom_space",
        ) {
            Spacer(
                modifier =
                    Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun ArchiveSummaryCard(
    monthCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowPrimary.copy(
                alpha = 0.10f,
            ),
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowPrimary.copy(
                    alpha = 0.28f,
                ),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Финансовая история",
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold,
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp),
                )

                Text(
                    text =
                        "Закрытые месяцы сохраняются в Room",
                    color = FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }

            Surface(
                color =
                    FinFlowSurfaceElevated,
                shape = CircleShape,
            ) {
                Text(
                    text = monthCount.toString(),
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 9.dp,
                    ),
                    color = FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ArchiveMonthCard(
    month: ArchiveMonthUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color =
            FinFlowSurface.copy(
                alpha = 0.92f,
            ),
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.75f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f),
                ) {
                    Text(
                        text = month.monthLabel,
                        color =
                            FinFlowTextPrimary,
                        style =
                            MaterialTheme.typography
                                .titleLarge,
                        fontWeight =
                            FontWeight.SemiBold,
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp),
                    )

                    Text(
                        text =
                            month.closedAtMillis
                                .toClosedDateText(),
                        color =
                            FinFlowTextMuted,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                    )
                }

                Surface(
                    color =
                        FinFlowIncome.copy(
                            alpha = 0.12f,
                        ),
                    shape = CircleShape,
                ) {
                    Text(
                        text = "Завершён",
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp,
                                vertical = 7.dp,
                            ),
                        color = FinFlowIncome,
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        fontWeight =
                            FontWeight.SemiBold,
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        FinFlowBorder.copy(
                            alpha = 0.55f,
                        ),
                    ),
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp),
            )

            Text(
                text = "СТАРТОВЫЙ БЮДЖЕТ",
                color = FinFlowTextMuted,
                style =
                    MaterialTheme.typography
                        .labelSmall,
                fontWeight =
                    FontWeight.SemiBold,
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp),
            )

            Text(
                text =
                    month.initialBudget
                        .toRubleText(),
                color =
                    FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp),
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(7.dp),
                    color = FinFlowPrimaryLight,
                    shape = CircleShape,
                ) {}

                Spacer(
                    modifier =
                        Modifier.size(8.dp),
                )

                Text(
                    text =
                        "Начат ${month.startedAtMillis.toDateText()}",
                    color =
                        FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End,
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Text(
                    text = "Подробнее",
                    color = FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .labelLarge,
                    fontWeight =
                        FontWeight.SemiBold,
                )

                Spacer(
                    modifier =
                        Modifier.size(6.dp),
                )

                Text(
                    text = "›",
                    color = FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ArchiveLoadingState(
    modifier: Modifier = Modifier,
) {
    ArchiveCenteredState(
        modifier = modifier,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(38.dp),
            color = FinFlowPrimary,
            trackColor = FinFlowSurfaceSoft,
            strokeWidth = 3.dp,
        )

        Spacer(
            modifier = Modifier.height(18.dp),
        )

        Text(
            text = "Загружаем архив",
            color = FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight =
                FontWeight.SemiBold,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Text(
            text =
                "Ищем завершённые финансовые месяцы.",
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArchiveEmptyState(
    modifier: Modifier = Modifier,
) {
    ArchiveCenteredState(
        modifier = modifier,
    ) {
        Surface(
            color =
                FinFlowPrimary.copy(
                    alpha = 0.12f,
                ),
            shape = CircleShape,
        ) {
            Box(
                modifier =
                    Modifier.size(58.dp),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    text = "◷",
                    color =
                        FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text = "Архив пока пуст",
            color = FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight =
                FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Text(
            text =
                "После завершения финансового месяца он появится здесь.",
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArchiveErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ArchiveCenteredState(
        modifier = modifier,
    ) {
        Surface(
            color =
                FinFlowSurfaceSoft,
            shape = CircleShape,
        ) {
            Box(
                modifier =
                    Modifier.size(58.dp),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color =
                        FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp),
        )

        Text(
            text =
                "Не удалось открыть архив",
            color = FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight =
                FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Text(
            text =
                message
                    ?: "Попробуйте загрузить данные ещё раз.",
            color = FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(22.dp),
        )

        Button(
            onClick = onRetry,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        FinFlowPrimary,
                    contentColor =
                        FinFlowTextPrimary,
                ),
        ) {
            Text(
                text = "Повторить",
            )
        }
    }
}

@Composable
private fun ArchiveCenteredState(
    modifier: Modifier = Modifier,
    content:
    @Composable
    androidx.compose.foundation.layout
    .ColumnScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier.fillMaxWidth(),
            color =
                FinFlowSurface.copy(
                    alpha = 0.92f,
                ),
            shape =
                MaterialTheme.shapes
                    .extraLarge,
            border = BorderStroke(
                width = 1.dp,
                color =
                    FinFlowBorder.copy(
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
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun ArchiveAuroraBackground() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label =
                "archiveAurora",
        )

    val movement by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    durationMillis =
                        16_000,
                    easing =
                        LinearEasing,
                ),
                repeatMode =
                    RepeatMode.Reverse,
            ),
        label =
            "archiveAuroraMovement",
    )

    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val radius =
            size.minDimension * 0.78f

        val primaryCenter =
            Offset(
                x =
                    size.width *
                            (
                                    0.08f +
                                            0.15f *
                                            movement
                                    ),
                y =
                    size.height *
                            (
                                    0.10f +
                                            0.05f *
                                            movement
                                    ),
            )

        val secondaryCenter =
            Offset(
                x =
                    size.width *
                            (
                                    0.92f -
                                            0.12f *
                                            movement
                                    ),
                y =
                    size.height *
                            (
                                    0.48f +
                                            0.08f *
                                            movement
                                    ),
            )

        val tertiaryCenter =
            Offset(
                x =
                    size.width *
                            (
                                    0.25f +
                                            0.16f *
                                            movement
                                    ),
                y =
                    size.height *
                            (
                                    0.92f -
                                            0.08f *
                                            movement
                                    ),
            )

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        FinFlowGlowPrimary,
                        Color.Transparent,
                    ),
                    center =
                        primaryCenter,
                    radius = radius,
                ),
            radius = radius,
            center = primaryCenter,
        )

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        FinFlowGlowSecondary,
                        Color.Transparent,
                    ),
                    center =
                        secondaryCenter,
                    radius = radius,
                ),
            radius = radius,
            center = secondaryCenter,
        )

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(
                        FinFlowGlowTertiary,
                        Color.Transparent,
                    ),
                    center =
                        tertiaryCenter,
                    radius = radius,
                ),
            radius = radius,
            center = tertiaryCenter,
        )
    }
}

@Composable
private fun ArchiveAutoSizingMoneyText(
    text: String,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    var currentFontSize by
    remember(
        text,
        maxFontSize,
    ) {
        mutableStateOf(maxFontSize)
    }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = currentFontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (
                result.didOverflowWidth &&
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
    val formatter =
        NumberFormat.getNumberInstance(
            Locale.forLanguageTag(
                "ru-RU",
            ),
        ).apply {
            isGroupingUsed = true
            minimumFractionDigits = 0
            maximumFractionDigits = 2
            roundingMode =
                RoundingMode.HALF_UP
        }

    return "${formatter.format(this)}\u00A0₽"
}

private fun Long.toDateText(): String {
    return DATE_FORMAT.format(
        Date(this),
    )
}

private fun Long?.toClosedDateText(): String {
    val value =
        this
            ?: return "Дата завершения недоступна"

    return "Завершён ${value.toDateText()}"
}

private fun BigDecimal.toIncomeText(): String {
    if (compareTo(BigDecimal.ZERO) == 0) {
        return "0\u00A0₽"
    }

    return "+${abs().toRubleText()}"
}

private fun BigDecimal.toExpenseText(): String {
    if (compareTo(BigDecimal.ZERO) == 0) {
        return "0\u00A0₽"
    }

    return "−${abs().toRubleText()}"
}

private fun BigDecimal.toSignedRubleText(
    type: TransactionType,
): String {
    return when (type) {
        TransactionType.INCOME -> toIncomeText()
        TransactionType.EXPENSE -> toExpenseText()
    }
}

private fun Double.toPercentText(): String {
    val formatter =
        NumberFormat.getNumberInstance(
            Locale.forLanguageTag(
                "ru-RU",
            ),
        ).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 1
            roundingMode = RoundingMode.HALF_UP
        }

    return "${formatter.format(this)}%"
}

private fun Int.toOperationCountText(): String {
    val lastTwoDigits = this % 100
    val lastDigit = this % 10

    val word =
        when {
            lastTwoDigits in 11..14 -> "операций"
            lastDigit == 1 -> "операция"
            lastDigit in 2..4 -> "операции"
            else -> "операций"
        }

    return "$this $word"
}

private fun Long.toTransactionDateText(): String {
    return TRANSACTION_DATE_FORMAT.format(
        Date(this),
    )
}

private val DATE_FORMAT =
    SimpleDateFormat(
        "d MMMM yyyy",
        Locale.forLanguageTag(
            "ru-RU",
        ),
    )

private val TRANSACTION_DATE_FORMAT =
    SimpleDateFormat(
        "d MMMM, HH:mm",
        Locale.forLanguageTag(
            "ru-RU",
        ),
    )

@Preview(
    name = "FinFlow Aurora — Archive",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun ArchiveScreenPreview() {
    FinFlowTheme {
        ArchiveScreen(
            uiState =
                ArchiveUiState(
                    status =
                        ArchiveUiStatus.CONTENT,
                    months = listOf(
                        ArchiveMonthUiModel(
                            id = 1L,
                            monthLabel =
                                "Июль 2026",
                            initialBudget =
                                BigDecimal(
                                    "40000",
                                ),
                            startedAtMillis =
                                1_751_324_400_000L,
                            closedAtMillis =
                                1_753_916_400_000L,
                        ),
                    ),
                ),
            onAction = {},
        )
    }
}