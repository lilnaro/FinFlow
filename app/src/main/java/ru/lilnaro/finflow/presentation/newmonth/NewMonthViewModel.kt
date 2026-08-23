package ru.lilnaro.finflow.presentation.newmonth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import java.util.Calendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private var existingPeriods:
            Set<FinancialMonthPeriod> =
        emptySet()

    private val _uiState =
        MutableStateFlow(
            NewMonthUiState(
                year = currentYear,
                monthNumber = currentMonthNumber,
                monthLabel =
                    "Определяем период…",
                availableMonthNumbers =
                    createAvailableMonthNumbers(
                        year = currentYear,
                        occupiedPeriods =
                            emptySet(),
                    ),
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
        resolvePeriodOptions()
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

            is NewMonthAction.MonthSelected -> {
                selectMonth(
                    monthNumber =
                        action.monthNumber,
                )
            }

            NewMonthAction.CreateMonthClicked -> {
                createFinancialMonth()
            }
        }
    }

    private fun resolvePeriodOptions() {
        viewModelScope.launch {
            val archivedMonths =
                try {
                    observeArchivedFinancialMonthsUseCase()
                        .first()
                } catch (_: Exception) {
                    _effect.send(
                        NewMonthEffect.ShowMessage(
                            message =
                                "Не удалось проверить историю месяцев. Доступность периода дополнительно проверится при создании.",
                        ),
                    )

                    emptyList()
                }

            existingPeriods =
                archivedMonths.map { financialMonth ->
                    FinancialMonthPeriod(
                        year =
                            financialMonth.year,
                        monthNumber =
                            financialMonth.monthNumber,
                    )
                }.toSet()

            val suggestedPeriod =
                findNewestAvailablePeriod(
                    occupiedPeriods =
                        existingPeriods,
                )

            if (suggestedPeriod == null) {
                _uiState.value =
                    _uiState.value.copy(
                        availableMonthNumbers =
                            emptyList(),
                        monthLabel =
                            "Нет доступного периода",
                        periodError =
                            "Не удалось определить текущий или следующий месяц.",
                        isPeriodLoading = false,
                    )

                return@launch
            }

            val suggestedMonthLabel =
                createMonthLabel(
                    monthNumber =
                        suggestedPeriod.monthNumber,
                    year =
                        suggestedPeriod.year,
                )

            _uiState.value =
                _uiState.value.copy(
                    year = suggestedPeriod.year,
                    monthNumber =
                        suggestedPeriod.monthNumber,
                    monthLabel =
                        suggestedMonthLabel,
                    availableMonthNumbers =
                        createAvailableMonthNumbers(
                            year =
                                suggestedPeriod.year,
                            occupiedPeriods =
                                existingPeriods,
                        ),
                    periodError =
                        if (
                            suggestedPeriod in
                            existingPeriods
                        ) {
                            if (
                                currentMonthNumber ==
                                MONTHS_IN_YEAR
                            ) {
                                "Текущий месяц уже создан."
                            } else {
                                "Текущий и следующий месяцы уже созданы."
                            }
                        } else {
                            null
                        },
                    isPeriodLoading = false,
                )
        }
    }

    private fun selectMonth(
        monthNumber: Int,
    ) {
        val currentState =
            _uiState.value

        if (
            currentState.isSaving ||
            currentState.isPeriodLoading
        ) {
            return
        }

        if (
            monthNumber !in
            currentState.availableMonthNumbers
        ) {
            return
        }

        val selectedPeriod =
            FinancialMonthPeriod(
                year = currentState.year,
                monthNumber =
                    monthNumber,
            )

        val selectedMonthLabel =
            createMonthLabel(
                monthNumber =
                    monthNumber,
                year =
                    currentState.year,
            )

        _uiState.value =
            currentState.copy(
                monthNumber = monthNumber,
                monthLabel =
                    selectedMonthLabel,
                periodError =
                    if (
                        selectedPeriod in
                        existingPeriods
                    ) {
                        "Финансовый месяц $selectedMonthLabel уже существует."
                    } else {
                        null
                    },
            )
    }

    private fun createAvailableMonthNumbers(
        year: Int,
        occupiedPeriods:
        Set<FinancialMonthPeriod>,
    ): List<Int> {
        if (year != currentYear) {
            return emptyList()
        }

        return createSupportedPeriods()
            .map { period ->
                period.monthNumber
            }
    }

    private fun createSupportedPeriods():
            List<FinancialMonthPeriod> {
        val currentPeriod =
            FinancialMonthPeriod(
                year = currentYear,
                monthNumber =
                    currentMonthNumber,
            )

        if (
            currentMonthNumber ==
            MONTHS_IN_YEAR
        ) {
            return listOf(
                currentPeriod,
            )
        }

        return listOf(
            currentPeriod,
            FinancialMonthPeriod(
                year = currentYear,
                monthNumber =
                    currentMonthNumber + 1,
            ),
        )
    }

    private fun findNewestAvailablePeriod(
        occupiedPeriods:
        Set<FinancialMonthPeriod>,
    ): FinancialMonthPeriod? {
        val supportedPeriods =
            createSupportedPeriods()

        return supportedPeriods
            .firstOrNull { period ->
                period !in occupiedPeriods
            }
            ?: supportedPeriods.lastOrNull()
    }

    private fun validateSelectedPeriod():
            String? {
        val currentState =
            _uiState.value

        val selectedPeriod =
            FinancialMonthPeriod(
                year = currentState.year,
                monthNumber =
                    currentState.monthNumber,
            )

        if (
            selectedPeriod.year !=
            currentYear
        ) {
            return "Год определяется автоматически по дате телефона."
        }

        if (
            selectedPeriod !in
            createSupportedPeriods()
        ) {
            return if (
                currentMonthNumber ==
                MONTHS_IN_YEAR
            ) {
                "В декабре доступен только текущий месяц этого года."
            } else {
                "Можно выбрать только текущий или следующий месяц."
            }
        }

        if (
            selectedPeriod in
            existingPeriods
        ) {
            return "Финансовый месяц ${currentState.monthLabel} уже существует."
        }

        return null
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

        val periodError =
            validateSelectedPeriod()

        if (periodError != null) {
            _uiState.value =
                currentState.copy(
                    periodError =
                        periodError,
                )

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
                periodError = null,
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
                        "Выбранный год находится вне поддерживаемого диапазона.",
                )
            }

            is CreateFinancialMonthResult.InvalidMonthNumber -> {
                finishSavingWithMessage(
                    message =
                        "Выбран некорректный месяц.",
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
                _uiState.value =
                    _uiState.value.copy(
                        periodError =
                            "Финансовый месяц ${_uiState.value.monthLabel} уже существует.",
                        isSaving = false,
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

        const val MONTHS_IN_YEAR = 12

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
