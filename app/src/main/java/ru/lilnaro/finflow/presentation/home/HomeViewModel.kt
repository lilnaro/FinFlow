package ru.lilnaro.finflow.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.math.MathContext
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.MonthSummary
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveMonthSummaryUseCase
import ru.lilnaro.finflow.presentation.home.model.HomeAction
import ru.lilnaro.finflow.presentation.home.model.HomeEffect
import ru.lilnaro.finflow.presentation.home.model.HomeUiState
import ru.lilnaro.finflow.presentation.home.model.HomeUiStatus

class HomeViewModel(
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val observeActiveMonthSummaryUseCase:
    ObserveActiveMonthSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(),
    )

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(
        capacity = Channel.BUFFERED,
    )

    val effect: Flow<HomeEffect> =
        _effect.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        observeHome()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.TransactionsClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToTransactions,
                )
            }

            HomeAction.AssistantClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToAssistant,
                )
            }

            HomeAction.NewMonthClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToNewMonth,
                )
            }

            HomeAction.ArchiveClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToArchive,
                )
            }

            HomeAction.RetryClicked -> {
                observeHome()
            }
        }
    }

    private fun observeHome() {
        observationJob?.cancel()

        _uiState.value = HomeUiState(
            status = HomeUiStatus.LOADING,
            greeting = createGreeting(),
        )

        observationJob = viewModelScope.launch {
            combine(
                observeActiveFinancialMonthUseCase(),
                observeActiveMonthSummaryUseCase(),
            ) { financialMonth, monthSummary ->
                financialMonth to monthSummary
            }
                .catch {
                    _uiState.value = HomeUiState(
                        status = HomeUiStatus.ERROR,
                        greeting = createGreeting(),
                        errorMessage =
                            "Не удалось загрузить данные. Попробуйте ещё раз.",
                    )
                }
                .collect { (financialMonth, monthSummary) ->
                    _uiState.value = when {
                        financialMonth == null -> {
                            HomeUiState(
                                status =
                                    HomeUiStatus.NO_ACTIVE_MONTH,
                                greeting = createGreeting(),
                            )
                        }

                        monthSummary == null ||
                                monthSummary.financialMonthId !=
                                financialMonth.id -> {
                            HomeUiState(
                                status = HomeUiStatus.LOADING,
                                greeting = createGreeting(),
                                monthLabel = createMonthLabel(
                                    monthNumber =
                                        financialMonth.monthNumber,
                                ),
                            )
                        }

                        else -> {
                            createContentState(
                                monthNumber =
                                    financialMonth.monthNumber,
                                monthSummary = monthSummary,
                            )
                        }
                    }
                }
        }
    }

    private fun createContentState(
        monthNumber: Int,
        monthSummary: MonthSummary,
    ): HomeUiState {
        val currentBalance =
            monthSummary.initialBudget
                .add(monthSummary.totalIncome)
                .subtract(monthSummary.totalExpense)

        val balanceChange =
            currentBalance.subtract(
                monthSummary.initialBudget,
            )

        val balanceChangePercent =
            calculatePercent(
                value = balanceChange,
                base = monthSummary.initialBudget,
            )

        val budgetRemainingPercent =
            calculatePercent(
                value = currentBalance,
                base = monthSummary.initialBudget,
            )

        return HomeUiState(
            status = HomeUiStatus.CONTENT,
            greeting = createGreeting(),
            monthLabel = createMonthLabel(
                monthNumber = monthNumber,
            ),
            initialBudget = monthSummary.initialBudget,
            currentBalance = currentBalance,
            totalIncome = monthSummary.totalIncome,
            totalExpense = monthSummary.totalExpense,
            balanceChangePercent = balanceChangePercent,
            budgetRemainingPercent =
                budgetRemainingPercent,
            errorMessage = null,
        )
    }

    private fun calculatePercent(
        value: BigDecimal,
        base: BigDecimal,
    ): Double {
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0
        }

        return value
            .divide(
                base,
                MathContext.DECIMAL64,
            )
            .multiply(PERCENT_MULTIPLIER)
            .toDouble()
    }

    private fun createGreeting(): String {
        val currentHour =
            Calendar.getInstance().get(
                Calendar.HOUR_OF_DAY,
            )

        return when (currentHour) {
            in 5..11 -> "Доброе утро"
            in 12..17 -> "Добрый день"
            in 18..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
    }

    private fun createMonthLabel(
        monthNumber: Int,
    ): String {
        return MONTH_NAMES.getOrElse(
            index = monthNumber - 1,
        ) {
            "Месяц $monthNumber"
        }
    }

    private companion object {

        val PERCENT_MULTIPLIER =
            BigDecimal("100")

        val MONTH_NAMES = listOf(
            "Январь",
            "Февраль",
            "Март",
            "Апрель",
            "Май",
            "Июнь",
            "Июль",
            "Август",
            "Сентябрь",
            "Октябрь",
            "Ноябрь",
            "Декабрь",
        )
    }
}