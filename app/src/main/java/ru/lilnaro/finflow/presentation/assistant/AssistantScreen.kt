package ru.lilnaro.finflow.presentation.assistant

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lilnaro.finflow.presentation.assistant.model.AssistantAction
import ru.lilnaro.finflow.presentation.assistant.model.AssistantMessageRole
import ru.lilnaro.finflow.presentation.assistant.model.AssistantModelDownloadStatus
import ru.lilnaro.finflow.presentation.assistant.model.AssistantMessageUiModel
import ru.lilnaro.finflow.presentation.assistant.model.AssistantModelStatus
import ru.lilnaro.finflow.presentation.assistant.model.AssistantUiState
import ru.lilnaro.finflow.presentation.assistant.model.AssistantUiStatus
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBorder
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowExpense
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowIncome
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowTertiary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowSecondary
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowGlowPrimary
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
fun AssistantScreen(
    uiState: AssistantUiState,
    onAction: (AssistantAction) -> Unit,
    showBackButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                FinFlowBackground,
            ),
    ) {
        AssistantAuroraBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { innerPadding ->
            when (uiState.status) {
                AssistantUiStatus.LOADING -> {
                    LoadingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                AssistantUiStatus.EMPTY -> {
                    EmptyState(
                        onBackClick = {
                            onAction(
                                AssistantAction.BackClicked,
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                AssistantUiStatus.ERROR -> {
                    ErrorState(
                        message =
                            uiState.errorMessage,
                        onRetry = {
                            onAction(
                                AssistantAction.RetryClicked,
                            )
                        },
                        onBackClick = {
                            onAction(
                                AssistantAction.BackClicked,
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                AssistantUiStatus.CONTENT -> {
                    AssistantContent(
                        uiState = uiState,
                        onAction = onAction,
                        showBackButton =
                            showBackButton,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantContent(
    uiState: AssistantUiState,
    onAction: (AssistantAction) -> Unit,
    showBackButton: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
            ),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
    ) {
        item {
            Spacer(
                modifier =
                    Modifier.height(8.dp),
            )
        }

        item {
            AssistantHeader(
                monthLabel =
                    uiState.monthLabel,
                showBackButton =
                    showBackButton,
                onBackClick = {
                    onAction(
                        AssistantAction.BackClicked,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = 560.dp,
                    ),
            )
        }

        item {
            ChatCard(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = 560.dp,
                    )
                    .height(
                        CHAT_PANEL_HEIGHT,
                    ),
            )
        }

        item {
            Spacer(
                modifier =
                    Modifier.height(24.dp),
            )
        }
    }
}

@Composable
private fun AssistantHeader(
    monthLabel: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                vertical = 8.dp,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        if (showBackButton) {
            Surface(
                onClick = onBackClick,
                modifier =
                    Modifier.size(42.dp),
                color = FinFlowSurface,
                shape = CircleShape,
                border = BorderStroke(
                    width = 1.dp,
                    color = FinFlowBorder,
                ),
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Text(
                        text = "←",
                        color =
                            FinFlowTextPrimary,
                        fontSize = 20.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start =
                        if (showBackButton) {
                            14.dp
                        } else {
                            0.dp
                        },
                    end = 14.dp,
                ),
        ) {
            Text(
                text = "FinFlow AI",
                color =
                    FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold,
            )

            Text(
                text = monthLabel,
                color =
                    FinFlowTextSecondary,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )
        }

        Surface(
            color =
                FinFlowPrimary.copy(
                    alpha = 0.14f,
                ),
            shape = CircleShape,
        ) {
            Text(
                text = "Ассистент",
                modifier =
                    Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 7.dp,
                    ),
                color =
                    FinFlowPrimaryLight,
                style =
                    MaterialTheme.typography
                        .labelMedium,
                fontWeight =
                    FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ChatCard(
    uiState: AssistantUiState,
    onAction: (AssistantAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color =
            FinFlowSurface,
        shape =
            MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowPrimary.copy(
                    alpha = 0.26f,
                ),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    17.dp,
                ),
        ) {
            Text(
                text =
                    "Спроси FinFlow",
                color =
                    FinFlowTextPrimary,
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
                    "Анализирует доходы, расходы, историю и личные категории, помогает замечать изменения и отвечает на вопросы по вашим финансам.",
                color =
                    FinFlowTextSecondary,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp),
            )

            if (
                uiState.modelStatus !=
                AssistantModelStatus.READY
            ) {
                AssistantSetupContent(
                    uiState = uiState,
                    onStartDownload = {
                        onAction(
                            AssistantAction
                                .StartModelDownloadClicked,
                        )
                    },
                    onCancelDownload = {
                        onAction(
                            AssistantAction
                                .CancelModelDownloadClicked,
                        )
                    },
                    onRetry = {
                        onAction(
                            AssistantAction
                                .CheckModelClicked,
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )

                return@Column
            }

            if (
                uiState.messages.isNotEmpty() ||
                uiState.isGenerating
            ) {
                ChatMessagesViewport(
                    messages =
                        uiState.messages,
                    isGenerating =
                        uiState.isGenerating,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp),
                )
            } else {
                if (
                    uiState.showQuickQuestions
                ) {
                    QuickQuestionButton(
                        text =
                            "Что сейчас сильнее всего влияет на мои финансы?",
                        onClick = {
                            onAction(
                                AssistantAction
                                    .QuickQuestionClicked(
                                        question =
                                            "Что сейчас сильнее всего влияет на мои финансы?",
                                    ),
                            )
                        },
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp),
                    )

                    QuickQuestionButton(
                        text =
                            "Как изменились мои доходы и расходы?",
                        onClick = {
                            onAction(
                                AssistantAction
                                    .QuickQuestionClicked(
                                        question =
                                            "Как изменились мои доходы и расходы?",
                                    ),
                            )
                        },
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp),
                    )

                    QuickQuestionButton(
                        text =
                            "Что можно улучшить без слишком жёсткой экономии?",
                        onClick = {
                            onAction(
                                AssistantAction
                                    .QuickQuestionClicked(
                                        question =
                                            "Что можно улучшить без слишком жёсткой экономии?",
                                    ),
                            )
                        },
                    )
                }

                Spacer(
                    modifier =
                        Modifier.weight(1f),
                )
            }

            uiState.chatErrorMessage
                ?.let { message ->
                    Surface(
                        modifier =
                            Modifier.fillMaxWidth(),
                        color =
                            FinFlowExpense.copy(
                                alpha = 0.09f,
                            ),
                        shape =
                            MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = message,
                            modifier =
                                Modifier.padding(
                                    12.dp,
                                ),
                            color =
                                FinFlowExpense,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(10.dp),
                    )
                }

            OutlinedTextField(
                value =
                    uiState.questionInput,
                onValueChange = { value ->
                    onAction(
                        AssistantAction
                            .QuestionChanged(
                                value = value,
                            ),
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text =
                            "Введите ваш вопрос здесь",
                        color =
                            FinFlowTextMuted,
                    )
                },
                minLines = 2,
                maxLines = 4,
                enabled =
                    !uiState.isGenerating,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization =
                            KeyboardCapitalization.Sentences,
                        imeAction =
                            ImeAction.Default,
                    ),
                colors =
                    OutlinedTextFieldDefaults
                        .colors(
                            focusedTextColor =
                                FinFlowTextPrimary,
                            unfocusedTextColor =
                                FinFlowTextPrimary,
                            focusedBorderColor =
                                FinFlowPrimary,
                            unfocusedBorderColor =
                                FinFlowBorder,
                            focusedContainerColor =
                                FinFlowSurfaceSoft,
                            unfocusedContainerColor =
                                FinFlowSurfaceSoft,
                            cursorColor =
                                FinFlowPrimary,
                        ),
                shape =
                    MaterialTheme.shapes.large,
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp),
            )

            Button(
                onClick = {
                    onAction(
                        AssistantAction
                            .SendQuestionClicked,
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    uiState.canSend,
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                FinFlowPrimary,
                            contentColor =
                                FinFlowTextPrimary,
                            disabledContainerColor =
                                FinFlowSurfaceSoft,
                            disabledContentColor =
                                FinFlowTextMuted,
                        ),
            ) {
                Text(
                    text =
                        if (
                            uiState.isGenerating
                        ) {
                            "Анализирую..."
                        } else {
                            "Спросить"
                        },
                    fontWeight =
                        FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AssistantSetupContent(
    uiState: AssistantUiState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                top = 2.dp,
            ),
        verticalArrangement =
            Arrangement.Top,
    ) {
        when {
            uiState.modelDownloadStatus ==
                    AssistantModelDownloadStatus.DOWNLOADING -> {
                DownloadingModelContent(
                    uiState = uiState,
                    onCancelDownload =
                        onCancelDownload,
                )
            }

            uiState.modelStatus ==
                    AssistantModelStatus.LOADING ||
                    uiState.modelStatus ==
                    AssistantModelStatus.CHECKING -> {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color =
                            FinFlowPrimaryLight,
                        trackColor =
                            FinFlowSurfaceSoft,
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp),
                    )

                    Column {
                        Text(
                            text =
                                "Подготавливаем FinFlow AI",
                            color =
                                FinFlowTextPrimary,
                            style =
                                MaterialTheme.typography
                                    .bodyMedium,
                            fontWeight =
                                FontWeight.SemiBold,
                        )

                        Text(
                            text =
                                "Первый запуск после установки может занять немного времени.",
                            color =
                                FinFlowTextSecondary,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                        )
                    }
                }
            }

            uiState.modelStatus ==
                    AssistantModelStatus.ERROR -> {
                SetupMessageCard(
                    title =
                        "Не удалось запустить FinFlow AI",
                    description =
                        uiState.modelErrorMessage
                            ?: "Попробуйте ещё раз. Если ошибка повторится, проверьте память устройства.",
                    accent =
                        FinFlowExpense,
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp),
                )

                Button(
                    onClick =
                        onRetry,
                    modifier =
                        Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text =
                            "Повторить",
                    )
                }
            }

            else -> {
                SetupMessageCard(
                    title =
                        "Подготовить FinFlow AI",
                    description =
                        "Один раз скачайте локальный AI-модуль. После установки ответы будут формироваться прямо на устройстве.",
                    accent =
                        FinFlowPrimaryLight,
                )

                Spacer(
                    modifier =
                        Modifier.height(10.dp),
                )

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        FinFlowSurfaceElevated,
                    shape =
                        MaterialTheme.shapes.large,
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color =
                                FinFlowBorder.copy(
                                    alpha = 0.68f,
                                ),
                        ),
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 11.dp,
                            ),
                    ) {
                        SetupInfoRow(
                            label =
                                "Размер загрузки",
                            value =
                                uiState.approximateModelDownloadBytes
                                    .toGigabyteText(),
                        )

                        Spacer(
                            modifier =
                                Modifier.height(7.dp),
                        )

                        SetupInfoRow(
                            label =
                                "Нужно свободного места",
                            value =
                                uiState.minimumRequiredFreeSpaceBytes
                                    .toGigabyteText(),
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp),
                        )

                        Text(
                            text =
                                "Для такого объёма рекомендуется Wi‑Fi.",
                            color =
                                FinFlowTextMuted,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                        )
                    }
                }

                uiState.modelDownloadErrorMessage
                    ?.let { error ->
                        Spacer(
                            modifier =
                                Modifier.height(10.dp),
                        )

                        Text(
                            text = error,
                            color =
                                FinFlowExpense,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                        )
                    }

                Spacer(
                    modifier =
                        Modifier.height(12.dp),
                )

                Button(
                    onClick =
                        onStartDownload,
                    modifier =
                        Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    FinFlowPrimary,
                                contentColor =
                                    FinFlowTextPrimary,
                            ),
                ) {
                    Text(
                        text =
                            "Скачать AI-модуль",
                        fontWeight =
                            FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color =
                FinFlowTextSecondary,
            style =
                MaterialTheme.typography
                    .bodySmall,
        )

        Text(
            text = value,
            color =
                FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
            fontWeight =
                FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DownloadingModelContent(
    uiState: AssistantUiState,
    onCancelDownload: () -> Unit,
) {
    val totalBytes =
        uiState.modelDownloadTotalBytes
            ?: uiState
                .approximateModelDownloadBytes

    val progress =
        if (totalBytes > 0L) {
            (
                    uiState.modelDownloadedBytes
                        .toDouble() /
                            totalBytes.toDouble()
                    )
                .coerceIn(
                    minimumValue = 0.0,
                    maximumValue = 1.0,
                )
                .toFloat()
        } else {
            0f
        }

    Text(
        text =
            "Скачиваем FinFlow AI",
        color =
            FinFlowTextPrimary,
        style =
            MaterialTheme.typography
                .titleMedium,
        fontWeight =
            FontWeight.SemiBold,
    )

    Spacer(
        modifier =
            Modifier.height(6.dp),
    )

    Text(
        text =
            "${uiState.modelDownloadedBytes.toGigabyteText()} / ${totalBytes.toGigabyteText()} · ${(progress * 100f).toInt()}%",
        color =
            FinFlowTextSecondary,
        style =
            MaterialTheme.typography
                .bodySmall,
    )

    Spacer(
        modifier =
            Modifier.height(12.dp),
    )

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(8.dp),
        color =
            FinFlowSurfaceSoft,
        shape =
            CircleShape,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction = progress,
                    )
                    .fillMaxSize()
                    .background(
                        FinFlowPrimary,
                        CircleShape,
                    ),
            )
        }
    }

    Spacer(
        modifier =
            Modifier.height(10.dp),
    )

    Text(
        text =
            "Загрузка продолжится при сворачивании FinFlow, пока приложение активно. При сетевом сбое FinFlow повторит запрос и продолжит с уже скачанного места.",
        color =
            FinFlowTextSecondary,
        style =
            MaterialTheme.typography
                .bodySmall,
    )

    Spacer(
        modifier =
            Modifier.height(14.dp),
    )

    Button(
        onClick =
            onCancelDownload,
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            ButtonDefaults
                .buttonColors(
                    containerColor =
                        FinFlowSurfaceElevated,
                    contentColor =
                        FinFlowTextPrimary,
                ),
    ) {
        Text(
            text =
                "Отменить загрузку",
        )
    }
}

@Composable
private fun SetupMessageCard(
    title: String,
    description: String,
    accent: Color,
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        color =
            accent.copy(
                alpha = 0.08f,
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
            modifier =
                Modifier.padding(
                    13.dp,
                ),
        ) {
            Text(
                text = title,
                color =
                    FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                fontWeight =
                    FontWeight.SemiBold,
            )

            Spacer(
                modifier =
                    Modifier.height(5.dp),
            )

            Text(
                text =
                    description,
                color =
                    FinFlowTextSecondary,
                style =
                    MaterialTheme.typography
                        .bodySmall,
            )
        }
    }
}

@Composable
private fun ChatMessagesViewport(
    messages: List<AssistantMessageUiModel>,
    isGenerating: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState =
        rememberLazyListState()

    val itemCount =
        messages.size +
                if (isGenerating) {
                    1
                } else {
                    0
                }

    LaunchedEffect(
        itemCount,
    ) {
        if (itemCount > 0) {
            listState.animateScrollToItem(
                index = itemCount - 1,
            )
        }
    }

    Surface(
        modifier = modifier,
        color =
            FinFlowBackground.copy(
                alpha = 0.34f,
            ),
        shape =
            RoundedCornerShape(
                24.dp,
            ),
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.42f,
                ),
        ),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp,
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp,
                ),
        ) {
            items(
                items = messages,
                key = { message ->
                    message.id
                },
            ) { message ->
                MessageBubble(
                    message = message,
                )
            }

            if (isGenerating) {
                item(
                    key = "generating",
                ) {
                    GeneratingBubble()
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message:
    AssistantMessageUiModel,
) {
    val isUser =
        message.role ==
                AssistantMessageRole.USER

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 2.dp,
                ),
        horizontalArrangement =
            if (isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
    ) {
        Surface(
            modifier =
                Modifier.widthIn(
                    max = 314.dp,
                ),
            color =
                if (isUser) {
                    FinFlowPrimary.copy(
                        alpha = 0.18f,
                    )
                } else {
                    FinFlowSurfaceElevated
                },
            shape =
                RoundedCornerShape(
                    20.dp,
                ),
            border =
                if (isUser) {
                    BorderStroke(
                        width = 1.dp,
                        color =
                            FinFlowPrimary.copy(
                                alpha = 0.24f,
                            ),
                    )
                } else {
                    BorderStroke(
                        width = 1.dp,
                        color =
                            FinFlowBorder.copy(
                                alpha = 0.38f,
                            ),
                    )
                },
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 14.dp,
                    ),
            ) {
                Text(
                    text =
                        if (isUser) {
                            "Вы"
                        } else {
                            "FinFlow AI"
                        },
                    color =
                        if (isUser) {
                            FinFlowPrimaryLight
                        } else {
                            FinFlowIncome
                        },
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    fontWeight =
                        FontWeight.SemiBold,
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp),
                )

                Text(
                    text =
                        message.text
                            .toAssistantAnnotatedText(),
                    color =
                        FinFlowTextPrimary,
                    style =
                        MaterialTheme.typography
                            .bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun GeneratingBubble() {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.Start,
    ) {
        Surface(
            color =
                FinFlowSurfaceElevated,
            shape =
                RoundedCornerShape(
                    20.dp,
                ),
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        FinFlowBorder.copy(
                            alpha = 0.38f,
                        ),
                ),
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 13.dp,
                    ),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color =
                        FinFlowPrimaryLight,
                    trackColor =
                        FinFlowSurfaceSoft,
                )

                Spacer(
                    modifier =
                        Modifier.size(9.dp),
                )

                Text(
                    text =
                        "Анализирую финансовый контекст...",
                    color =
                        FinFlowTextSecondary,
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QuickQuestionButton(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier.fillMaxWidth(),
        color =
            FinFlowSurfaceElevated,
        shape =
            MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color =
                FinFlowBorder.copy(
                    alpha = 0.75f,
                ),
        ),
    ) {
        Text(
            text = text,
            modifier =
                Modifier.padding(
                    horizontal = 13.dp,
                    vertical = 11.dp,
                ),
            color =
                FinFlowTextPrimary,
            style =
                MaterialTheme.typography
                    .bodyMedium,
        )
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment =
            Alignment.Center,
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color =
                    FinFlowPrimary,
                trackColor =
                    FinFlowSurfaceSoft,
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp),
            )

            Text(
                text =
                    "Готовим финансовый контекст",
                color =
                    FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .titleMedium,
                fontWeight =
                    FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyState(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        modifier = modifier,
        symbol = "+",
        title =
            "Нет активного месяца",
        description =
            "Создайте финансовый месяц, прежде чем использовать ИИ-анализ.",
        primaryText =
            "Вернуться",
        onPrimaryClick =
            onBackClick,
    )
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredState(
        modifier = modifier,
        symbol = "!",
        title =
            "Анализ временно недоступен",
        description =
            message
                ?: "Попробуйте ещё раз.",
        primaryText =
            "Повторить",
        onPrimaryClick =
            onRetry,
        secondaryText =
            "Вернуться",
        onSecondaryClick =
            onBackClick,
    )
}

@Composable
private fun CenteredState(
    symbol: String,
    title: String,
    description: String,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondaryClick:
    (() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier.padding(24.dp),
        contentAlignment =
            Alignment.Center,
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier =
                    Modifier.size(52.dp),
                color =
                    FinFlowPrimary.copy(
                        alpha = 0.15f,
                    ),
                shape = CircleShape,
            ) {
                Box(
                    contentAlignment =
                        Alignment.Center,
                ) {
                    Text(
                        text = symbol,
                        color =
                            FinFlowPrimaryLight,
                        fontSize = 22.sp,
                        fontWeight =
                            FontWeight.Bold,
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(18.dp),
            )

            Text(
                text = title,
                color =
                    FinFlowTextPrimary,
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center,
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp),
            )

            Text(
                text = description,
                color =
                    FinFlowTextSecondary,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                textAlign =
                    TextAlign.Center,
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp),
            )

            Button(
                onClick =
                    onPrimaryClick,
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                FinFlowPrimary,
                            contentColor =
                                FinFlowTextPrimary,
                        ),
            ) {
                Text(
                    text =
                        primaryText,
                )
            }

            if (
                secondaryText != null &&
                onSecondaryClick != null
            ) {
                Spacer(
                    modifier =
                        Modifier.height(8.dp),
                )

                Button(
                    onClick =
                        onSecondaryClick,
                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    FinFlowSurface,
                                contentColor =
                                    FinFlowTextPrimary,
                            ),
                ) {
                    Text(
                        text =
                            secondaryText,
                    )
                }
            }
        }
    }
}

private fun Long.toGigabyteText(): String {
    if (this <= 0L) {
        return "—"
    }

    val gigabytes =
        this.toDouble() /
                1_000_000_000.0

    return String.format(
        java.util.Locale.forLanguageTag(
            "ru-RU",
        ),
        "%.1f ГБ",
        gigabytes,
    )
}

private val CHAT_PANEL_HEIGHT =
    570.dp

private fun String.toAssistantAnnotatedText():
        AnnotatedString {
    val sourceLines =
        lines()

    return buildAnnotatedString {
        sourceLines.forEachIndexed {
                index,
                sourceLine,
            ->
            val trimmed =
                sourceLine.trimStart()

            val line =
                when {
                    trimmed.startsWith("* ") -> {
                        "• " +
                                trimmed
                                    .removePrefix("* ")
                    }

                    trimmed.startsWith("- ") -> {
                        "• " +
                                trimmed
                                    .removePrefix("- ")
                    }

                    trimmed.startsWith("### ") -> {
                        trimmed.removePrefix("### ")
                    }

                    trimmed.startsWith("## ") -> {
                        trimmed.removePrefix("## ")
                    }

                    trimmed.startsWith("# ") -> {
                        trimmed.removePrefix("# ")
                    }

                    else -> {
                        sourceLine
                    }
                }

            val isHeading =
                trimmed.startsWith("#")

            if (isHeading) {
                pushStyle(
                    SpanStyle(
                        fontWeight =
                            FontWeight.Bold,
                    ),
                )
            }

            appendMarkdownBold(
                line = line,
            )

            if (isHeading) {
                pop()
            }

            if (index != sourceLines.lastIndex) {
                append('\n')
            }
        }
    }
}

private fun AnnotatedString.Builder.appendMarkdownBold(
    line: String,
) {
    var cursor = 0

    while (cursor < line.length) {
        val boldStart =
            line.indexOf(
                string = "**",
                startIndex = cursor,
            )

        if (boldStart < 0) {
            append(
                line.substring(cursor),
            )
            return
        }

        append(
            line.substring(
                startIndex = cursor,
                endIndex = boldStart,
            ),
        )

        val boldEnd =
            line.indexOf(
                string = "**",
                startIndex =
                    boldStart + 2,
            )

        if (boldEnd < 0) {
            append(
                line.substring(boldStart),
            )
            return
        }

        pushStyle(
            SpanStyle(
                fontWeight =
                    FontWeight.Bold,
            ),
        )

        append(
            line.substring(
                startIndex =
                    boldStart + 2,
                endIndex = boldEnd,
            ),
        )

        pop()

        cursor =
            boldEnd + 2
    }
}

@Composable
private fun AssistantAuroraBackground() {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "assistantAuroraBackground",
        )

    val movement by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 16_000,
                        easing = LinearEasing,
                    ),
                repeatMode =
                    RepeatMode.Reverse,
            ),
        label = "assistantAuroraMovement",
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
                    colors =
                        listOf(
                            FinFlowGlowPrimary,
                            Color.Transparent,
                        ),
                    center = primaryCenter,
                    radius = radius,
                ),
            radius = radius,
            center = primaryCenter,
        )

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            FinFlowGlowSecondary,
                            Color.Transparent,
                        ),
                    center = secondaryCenter,
                    radius = radius,
                ),
            radius = radius,
            center = secondaryCenter,
        )

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            FinFlowGlowTertiary,
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

@Preview(
    name = "FinFlow AI",
    showBackground = true,
    backgroundColor = 0xFF09111A,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun AssistantScreenPreview() {
    FinFlowTheme {
        AssistantScreen(
            uiState =
                AssistantUiState(
                    status =
                        AssistantUiStatus.CONTENT,
                    monthLabel =
                        "Ноябрь 2026",
                    questionInput =
                        "Почему у меня выросли расходы?",
                ),
            onAction = {},
        )
    }
}