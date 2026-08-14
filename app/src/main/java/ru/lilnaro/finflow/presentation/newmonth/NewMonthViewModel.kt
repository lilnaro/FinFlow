package ru.lilnaro.finflow.presentation.newmonth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.util.Calendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.result.CreateFinancialMonthResult
import ru.lilnaro.finflow.domain.usecase.CreateFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveArchivedFinancialMonthsUseCase
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthAction
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthEffect
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthUiState

class NewMonthViewModel(
    private val createFinancialMonthUseCase:
    CreateFinancialMonthUseCase,
    private val observeArchivedFinancialMonthsUseCase:
    ObserveArchivedFinancialMonthsUseCase,
) : ViewModel() {

    private val currentCalendar =
        Calendar.getInstance()

    private val currentYear =
        currentCalendar.get(
            Calendar.YEAR,
        )

    private val currentMonthNumber =
        currentCalendar.get(
            Calendar.MONTH,
        ) + 1

    private val _uiState =
        MutableStateFlow(
            NewMonthUiState(
                year = currentYear,
                monthNumber = currentMonthNumber,
                monthLabel =
                    "Определяем период…",
                isPeriodLoading = true,
            ),
        )

    val uiState: StateFlow<NewMonthUiState> =
        _uiState.asStateFlow()

    private val _effect =
        Channel<NewMonthEffect>(
            capacity = Channel.BUFFERED,
        )

    val effect: Flow<NewMonthEffect> =
        _effect.receiveAsFlow()

    init {
        resolveSuggestedPeriod()
    }

    fun onAction(
        action: NewMonthAction,
    ) {
        when (action) {
            NewMonthAction.BackClicked -> {
                handleBackClick()
            }

            is NewMonthAction.InitialBudgetChanged -> {
                changeInitialBudget(
                    rawValue = action.value,
                )
            }

            NewMonthAction.CreateMonthClicked -> {
                createFinancialMonth()
            }
        }
    }

    private fun resolveSuggestedPeriod() {
        viewModelScope.launch {
            val suggestedPeriod =
                try {
                    val archivedMonths =
                        observeArchivedFinancialMonthsUseCase()
                            .first()

                    calculateSuggestedPeriod(
                        archivedMonths =
                            archivedMonths,
                    )
                } catch (_: Exception) {
                    _effect.send(
                        NewMonthEffect.ShowMessage(
                            message =
                                "Не удалось проверить историю месяцев. Используем текущий период.",
                        ),
                    )

                    FinancialMonthPeriod(
                        year = currentYear,
                        monthNumber =
                            currentMonthNumber,
                    )
                }

            _uiState.value =
                _uiState.value.copy(
                    year = suggestedPeriod.year,
                    monthNumber =
                        suggestedPeriod.monthNumber,
                    monthLabel =
                        createMonthLabel(
                            monthNumber =
                                suggestedPeriod.monthNumber,
                            year =
                                suggestedPeriod.year,
                        ),
                    isPeriodLoading = false,
                )
        }
    }

    private fun calculateSuggestedPeriod(
        archivedMonths: List<FinancialMonth>,
    ): FinancialMonthPeriod {
        val currentPeriod =
            FinancialMonthPeriod(
                year = currentYear,
                monthNumber =
                    currentMonthNumber,
            )

        val latestArchivedMonth =
            archivedMonths.maxWithOrNull(
                compareBy<FinancialMonth>(
                    { financialMonth ->
                        financialMonth.year
                    },
                    { financialMonth ->
                        financialMonth.monthNumber
                    },
                ),
            ) ?: return currentPeriod

        val nextAfterLatest =
            if (
                latestArchivedMonth.monthNumber == 12
            ) {
                FinancialMonthPeriod(
                    year =
                        latestArchivedMonth.year + 1,
                    monthNumber = 1,
                )
            } else {
                FinancialMonthPeriod(
                    year = latestArchivedMonth.year,
                    monthNumber =
                        latestArchivedMonth.monthNumber + 1,
                )
            }

        return if (
            nextAfterLatest.toPeriodIndex() >
            currentPeriod.toPeriodIndex()
        ) {
            nextAfterLatest
        } else {
            currentPeriod
        }
    }

    private fun FinancialMonthPeriod.toPeriodIndex(): Long {
        return year.toLong() * MONTHS_IN_YEAR +
                monthNumber
    }

    private fun handleBackClick() {
        if (_uiState.value.isSaving) {
            return
        }

        _effect.trySend(
            NewMonthEffect.NavigateBack,
        )
    }

    private fun changeInitialBudget(
        rawValue: String,
    ) {
        if (_uiState.value.isSaving) {
            return
        }

        val normalizedInput =
            rawValue
                .replace(
                    " ",
                    "",
                )
                .replace(
                    "\u00A0",
                    "",
                )

        if (
            normalizedInput.length >
            MAX_BUDGET_INPUT_LENGTH
        ) {
            _uiState.value =
                _uiState.value.copy(
                    budgetError =
                        MAX_BUDGET_ERROR_MESSAGE,
                )

            return
        }

        if (
            normalizedInput.isNotEmpty() &&
            !BUDGET_INPUT_REGEX.matches(
                normalizedInput,
            )
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                initialBudgetInput =
                    normalizedInput,
                budgetError =
                    validateBudgetWhileTyping(
                        input =
                            normalizedInput,
                    ),
            )
    }

    private fun createFinancialMonth() {
        val currentState =
            _uiState.value

        if (
            currentState.isSaving ||
            currentState.isPeriodLoading
        ) {
            return
        }

        val validationResult =
            validateBudgetForSaving(
                input =
                    currentState.initialBudgetInput,
            )

        val initialBudget =
            validationResult.initialBudget

        if (
            initialBudget == null ||
            validationResult.errorMessage != null
        ) {
            _uiState.value =
                currentState.copy(
                    budgetError =
                        validationResult.errorMessage,
                )

            return
        }

        _uiState.value =
            currentState.copy(
                budgetError = null,
                isSaving = true,
            )

        viewModelScope.launch {
            val startedAtMillis =
                System.currentTimeMillis()

            val result =
                try {
                    createFinancialMonthUseCase(
                        year =
                            currentState.year,
                        monthNumber =
                            currentState.monthNumber,
                        initialBudget =
                            initialBudget,
                        startedAtMillis =
                            startedAtMillis,
                    )
                } catch (_: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                        )

                    _effect.send(
                        NewMonthEffect.ShowMessage(
                            message =
                                "Не удалось создать финансовый месяц.",
                        ),
                    )

                    return@launch
                }

            handleCreateResult(
                result = result,
            )
        }
    }

    private suspend fun handleCreateResult(
        result: CreateFinancialMonthResult,
    ) {
        when (result) {
            is CreateFinancialMonthResult.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                    )

                _effect.send(
                    NewMonthEffect.MonthCreated(
                        message =
                            "Финансовый месяц создан.",
                    ),
                )
            }

            is CreateFinancialMonthResult.YearOutOfRange -> {
                finishSavingWithMessage(
                    message =
                        "Текущий год находится вне поддерживаемого диапазона.",
                )
            }

            is CreateFinancialMonthResult.InvalidMonthNumber -> {
                finishSavingWithMessage(
                    message =
                        "Не удалось определить текущий месяц.",
                )
            }

            is CreateFinancialMonthResult.NegativeInitialBudget -> {
                _uiState.value =
                    _uiState.value.copy(
                        budgetError =
                            "Стартовый бюджет не может быть отрицательным.",
                        isSaving = false,
                    )
            }

            is CreateFinancialMonthResult.InvalidStartedAtMillis -> {
                finishSavingWithMessage(
                    message =
                        "Не удалось определить время начала месяца.",
                )
            }

            is CreateFinancialMonthResult
            .ActiveFinancialMonthAlreadyExists -> {
                finishSavingWithMessage(
                    message =
                        "Активный финансовый месяц уже существует.",
                )
            }

            is CreateFinancialMonthResult
            .FinancialMonthAlreadyExists -> {
                finishSavingWithMessage(
                    message =
                        "Финансовый месяц ${_uiState.value.monthLabel} уже существует.",
                )
            }
        }
    }

    private suspend fun finishSavingWithMessage(
        message: String,
    ) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false,
            )

        _effect.send(
            NewMonthEffect.ShowMessage(
                message = message,
            ),
        )
    }

    private fun validateBudgetWhileTyping(
        input: String,
    ): String? {
        if (input.isBlank()) {
            return null
        }

        if (
            input.endsWith(",") ||
            input.endsWith(".")
        ) {
            return null
        }

        val budget =
            input.toBudgetOrNull()
                ?: return "Введите корректный бюджет."

        return if (
            budget >
            MAX_INITIAL_BUDGET
        ) {
            MAX_BUDGET_ERROR_MESSAGE
        } else {
            null
        }
    }

    private fun validateBudgetForSaving(
        input: String,
    ): BudgetValidationResult {
        if (input.isBlank()) {
            return BudgetValidationResult(
                initialBudget = null,
                errorMessage =
                    "Введите стартовый бюджет.",
            )
        }

        if (
            input.endsWith(",") ||
            input.endsWith(".")
        ) {
            return BudgetValidationResult(
                initialBudget = null,
                errorMessage =
                    "Завершите ввод бюджета.",
            )
        }

        val budget =
            input.toBudgetOrNull()
                ?: return BudgetValidationResult(
                    initialBudget = null,
                    errorMessage =
                        "Введите корректный бюджет.",
                )

        if (budget < BigDecimal.ZERO) {
            return BudgetValidationResult(
                initialBudget = null,
                errorMessage =
                    "Стартовый бюджет не может быть отрицательным.",
            )
        }

        if (
            budget >
            MAX_INITIAL_BUDGET
        ) {
            return BudgetValidationResult(
                initialBudget = null,
                errorMessage =
                    MAX_BUDGET_ERROR_MESSAGE,
            )
        }

        return BudgetValidationResult(
            initialBudget = budget,
            errorMessage = null,
        )
    }

    private fun String.toBudgetOrNull():
            BigDecimal? {
        val normalized =
            replace(
                oldChar = ',',
                newChar = '.',
            ).let { value ->
                if (
                    value.startsWith(".")
                ) {
                    "0$value"
                } else {
                    value
                }
            }

        return normalized.toBigDecimalOrNull()
    }

    private fun createMonthLabel(
        monthNumber: Int,
        year: Int,
    ): String {
        val monthName =
            MONTH_NAMES.getOrElse(
                index = monthNumber - 1,
            ) {
                "Месяц $monthNumber"
            }

        return "$monthName $year"
    }

    private data class FinancialMonthPeriod(
        val year: Int,
        val monthNumber: Int,
    )

    private data class BudgetValidationResult(
        val initialBudget: BigDecimal?,
        val errorMessage: String?,
    )

    private companion object {

        const val MONTHS_IN_YEAR = 12L

        val MAX_INITIAL_BUDGET =
            BigDecimal("999999999999.99")

        const val MAX_BUDGET_INPUT_LENGTH =
            15

        const val MAX_BUDGET_ERROR_MESSAGE =
            "Максимальный бюджет — 999 999 999 999,99 ₽."

        val BUDGET_INPUT_REGEX =
            Regex(
                pattern =
                    """^\d*([.,]\d{0,2})?$""",
            )

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