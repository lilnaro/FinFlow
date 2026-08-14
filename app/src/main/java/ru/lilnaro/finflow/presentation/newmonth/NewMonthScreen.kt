package ru.lilnaro.finflow.presentation.newmonth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthAction
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthUiState
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowPrimary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowTertiary
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
fun NewMonthScreen(
    uiState: NewMonthUiState,
    onAction: (NewMonthAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FinFlowBackground),
    ) {
        NewMonthAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                )
            },
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
                        .widthIn(max = 560.dp)
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState(),
                        ),
                ) {
                    NewMonthHeader(
                        onBackClick = {
                            onAction(
                                NewMonthAction.BackClicked,
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(24.dp),
                    )

                    MonthHeroCard(
                        monthLabel = uiState.monthLabel,
                    )

                    Spacer(
                        modifier = Modifier.height(18.dp),
                    )

                    BudgetCard(
                        value = uiState.initialBudgetInput,
                        error = uiState.budgetError,
                        enabled = !uiState.isSaving,
                        onValueChange = { value ->
                            onAction(
                                NewMonthAction
                                    .InitialBudgetChanged(
                                        value = value,
                                    ),
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp),
                    )

                    MonthInfoCard()

                    Spacer(
                        modifier = Modifier.height(24.dp),
                    )

                    CreateMonthButton(
                        enabled = uiState.isCreateEnabled,
                        isSaving = uiState.isSaving,
                        onClick = {
                            onAction(
                                NewMonthAction
                                    .CreateMonthClicked,
                            )
                        },
                    )

                    Spacer(
                        modifier = Modifier.height(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NewMonthHeader(
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
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Text(
            text = "Новый месяц",
            modifier = Modifier
                .weight(1f)
                .padding(
                    horizontal = 14.dp,
                ),
            color = FinFlowTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Surface(
            color = FinFlowPrimary.copy(
                alpha = 0.14f,
            ),
            shape = CircleShape,
        ) {
            Text(
                text = "Старт",
                modifier = Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 8.dp,
                ),
                color = FinFlowPrimaryLight,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MonthHeroCard(
    monthLabel: String,
) {
    val shape =
        MaterialTheme.shapes.extraLarge

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        FinFlowPrimary.copy(
                            alpha = 0.20f,
                        ),
                        FinFlowSurfaceElevated,
                        FinFlowSurface,
                    ),
                ),
                shape = shape,
            )
            .padding(
                horizontal = 22.dp,
                vertical = 26.dp,
            ),
    ) {
        Column {
            Surface(
                modifier = Modifier.size(48.dp),
                color = FinFlowPrimary.copy(
                    alpha = 0.17f,
                ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = FinFlowPrimaryLight,
                        style =
                            MaterialTheme.typography
                                .headlineMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp),
            )

            Text(
                text = "НАЧИНАЕМ",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(6.dp),
            )

            Text(
                text = monthLabel,
                color = FinFlowTextPrimary,
                style =
                    MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(
                modifier = Modifier.height(8.dp),
            )

            Text(
                text =
                    "Задайте стартовый остаток — от него FinFlow начнёт считать ваш баланс.",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BudgetCard(
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FinFlowSurface.copy(
            alpha = 0.94f,
        ),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = if (error == null) {
                FinFlowBorder
            } else {
                MaterialTheme.colorScheme.error
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = "СТАРТОВЫЙ БЮДЖЕТ",
                color = FinFlowTextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(
                modifier = Modifier.height(12.dp),
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "40 000",
                    )
                },
                suffix = {
                    Text(
                        text = "₽",
                        fontWeight = FontWeight.Bold,
                    )
                },
                isError = error != null,
                supportingText = {
                    Text(
                        text = error
                            ?: "Можно начать и с нулевого остатка.",
                    )
                },
                textStyle =
                    MaterialTheme.typography
                        .headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                keyboardOptions = KeyboardOptions(
                    keyboardType =
                        KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor =
                            FinFlowTextPrimary,
                        unfocusedTextColor =
                            FinFlowTextPrimary,
                        focusedBorderColor =
                            FinFlowPrimary,
                        unfocusedBorderColor =
                            FinFlowBorder,
                        focusedContainerColor =
                            Color.Transparent,
                        unfocusedContainerColor =
                            Color.Transparent,
                        cursorColor =
                            FinFlowPrimary,
                        errorBorderColor =
                            MaterialTheme.colorScheme.error,
                    ),
                shape = MaterialTheme.shapes.large,
            )
        }
    }
}

@Composable
private fun MonthInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FinFlowSecondary.copy(
            alpha = 0.08f,
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = FinFlowSecondary.copy(
                alpha = 0.22f,
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                color = FinFlowSecondary.copy(
                    alpha = 0.14f,
                ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "i",
                        color = FinFlowSecondary,
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
                    text = "Что произойдёт дальше?",
                    color = FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(
                    modifier = Modifier.height(5.dp),
                )

                Text(
                    text =
                        "Месяц станет активным. После этого можно добавлять доходы и расходы, а FinFlow будет автоматически пересчитывать баланс.",
                    color = FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CreateMonthButton(
    enabled: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = FinFlowPrimary,
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
                text = "Создаём месяц...",
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(
                text = "Начать финансовый месяц",
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NewMonthAuroraBackground() {
    val transition =
        rememberInfiniteTransition(
            label = "newMonthAurora",
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
        label = "newMonthAuroraMovement",
    )

    Canvas(
        modifier = Modifier.fillMaxSize(),
    ) {
        val radius =
            size.minDimension * 0.80f

        val firstCenter = Offset(
            x = size.width *
                    (0.85f - 0.10f * movement),
            y = size.height *
                    (0.12f + 0.08f * movement),
        )

        val secondCenter = Offset(
            x = size.width *
                    (0.10f + 0.12f * movement),
            y = size.height *
                    (0.58f - 0.06f * movement),
        )

        val thirdCenter = Offset(
            x = size.width *
                    (0.72f - 0.08f * movement),
            y = size.height *
                    (0.94f - 0.05f * movement),
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
    name = "New month",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun NewMonthScreenPreview() {
    FinFlowTheme {
        NewMonthScreen(
            uiState = NewMonthUiState(
                year = 2026,
                monthNumber = 8,
                monthLabel = "Август 2026",
                initialBudgetInput = "40000",
            ),
            onAction = {},
            snackbarHostState =
                androidx.compose.runtime.remember {
                    SnackbarHostState()
                },
        )
    }
}