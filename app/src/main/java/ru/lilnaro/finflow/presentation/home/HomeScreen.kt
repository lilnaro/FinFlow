package ru.lilnaro.finflow.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import ru.lilnaro.finflow.presentation.home.model.HomeAction
import ru.lilnaro.finflow.presentation.home.model.HomeUiState
import ru.lilnaro.finflow.presentation.home.style.HomeColors
import ru.lilnaro.finflow.ui.theme.FinFlowTheme

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HomeColors.AppBackground,
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                color = HomeColors.MainCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = HomeColors.Border,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                ) {
                    Text(
                        text = "FinFlow",
                        modifier = Modifier.fillMaxWidth(),
                        color = HomeColors.PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    BudgetInformation(
                        uiState = uiState,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    BudgetChart(
                        initialBudget = uiState.initialBudget,
                        currentBalance = uiState.currentBalance,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HomeMenuButton(
                        symbol = "☷",
                        title = "Транзакции",
                        onClick = {
                            onAction(HomeAction.TransactionsClicked)
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HomeMenuButton(
                        symbol = "◯",
                        title = "ИИ ассистент",
                        onClick = {
                            onAction(HomeAction.AssistantClicked)
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HomeMenuButton(
                        symbol = "▣",
                        title = "Новый месяц",
                        onClick = {
                            onAction(HomeAction.NewMonthClicked)
                        },
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HomeMenuButton(
                        symbol = "▤",
                        title = "Архив месяцев",
                        onClick = {
                            onAction(HomeAction.ArchiveClicked)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetInformation(
    uiState: HomeUiState,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Сумма в начале месяца",
            color = HomeColors.SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = uiState.initialBudget.toRubleText(),
            color = HomeColors.PrimaryText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Актуальный остаток",
            color = HomeColors.SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = uiState.currentBalance.toRubleText(),
            color = HomeColors.PrimaryText,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = uiState.balanceChangePercent.toPercentText(),
            color = uiState.balanceChangePercent.toChangeColor(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BudgetChart(
    initialBudget: BigDecimal,
    currentBalance: BigDecimal,
) {
    val maximumValue = maxOf(
        initialBudget,
        currentBalance,
        BigDecimal.ONE,
    )

    val initialBudgetRatio =
        initialBudget
            .coerceAtLeast(BigDecimal.ZERO)
            .divide(
                maximumValue,
                MathContext.DECIMAL64,
            )
            .toFloat()

    val currentBalanceRatio =
        currentBalance
            .coerceAtLeast(BigDecimal.ZERO)
            .divide(
                maximumValue,
                MathContext.DECIMAL64,
            )
            .toFloat()

    val maximumBarHeight = 72f

    val initialBarHeight =
        (maximumBarHeight * initialBudgetRatio).dp

    val currentBarHeight =
        (maximumBarHeight * currentBalanceRatio).dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
        color = HomeColors.ChartBackground,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartColumn(
                label = "начало",
                barColor = HomeColors.InitialBar,
                barHeight = initialBarHeight,
            )

            ChartColumn(
                label = "остаток",
                barColor = HomeColors.CurrentBar,
                barHeight = currentBarHeight,
            )
        }
    }
}

@Composable
private fun ChartColumn(
    label: String,
    barColor: Color,
    barHeight: Dp,
) {
    val displayedBarHeight = if (barHeight < 6.dp) {
        6.dp
    } else {
        barHeight
    }

    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(72.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(displayedBarHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor),
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = label,
            color = HomeColors.ChartLabel,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeMenuButton(
    symbol: String,
    title: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = HomeColors.Border,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = HomeColors.PrimaryText,
        ),
        contentPadding = PaddingValues(
            horizontal = 14.dp,
            vertical = 0.dp,
        ),
    ) {
        Text(
            text = symbol,
            modifier = Modifier.width(28.dp),
            color = HomeColors.PrimaryText,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = HomeColors.PrimaryText,
            fontSize = 14.sp,
            textAlign = TextAlign.Start,
        )
    }
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

    return "${formatter.format(this)} ₽"
}

private fun Double.toPercentText(): String {
    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("ru-RU"),
    ).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }

    val sign = if (this > 0) "+" else ""

    return "$sign${formatter.format(this)}%"
}

private fun Double.toChangeColor(): Color {
    return when {
        this > 0 -> HomeColors.Positive
        this < 0 -> HomeColors.Negative
        else -> HomeColors.SecondaryText
    }
}

@Preview(
    name = "Главный экран FinFlow",
    showBackground = true,
    backgroundColor = 0xFF171817,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun HomeScreenPreview() {
    FinFlowTheme {
        HomeScreen(
            uiState = HomeUiState(),
            onAction = {},
        )
    }
}