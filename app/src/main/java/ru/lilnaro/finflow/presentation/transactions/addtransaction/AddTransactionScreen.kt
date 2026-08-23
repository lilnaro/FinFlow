package ru.lilnaro.finflow.presentation.transactions.addtransaction

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionAction
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionCategoryUiModel
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionUiState
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionUiStatus
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowExpense
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowTertiary
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
fun AddTransactionScreen(
    uiState: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FinFlowBackground),
    ) {
        AddTransactionAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                )
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

                    AddTransactionHeader(
                        monthLabel = uiState.monthLabel,
                        onBackClick = {
                            onAction(
                                AddTransactionAction.BackClicked,
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(20.dp),
                    )

                    when (uiState.status) {
                        AddTransactionUiStatus.LOADING -> {
                            LoadingState(
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AddTransactionUiStatus.CONTENT -> {
                            AddTransactionForm(
                                uiState = uiState,
                                onAction = onAction,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AddTransactionUiStatus.NO_ACTIVE_MONTH -> {
                            NoActiveMonthState(
                                onBackClick = {
                                    onAction(
                                        AddTransactionAction.BackClicked,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        AddTransactionUiStatus.ERROR -> {
                            ErrorState(
                                message = uiState.errorMessage,
                                onRetry = {
                                    onAction(
                                        AddTransactionAction.RetryClicked,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isCustomCategoryDialogVisible) {
            CustomCategoryDialog(
                uiState = uiState,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun AddTransactionHeader(
    monthLabel: String,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBackClick,
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
                    text = "←",
                    modifier = Modifier.offset(
                        x = (-1).dp,
                        y = (-1).dp,
                    ),
                    color = FinFlowTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "Новая транзакция",
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 14.dp,
                ),
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        if (monthLabel.isNotBlank()) {
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
                    text = monthLabel,
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

@Composable
private fun AddTransactionForm(
    uiState: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = when (uiState.type) {
        TransactionType.EXPENSE -> FinFlowExpense
        TransactionType.INCOME -> FinFlowIncome
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(
            18.dp,
        ),
    ) {
        item(
            key = "amount",
        ) {
            AmountCard(
                amountInput = uiState.amountInput,
                amountError = uiState.amountError,
                accent = accent,
                isEnabled = !uiState.isSaving,
                onAmountChanged = { value ->
                    onAction(
                        AddTransactionAction.AmountChanged(
                            value = value,
                        ),
                    )
                },
            )
        }

        item(
            key = "type",
        ) {
            TransactionTypeSelector(
                selectedType = uiState.type,
                enabled = !uiState.isSaving,
                onTypeSelected = { type ->
                    onAction(
                        AddTransactionAction.TypeChanged(
                            type = type,
                        ),
                    )
                },
            )
        }

        item(
            key = "category",
        ) {
            CategorySection(
                categories = uiState.categories,
                selectedCategoryId =
                    uiState.selectedCategoryId,
                categoryError =
                    uiState.categoryError,
                accent = accent,
                enabled = !uiState.isSaving,
                onCategorySelected = { categoryId ->
                    onAction(
                        AddTransactionAction.CategorySelected(
                            categoryId = categoryId,
                        ),
                    )
                },
                onAddCustomCategory = {
                    onAction(
                        AddTransactionAction
                            .AddCustomCategoryClicked,
                    )
                },
            )
        }

        item(
            key = "note",
        ) {
            NoteField(
                value = uiState.noteInput,
                enabled = !uiState.isSaving,
                onValueChange = { value ->
                    onAction(
                        AddTransactionAction.NoteChanged(
                            value = value,
                        ),
                    )
                },
            )
        }

        item(
            key = "save",
        ) {
            SaveTransactionButton(
                type = uiState.type,
                isSaving = uiState.isSaving,
                enabled = uiState.isSaveEnabled,
                onClick = {
                    onAction(
                        AddTransactionAction.SaveClicked,
                    )
                },
            )
        }
    }
}

@Composable
private fun AmountCard(
    amountInput: String,
    amountError: String?,
    accent: Color,
    isEnabled: Boolean,
    onAmountChanged: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FinFlowSurface.copy(
            alpha = 0.94f,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = if (amountError == null) {
                accent.copy(
                    alpha = 0.30f,
                )
            } else {
                FinFlowExpense.copy(
                    alpha = 0.65f,
                )
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 20.dp,
                vertical = 22.dp,
            ),
        ) {
            Text(
                text = "СУММА",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            OutlinedTextField(
                value = amountInput,
                onValueChange = onAmountChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "0,00",
                    )
                },
                suffix = {
                    Text(
                        text = "₽",
                        fontWeight = FontWeight.Bold,
                    )
                },
                isError = amountError != null,
                supportingText =
                    if (amountError != null) {
                        {
                            Text(
                                text = amountError,
                            )
                        }
                    } else {
                        null
                    },
                textStyle =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor =
                        FinFlowTextPrimary,
                    unfocusedTextColor =
                        FinFlowTextPrimary,
                    focusedBorderColor =
                        accent,
                    unfocusedBorderColor =
                        FinFlowBorder,
                    errorBorderColor =
                        FinFlowExpense,
                    focusedLabelColor =
                        accent,
                    cursorColor =
                        accent,
                    focusedContainerColor =
                        Color.Transparent,
                    unfocusedContainerColor =
                        Color.Transparent,
                ),
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    enabled: Boolean,
    onTypeSelected: (TransactionType) -> Unit,
) {
    Column {
        SectionTitle(
            title = "Тип операции",
            subtitle =
                "Выберите, как операция повлияет на баланс.",
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp),
        ) {
            TypeButton(
                text = "Расход",
                symbol = "−",
                selected =
                    selectedType ==
                            TransactionType.EXPENSE,
                accent = FinFlowExpense,
                enabled = enabled,
                onClick = {
                    onTypeSelected(
                        TransactionType.EXPENSE,
                    )
                },
                modifier = Modifier.weight(1f),
            )

            TypeButton(
                text = "Доход",
                symbol = "+",
                selected =
                    selectedType ==
                            TransactionType.INCOME,
                accent = FinFlowIncome,
                enabled = enabled,
                onClick = {
                    onTypeSelected(
                        TransactionType.INCOME,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TypeButton(
    text: String,
    symbol: String,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        color = if (selected) {
            accent.copy(
                alpha = 0.16f,
            )
        } else {
            FinFlowSurface.copy(
                alpha = 0.92f,
            )
        },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                accent.copy(
                    alpha = 0.60f,
                )
            } else {
                FinFlowBorder.copy(
                    alpha = 0.75f,
                )
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 14.dp,
            ),
            horizontalArrangement =
                Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol,
                color = if (selected) {
                    accent
                } else {
                    FinFlowTextSecondary
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.size(7.dp),
            )

            Text(
                text = text,
                color = if (selected) {
                    FinFlowTextPrimary
                } else {
                    FinFlowTextSecondary
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CategorySection(
    categories: List<AddTransactionCategoryUiModel>,
    selectedCategoryId: Long?,
    categoryError: String?,
    accent: Color,
    enabled: Boolean,
    onCategorySelected: (Long) -> Unit,
    onAddCustomCategory: () -> Unit,
) {
    Column {
        SectionTitle(
            title = "Категория",
            subtitle =
                "Категории зависят от выбранного типа операции.",
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        if (categories.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FinFlowSurface.copy(
                    alpha = 0.92f,
                ),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    width = 1.dp,
                    color = FinFlowBorder,
                ),
            ) {
                Text(
                    text =
                        "Для этого типа операций пока нет доступных категорий.",
                    modifier = Modifier.padding(16.dp),
                    color = FinFlowTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState(),
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected =
                            category.id ==
                                    selectedCategoryId,
                        accent = accent,
                        enabled = enabled,
                        onClick = {
                            onCategorySelected(
                                category.id,
                            )
                        },
                    )
                }
            }
        }

        val hasBuiltInCategories =
            categories.any { category ->
                !category.isCustom
            }

        if (hasBuiltInCategories) {
            Spacer(
                modifier = Modifier.height(10.dp),
            )

            AddCustomCategoryButton(
                accent = accent,
                enabled = enabled,
                onClick = onAddCustomCategory,
            )
        }

        if (categoryError != null) {
            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text = categoryError,
                color = FinFlowExpense,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AddCustomCategoryButton(
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = accent.copy(
            alpha = 0.08f,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(
                alpha = 0.38f,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 11.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "+",
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.size(7.dp),
            )

            Text(
                text = "Своя категория",
                color = FinFlowTextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CustomCategoryDialog(
    uiState: AddTransactionUiState,
    onAction: (AddTransactionAction) -> Unit,
) {
    val accent = when (uiState.type) {
        TransactionType.EXPENSE -> FinFlowExpense
        TransactionType.INCOME -> FinFlowIncome
    }

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isCreatingCategory) {
                onAction(
                    AddTransactionAction
                        .CustomCategoryDialogDismissed,
                )
            }
        },
        containerColor = FinFlowSurfaceElevated,
        titleContentColor = FinFlowTextPrimary,
        textContentColor = FinFlowTextSecondary,
        title = {
            Text(
                text = "Своя категория",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text =
                        "Введите название и выберите базовую категорию. Новая категория будет доступна для текущего типа операции.",
                    color = FinFlowTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(
                    modifier = Modifier.height(16.dp),
                )

                OutlinedTextField(
                    value =
                        uiState.customCategoryNameInput,
                    onValueChange = { value ->
                        onAction(
                            AddTransactionAction
                                .CustomCategoryNameChanged(
                                    value = value,
                                ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled =
                        !uiState.isCreatingCategory,
                    singleLine = true,
                    label = {
                        Text(
                            text = "Название",
                        )
                    },
                    isError =
                        uiState.customCategoryNameError !=
                                null,
                    supportingText =
                        if (
                            uiState.customCategoryNameError !=
                            null
                        ) {
                            {
                                Text(
                                    text =
                                        uiState
                                            .customCategoryNameError,
                                )
                            }
                        } else {
                            {
                                Text(
                                    text =
                                        "${uiState.customCategoryNameInput.length}/40",
                                )
                            }
                        },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor =
                                FinFlowTextPrimary,
                            unfocusedTextColor =
                                FinFlowTextPrimary,
                            focusedBorderColor =
                                accent,
                            unfocusedBorderColor =
                                FinFlowBorder,
                            errorBorderColor =
                                FinFlowExpense,
                            cursorColor = accent,
                            focusedContainerColor =
                                Color.Transparent,
                            unfocusedContainerColor =
                                Color.Transparent,
                        ),
                    shape = MaterialTheme.shapes.large,
                )

                Spacer(
                    modifier = Modifier.height(16.dp),
                )

                Text(
                    text = "Базовая категория",
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(4.dp),
                )

                Text(
                    text =
                        "Выберите на что примерно похожа ваша новая категория из заданного списка",
                    color = FinFlowTextMuted,
                    style =
                        MaterialTheme.typography.bodySmall,
                )

                Spacer(
                    modifier = Modifier.height(10.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState(),
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    uiState.builtInCategories
                        .forEach { category ->
                            CategoryChip(
                                category = category,
                                selected =
                                    category.id ==
                                            uiState
                                                .selectedParentCategoryId,
                                accent = accent,
                                enabled =
                                    !uiState
                                        .isCreatingCategory,
                                onClick = {
                                    onAction(
                                        AddTransactionAction
                                            .CustomCategoryParentSelected(
                                                categoryId =
                                                    category.id,
                                            ),
                                    )
                                },
                            )
                        }
                }

                if (
                    uiState.customCategoryParentError !=
                    null
                ) {
                    Spacer(
                        modifier = Modifier.height(8.dp),
                    )

                    Text(
                        text =
                            uiState
                                .customCategoryParentError,
                        color = FinFlowExpense,
                        style =
                            MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(
                        AddTransactionAction
                            .CustomCategoryCreateClicked,
                    )
                },
                enabled =
                    uiState
                        .isCustomCategoryCreateEnabled,
            ) {
                if (uiState.isCreatingCategory) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = accent,
                        strokeWidth = 2.dp,
                    )

                    Spacer(
                        modifier = Modifier.size(8.dp),
                    )

                    Text(
                        text = "Создаём...",
                    )
                } else {
                    Text(
                        text = "Создать",
                        color = if (
                            uiState
                                .isCustomCategoryCreateEnabled
                        ) {
                            accent
                        } else {
                            FinFlowTextMuted
                        },
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onAction(
                        AddTransactionAction
                            .CustomCategoryDialogDismissed,
                    )
                },
                enabled =
                    !uiState.isCreatingCategory,
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
private fun CategoryChip(
    category: AddTransactionCategoryUiModel,
    selected: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = if (selected) {
            accent.copy(
                alpha = 0.16f,
            )
        } else {
            FinFlowSurface.copy(
                alpha = 0.92f,
            )
        },
        contentColor = if (selected) {
            accent
        } else {
            FinFlowTextSecondary
        },
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                accent.copy(
                    alpha = 0.60f,
                )
            } else {
                FinFlowBorder.copy(
                    alpha = 0.75f,
                )
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 10.dp,
            ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                color = if (selected) {
                    accent.copy(
                        alpha = 0.18f,
                    )
                } else {
                    FinFlowSurfaceSoft
                },
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Text(
                        text = category.name
                            .take(1)
                            .uppercase(),
                        color = if (selected) {
                            accent
                        } else {
                            FinFlowTextMuted
                        },
                        style =
                            MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(8.dp),
            )

            Text(
                text = category.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Medium
                },
            )

            if (category.isCustom) {
                Spacer(
                    modifier = Modifier.size(6.dp),
                )

                Text(
                    text = "•",
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun NoteField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column {
        SectionTitle(
            title = "Комментарий",
            subtitle =
                "Необязательно · до 120 символов",
        )

        Spacer(
            modifier = Modifier.height(10.dp),
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            minLines = 3,
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor =
                    FinFlowTextPrimary,
                unfocusedTextColor =
                    FinFlowTextPrimary,
                focusedBorderColor =
                    FinFlowPrimary,
                unfocusedBorderColor =
                    FinFlowBorder,
                cursorColor =
                    FinFlowPrimary,
                focusedContainerColor =
                    FinFlowSurface.copy(
                        alpha = 0.55f,
                    ),
                unfocusedContainerColor =
                    FinFlowSurface.copy(
                        alpha = 0.55f,
                    ),
            ),
            shape = MaterialTheme.shapes.large,
        )

        Spacer(
            modifier = Modifier.height(5.dp),
        )

        Text(
            text = "${value.length}/120",
            modifier = Modifier.fillMaxWidth(),
            color = FinFlowTextMuted,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SaveTransactionButton(
    type: TransactionType,
    isSaving: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = when (type) {
        TransactionType.EXPENSE -> FinFlowExpense
        TransactionType.INCOME -> FinFlowIncome
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = FinFlowTextPrimary,
            disabledContainerColor =
                FinFlowSurfaceSoft,
            disabledContentColor =
                FinFlowTextMuted,
        ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = FinFlowTextPrimary,
                strokeWidth = 2.dp,
            )

            Spacer(
                modifier = Modifier.size(10.dp),
            )

            Text(
                text = "Сохраняем...",
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                text = if (
                    type == TransactionType.EXPENSE
                ) {
                    "Добавить расход"
                } else {
                    "Добавить доход"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
) {
    Column {
        Text(
            text = title,
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(
            modifier = Modifier.height(3.dp),
        )

        Text(
            text = subtitle,
            color = FinFlowTextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    CenteredState(
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
            text = "Готовим форму",
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Text(
            text =
                "Загружаем активный месяц и доступные категории.",
            color = FinFlowTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NoActiveMonthState(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        modifier = modifier,
    ) {
        StateSymbol(
            text = "+",
            color = FinFlowPrimaryLight,
        )

        Spacer(
            modifier = Modifier.height(18.dp),
        )

        Text(
            text = "Нет активного месяца",
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        Text(
            text =
                "Создайте финансовый месяц, прежде чем добавлять операции.",
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
        ) {
            Text(
                text = "Вернуться",
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        modifier = modifier,
    ) {
        StateSymbol(
            text = "!",
            color = FinFlowExpense,
        )

        Spacer(
            modifier = Modifier.height(18.dp),
        )

        Text(
            text = "Не удалось открыть форму",
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
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
        ) {
            Text(
                text = "Повторить",
            )
        }
    }
}

@Composable
private fun CenteredState(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FinFlowSurface.copy(
                alpha = 0.92f,
            ),
            shape = MaterialTheme.shapes.extraLarge,
            border = BorderStroke(
                width = 1.dp,
                color = FinFlowBorder,
            ),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 26.dp,
                    vertical = 32.dp,
                ),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun StateSymbol(
    text: String,
    color: Color,
) {
    Surface(
        color = color.copy(
            alpha = 0.13f,
        ),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.size(58.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = color,
                style =
                    MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AddTransactionAuroraBackground() {
    val transition =
        rememberInfiniteTransition(
            label = "addTransactionAurora",
        )

    val movement by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18_000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "addTransactionAuroraMovement",
    )

    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val radius =
            size.minDimension * 0.78f

        val firstCenter = Offset(
            x = size.width *
                    (0.88f - 0.12f * movement),
            y = size.height *
                    (0.12f + 0.06f * movement),
        )

        val secondCenter = Offset(
            x = size.width *
                    (0.08f + 0.10f * movement),
            y = size.height *
                    (0.55f - 0.06f * movement),
        )

        val thirdCenter = Offset(
            x = size.width *
                    (0.75f - 0.08f * movement),
            y = size.height *
                    (0.92f - 0.05f * movement),
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

@Preview(
    name = "Add transaction",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun AddTransactionScreenPreview() {
    FinFlowTheme {
        AddTransactionScreen(
            uiState = AddTransactionUiState(
                status =
                    AddTransactionUiStatus.CONTENT,
                monthLabel = "Август",
                type = TransactionType.EXPENSE,
                amountInput = "1250,50",
                categories = listOf(
                    AddTransactionCategoryUiModel(
                        id = 1L,
                        name = "Продукты",
                        isCustom = false,
                    ),
                    AddTransactionCategoryUiModel(
                        id = 2L,
                        name = "Такси",
                        isCustom = false,
                    ),
                    AddTransactionCategoryUiModel(
                        id = 3L,
                        name = "Досуг",
                        isCustom = true,
                    ),
                ),
                selectedCategoryId = 1L,
                noteInput = "Продукты на неделю",
            ),
            onAction = {},
            snackbarHostState =
                androidx.compose.runtime.remember {
                    SnackbarHostState()
                },
        )
    }
}