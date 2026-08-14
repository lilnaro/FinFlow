package ru.lilnaro.finflow.presentation.transactions

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.presentation.transactions.model.TransactionUiModel
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsAction
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsFilter
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsUiState
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsUiStatus
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowExpense
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowTertiary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowIncome
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowPrimaryLight
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurface
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurfaceElevated
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowSurfaceSoft
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextMuted
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTextSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTheme

@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    onAction: (TransactionsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FinFlowBackground),
    ) {
        TransactionsAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                )
            },
            floatingActionButton = {
                if (
                    uiState.status in listOf(
                        TransactionsUiStatus.EMPTY,
                        TransactionsUiStatus.CONTENT,
                    ) &&
                    !uiState.isSelectionMode
                ) {
                    AddTransactionButton(
                        onClick = {
                            onAction(
                                TransactionsAction
                                    .AddTransactionClicked,
                            )
                        },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 20.dp,
                    ),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 560.dp)
                        .fillMaxSize(),
                ) {
                    Spacer(
                        modifier = Modifier.height(12.dp),
                    )

                    TransactionsHeader(
                        uiState = uiState,
                        onAction = onAction,
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp),
                    )

                    when (uiState.status) {
                        TransactionsUiStatus.LOADING -> {
                            TransactionsLoadingState(
                                modifier = Modifier.weight(1f),
                            )
                        }

                        TransactionsUiStatus.NO_ACTIVE_MONTH -> {
                            NoActiveMonthState(
                                onBackClick = {
                                    onAction(
                                        TransactionsAction.BackClicked,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        TransactionsUiStatus.EMPTY,
                        TransactionsUiStatus.CONTENT,
                            -> {
                            TransactionsDataContent(
                                uiState = uiState,
                                onAction = onAction,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        TransactionsUiStatus.ERROR -> {
                            TransactionsErrorState(
                                message = uiState.errorMessage,
                                onRetry = {
                                    onAction(
                                        TransactionsAction.RetryClicked,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isDeleteConfirmationVisible) {
            DeleteConfirmationDialog(
                selectedCount = uiState.selectedCount,
                isDeleting = uiState.isDeleting,
                onConfirm = {
                    onAction(
                        TransactionsAction.DeleteConfirmed,
                    )
                },
                onDismiss = {
                    onAction(
                        TransactionsAction.DeleteCancelled,
                    )
                },
            )
        }
    }
}

@Composable
private fun TransactionsHeader(
    uiState: TransactionsUiState,
    onAction: (TransactionsAction) -> Unit,
) {
    if (uiState.isSelectionMode) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCircleButton(
                text = "×",
                onClick = {
                    onAction(
                        TransactionsAction
                            .ExitSelectionModeClicked,
                    )
                },
            )

            Text(
                text = "Выбрано: ${uiState.selectedCount}",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                color = FinFlowTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            TextButton(
                onClick = {
                    onAction(
                        TransactionsAction
                            .DeleteSelectedClicked,
                    )
                },
                enabled = !uiState.isDeleting,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = FinFlowExpense,
                    disabledContentColor =
                        FinFlowExpense.copy(
                            alpha = 0.45f,
                        ),
                ),
            ) {
                Text(
                    text = "Удалить",
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderCircleButton(
                text = "←",
                onClick = {
                    onAction(
                        TransactionsAction.BackClicked,
                    )
                },
            )

            Text(
                text = "Транзакции",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                color = FinFlowTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            if (uiState.monthLabel.isNotBlank()) {
                Surface(
                    color = FinFlowSurfaceElevated.copy(
                        alpha = 0.92f,
                    ),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        width = 1.dp,
                        color = FinFlowBorder,
                    ),
                ) {
                    Text(
                        text = uiState.monthLabel,
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 8.dp,
                        ),
                        color = FinFlowTextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    text: String,
    onClick: () -> Unit,
) {
    val textModifier = if (text == "←") {
        Modifier.offset(x = (-1).dp, y = (-1).dp)
    } else {
        Modifier
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        color = FinFlowSurfaceElevated.copy(
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                modifier = textModifier,
                color = FinFlowTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun TransactionsDataContent(
    uiState: TransactionsUiState,
    onAction: (TransactionsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleTransactions =
        uiState.visibleTransactions

    val groupedTransactions = remember(
        visibleTransactions,
    ) {
        visibleTransactions.groupBy { transaction ->
            transaction.createdAtMillis.toDayKey()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = 110.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(
            12.dp,
        ),
    ) {
        item(
            key = "summary",
        ) {
            TransactionsSummary(
                currentBalance = uiState.currentBalance,
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
            )
        }

        item(
            key = "filters",
        ) {
            TransactionFilters(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { filter ->
                    onAction(
                        TransactionsAction.FilterChanged(
                            filter = filter,
                        ),
                    )
                },
            )
        }

        if (visibleTransactions.isEmpty()) {
            item(
                key = "empty",
            ) {
                EmptyTransactionsState(
                    isEntireMonthEmpty =
                        uiState.transactions.isEmpty(),
                    selectedFilter =
                        uiState.selectedFilter,
                )
            }
        } else {
            groupedTransactions.forEach {
                    (_, transactions) ->

                val firstTransaction =
                    transactions.first()

                item(
                    key =
                        "date-${firstTransaction.createdAtMillis.toDayKey()}",
                ) {
                    Text(
                        text =
                            firstTransaction.createdAtMillis
                                .toDateGroupLabel(),
                        modifier = Modifier.padding(
                            top = 8.dp,
                            bottom = 2.dp,
                        ),
                        color = FinFlowTextSecondary,
                        style =
                            MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                items(
                    items = transactions,
                    key = { transaction ->
                        transaction.id
                    },
                ) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        isSelected =
                            transaction.id in
                                    uiState.selectedTransactionIds,
                        onClick = {
                            onAction(
                                TransactionsAction.TransactionClicked(
                                    transactionId =
                                        transaction.id,
                                ),
                            )
                        },
                        onLongClick = {
                            onAction(
                                TransactionsAction
                                    .TransactionLongClicked(
                                        transactionId =
                                            transaction.id,
                                    ),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionsSummary(
    currentBalance: BigDecimal,
    totalIncome: BigDecimal,
    totalExpense: BigDecimal,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            12.dp,
        ),
    ) {
        val heroShape =
            MaterialTheme.shapes.extraLarge

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            FinFlowSurfaceElevated,
                            FinFlowSurface,
                        ),
                    ),
                    shape = heroShape,
                )
                .padding(22.dp),
        ) {
            Column {
                Text(
                    text = "БАЛАНС",
                    color = FinFlowTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(8.dp),
                )

                AutoSizingMoneyText(
                    text = currentBalance.toRubleText(),
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography.displayMedium,
                    maxFontSize = 36.sp,
                    minFontSize = 18.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                12.dp,
            ),
        ) {
            SummaryMetricCard(
                title = "Доходы",
                value = totalIncome.toIncomeText(),
                accent = FinFlowIncome,
                modifier = Modifier.weight(1f),
            )

            SummaryMetricCard(
                title = "Расходы",
                value = totalExpense.toExpenseText(),
                accent = FinFlowExpense,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = FinFlowSurface.copy(
            alpha = 0.92f,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = FinFlowBorder.copy(
                alpha = 0.75f,
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
                modifier = Modifier.height(10.dp),
            )

            AutoSizingMoneyText(
                text = value,
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                maxFontSize = 18.sp,
                minFontSize = 11.sp,
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            Text(
                text = title,
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TransactionFilters(
    selectedFilter: TransactionsFilter,
    onFilterSelected: (TransactionsFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState(),
            ),
        horizontalArrangement = Arrangement.spacedBy(
            8.dp,
        ),
    ) {
        TransactionsFilter.entries.forEach { filter ->
            FilterPill(
                text = filter.toDisplayName(),
                selected = filter == selectedFilter,
                onClick = {
                    onFilterSelected(filter)
                },
            )
        }
    }
}

@Composable
private fun FilterPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) {
            FinFlowPrimary.copy(
                alpha = 0.18f,
            )
        } else {
            FinFlowSurface.copy(
                alpha = 0.90f,
            )
        },
        contentColor = if (selected) {
            FinFlowPrimaryLight
        } else {
            FinFlowTextSecondary
        },
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                FinFlowPrimary.copy(
                    alpha = 0.55f,
                )
            } else {
                FinFlowBorder.copy(
                    alpha = 0.75f,
                )
            },
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 9.dp,
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Medium
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionCard(
    transaction: TransactionUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val accent = when (transaction.type) {
        TransactionType.INCOME -> FinFlowIncome
        TransactionType.EXPENSE -> FinFlowExpense
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        color = if (isSelected) {
            FinFlowPrimary.copy(
                alpha = 0.15f,
            )
        } else {
            FinFlowSurface.copy(
                alpha = 0.92f,
            )
        },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                FinFlowPrimary.copy(
                    alpha = 0.65f,
                )
            } else {
                FinFlowBorder.copy(
                    alpha = 0.65f,
                )
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 13.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = accent.copy(
                    alpha = 0.13f,
                ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (isSelected) {
                            "✓"
                        } else {
                            transaction.categoryName
                                .take(1)
                                .uppercase()
                        },
                        color = accent,
                        style =
                            MaterialTheme.typography.titleMedium,
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
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(
                    modifier = Modifier.height(3.dp),
                )

                Text(
                    text = transaction
                        .createTransactionSubtitle(),
                    color = FinFlowTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(
                modifier = Modifier.size(10.dp),
            )

            Box(
                modifier = Modifier.widthIn(
                    min = 78.dp,
                    max = 142.dp,
                ),
            ) {
                AutoSizingMoneyText(
                    text = transaction
                        .toSignedMoneyText(),
                    color = accent,
                    style =
                        MaterialTheme.typography.titleMedium,
                    maxFontSize = 16.sp,
                    minFontSize = 10.sp,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun EmptyTransactionsState(
    isEntireMonthEmpty: Boolean,
    selectedFilter: TransactionsFilter,
) {
    StateCard {
        Surface(
            color = FinFlowPrimary.copy(
                alpha = 0.13f,
            ),
            shape = CircleShape,
        ) {
            Box(
                modifier = Modifier.size(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "↗",
                    color = FinFlowPrimaryLight,
                    style =
                        MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp),
        )

        Text(
            text = if (isEntireMonthEmpty) {
                "Пока нет транзакций"
            } else {
                "По этому фильтру пусто"
            },
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Text(
            text = if (isEntireMonthEmpty) {
                "Добавьте первую операцию, чтобы начать собирать финансовую историю месяца."
            } else {
                "В категории «${selectedFilter.toDisplayName()}» операций пока нет."
            },
            color = FinFlowTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TransactionsLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        StateCard {
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
                text = "Загружаем операции",
                color = FinFlowTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text =
                    "Собираем доходы, расходы и категории текущего месяца.",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoActiveMonthState(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        StateCard {
            Surface(
                color = FinFlowPrimary.copy(
                    alpha = 0.13f,
                ),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = FinFlowPrimaryLight,
                        style =
                            MaterialTheme.typography.headlineLarge,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp),
            )

            Text(
                text = "Нет активного месяца",
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text =
                    "Создайте финансовый месяц на главном экране, прежде чем добавлять операции.",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(22.dp),
            )

            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinFlowPrimary,
                    contentColor = FinFlowTextPrimary,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "Вернуться",
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 3.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun TransactionsErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        StateCard {
            Surface(
                color = FinFlowExpense.copy(
                    alpha = 0.13f,
                ),
                shape = CircleShape,
            ) {
                Box(
                    modifier = Modifier.size(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "!",
                        color = FinFlowExpense,
                        style =
                            MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp),
            )

            Text(
                text = "Не удалось загрузить операции",
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text = message
                    ?: "Попробуйте загрузить данные ещё раз.",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Spacer(
                modifier = Modifier.height(22.dp),
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinFlowPrimary,
                    contentColor = FinFlowTextPrimary,
                ),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "Повторить",
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 3.dp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StateCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FinFlowSurface.copy(
            alpha = 0.92f,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = FinFlowBorder.copy(
                alpha = 0.75f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 26.dp,
                    vertical = 32.dp,
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun AddTransactionButton(
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = FinFlowPrimary,
        contentColor = FinFlowTextPrimary,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )

            Spacer(
                modifier = Modifier.size(7.dp),
            )

            Text(
                text = "Добавить",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    selectedCount: Int,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        containerColor = FinFlowSurfaceElevated,
        titleContentColor = FinFlowTextPrimary,
        textContentColor = FinFlowTextSecondary,
        title = {
            Text(
                text =
                    "Удалить ${selectedCount.toTransactionCountText()}?",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text =
                    "После удаления баланс и статистика месяца будут пересчитаны. Это действие нельзя отменить.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinFlowExpense,
                    contentColor = FinFlowTextPrimary,
                ),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = FinFlowTextPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Удалить",
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text(
                    text = "Отмена",
                    color = FinFlowTextSecondary,
                )
            }
        },
    )
}

@Composable
private fun TransactionsAuroraBackground() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "transactionsAurora",
        )

    val movement by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "transactionsAuroraMovement",
    )

    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val radius =
            size.minDimension * 0.78f

        val firstCenter = Offset(
            x = size.width *
                    (0.85f - 0.12f * movement),
            y = size.height *
                    (0.12f + 0.08f * movement),
        )

        val secondCenter = Offset(
            x = size.width *
                    (0.10f + 0.12f * movement),
            y = size.height *
                    (0.58f - 0.05f * movement),
        )

        val thirdCenter = Offset(
            x = size.width *
                    (0.70f - 0.10f * movement),
            y = size.height *
                    (0.94f - 0.06f * movement),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    FinFlowGlowPrimary,
                    Color.Transparent,
                ),
                center = firstCenter,
                radius = radius,
            ),
            center = firstCenter,
            radius = radius,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    FinFlowGlowSecondary,
                    Color.Transparent,
                ),
                center = secondCenter,
                radius = radius,
            ),
            center = secondCenter,
            radius = radius,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    FinFlowGlowTertiary,
                    Color.Transparent,
                ),
                center = thirdCenter,
                radius = radius,
            ),
            center = thirdCenter,
            radius = radius,
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
    textAlign: TextAlign = TextAlign.Start,
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
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
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
        return "0\u00A0₽"
    }

    return "+${toRubleText()}"
}

private fun BigDecimal.toExpenseText(): String {
    if (compareTo(BigDecimal.ZERO) == 0) {
        return "0\u00A0₽"
    }

    return "−${toRubleText()}"
}

private fun TransactionUiModel.toSignedMoneyText(): String {
    return when (type) {
        TransactionType.INCOME -> {
            "+${amount.toRubleText()}"
        }

        TransactionType.EXPENSE -> {
            "−${amount.toRubleText()}"
        }
    }
}

private fun TransactionUiModel.createTransactionSubtitle(): String {
    val time = createdAtMillis.toTimeText()

    return if (note.isBlank()) {
        time
    } else {
        "$time • $note"
    }
}

private fun Long.toTimeText(): String {
    return SimpleDateFormat(
        "HH:mm",
        Locale.forLanguageTag("ru-RU"),
    ).format(
        Date(this),
    )
}

private fun Long.toDayKey(): Int {
    val calendar =
        Calendar.getInstance().apply {
            timeInMillis = this@toDayKey
        }

    return calendar.get(Calendar.YEAR) * 1_000 +
            calendar.get(Calendar.DAY_OF_YEAR)
}

private fun Long.toDateGroupLabel(): String {
    val transactionCalendar =
        Calendar.getInstance().apply {
            timeInMillis =
                this@toDateGroupLabel
        }

    val today =
        Calendar.getInstance()

    if (
        transactionCalendar.isSameDay(
            other = today,
        )
    ) {
        return "Сегодня"
    }

    val yesterday =
        (today.clone() as Calendar).apply {
            add(
                Calendar.DAY_OF_YEAR,
                -1,
            )
        }

    if (
        transactionCalendar.isSameDay(
            other = yesterday,
        )
    ) {
        return "Вчера"
    }

    return SimpleDateFormat(
        "d MMMM",
        Locale.forLanguageTag("ru-RU"),
    ).format(
        Date(this),
    )
}

private fun Calendar.isSameDay(
    other: Calendar,
): Boolean {
    return get(Calendar.YEAR) ==
            other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) ==
            other.get(Calendar.DAY_OF_YEAR)
}

private fun TransactionsFilter.toDisplayName(): String {
    return when (this) {
        TransactionsFilter.ALL -> "Все"
        TransactionsFilter.EXPENSE -> "Расходы"
        TransactionsFilter.INCOME -> "Доходы"
    }
}

private fun Int.toTransactionCountText(): String {
    val lastDigit = this % 10
    val lastTwoDigits = this % 100

    return when {
        lastDigit == 1 &&
                lastTwoDigits != 11 -> {
            "$this транзакцию"
        }

        lastDigit in 2..4 &&
                lastTwoDigits !in 12..14 -> {
            "$this транзакции"
        }

        else -> {
            "$this транзакций"
        }
    }
}

@Preview(
    name = "Transactions — Content",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun TransactionsScreenPreview() {
    FinFlowTheme {
        TransactionsScreen(
            uiState = TransactionsUiState(
                status = TransactionsUiStatus.CONTENT,
                monthLabel = "Август",
                currentBalance =
                    BigDecimal("27400.00"),
                totalIncome =
                    BigDecimal("12300.00"),
                totalExpense =
                    BigDecimal("24900.00"),
                transactions = listOf(
                    TransactionUiModel(
                        id = 1L,
                        amount =
                            BigDecimal("850.00"),
                        type =
                            TransactionType.EXPENSE,
                        categoryId = 1L,
                        categoryName = "Продукты",
                        note = "Супермаркет",
                        createdAtMillis =
                            System.currentTimeMillis(),
                    ),
                    TransactionUiModel(
                        id = 2L,
                        amount =
                            BigDecimal("420.00"),
                        type =
                            TransactionType.EXPENSE,
                        categoryId = 2L,
                        categoryName = "Такси",
                        note = "",
                        createdAtMillis =
                            System.currentTimeMillis() -
                                    3_600_000L,
                    ),
                    TransactionUiModel(
                        id = 3L,
                        amount =
                            BigDecimal("12000.00"),
                        type =
                            TransactionType.INCOME,
                        categoryId = 3L,
                        categoryName = "Зарплата",
                        note = "",
                        createdAtMillis =
                            System.currentTimeMillis() -
                                    7_200_000L,
                    ),
                ),
            ),
            onAction = {},
            snackbarHostState = remember {
                SnackbarHostState()
            },
        )
    }
}