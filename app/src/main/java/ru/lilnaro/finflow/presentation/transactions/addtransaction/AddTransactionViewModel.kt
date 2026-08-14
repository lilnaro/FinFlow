package ru.lilnaro.finflow.presentation.transactions.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.lilnaro.finflow.domain.model.FinancialMonth
import ru.lilnaro.finflow.domain.model.Transaction
import ru.lilnaro.finflow.domain.model.TransactionCategory
import ru.lilnaro.finflow.domain.model.TransactionType
import ru.lilnaro.finflow.domain.model.result.AddTransactionResult
import ru.lilnaro.finflow.domain.usecase.AddTransactionUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveCategoriesByTypeUseCase
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionAction
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionCategoryUiModel
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionEffect
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionUiState
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionUiStatus

class AddTransactionViewModel(
    private val observeActiveFinancialMonthUseCase:
    ObserveActiveFinancialMonthUseCase,
    private val observeCategoriesByTypeUseCase:
    ObserveCategoriesByTypeUseCase,
    private val addTransactionUseCase:
    AddTransactionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(),
    )

    val uiState: StateFlow<AddTransactionUiState> =
        _uiState.asStateFlow()

    private val _effect =
        Channel<AddTransactionEffect>(
            capacity = Channel.BUFFERED,
        )

    val effect: Flow<AddTransactionEffect> =
        _effect.receiveAsFlow()

    private val selectedType =
        MutableStateFlow(
            TransactionType.EXPENSE,
        )

    private var activeFinancialMonthId: Long? =
        null

    private var observationJob: Job? = null

    init {
        observeFormData()
    }

    fun onAction(
        action: AddTransactionAction,
    ) {
        when (action) {
            AddTransactionAction.BackClicked -> {
                _effect.trySend(
                    AddTransactionEffect.NavigateBack,
                )
            }

            AddTransactionAction.RetryClicked -> {
                observeFormData()
            }

            is AddTransactionAction.TypeChanged -> {
                changeType(
                    type = action.type,
                )
            }

            is AddTransactionAction.AmountChanged -> {
                changeAmount(
                    rawValue = action.value,
                )
            }

            is AddTransactionAction.CategorySelected -> {
                selectCategory(
                    categoryId = action.categoryId,
                )
            }

            is AddTransactionAction.NoteChanged -> {
                changeNote(
                    value = action.value,
                )
            }

            AddTransactionAction.SaveClicked -> {
                saveTransaction()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFormData() {
        observationJob?.cancel()

        _uiState.value =
            _uiState.value.copy(
                status =
                    AddTransactionUiStatus.LOADING,
                errorMessage = null,
                isSaving = false,
            )

        observationJob =
            viewModelScope.launch {
                combine(
                    observeActiveFinancialMonthUseCase(),
                    selectedType,
                ) { financialMonth, type ->
                    financialMonth to type
                }
                    .flatMapLatest {
                            (financialMonth, type) ->

                        if (financialMonth == null) {
                            flowOf(
                                AddTransactionSnapshot(
                                    financialMonth = null,
                                    type = type,
                                    categories = emptyList(),
                                ),
                            )
                        } else {
                            observeCategoriesByTypeUseCase(
                                type = type,
                            ).map { categories ->
                                AddTransactionSnapshot(
                                    financialMonth =
                                        financialMonth,
                                    type = type,
                                    categories =
                                        categories,
                                )
                            }
                        }
                    }
                    .catch {
                        activeFinancialMonthId = null

                        _uiState.value =
                            _uiState.value.copy(
                                status =
                                    AddTransactionUiStatus.ERROR,
                                categories = emptyList(),
                                selectedCategoryId = null,
                                errorMessage =
                                    "Не удалось загрузить данные для новой транзакции.",
                                isSaving = false,
                            )
                    }
                    .collect { snapshot ->
                        updateFormData(
                            snapshot = snapshot,
                        )
                    }
            }
    }

    private fun updateFormData(
        snapshot: AddTransactionSnapshot,
    ) {
        val financialMonth =
            snapshot.financialMonth

        if (financialMonth == null) {
            activeFinancialMonthId = null

            _uiState.value =
                _uiState.value.copy(
                    status =
                        AddTransactionUiStatus
                            .NO_ACTIVE_MONTH,
                    monthLabel = "",
                    type = snapshot.type,
                    categories = emptyList(),
                    selectedCategoryId = null,
                    categoryError = null,
                    errorMessage = null,
                    isSaving = false,
                )

            return
        }

        activeFinancialMonthId =
            financialMonth.id

        val categoryUiModels =
            snapshot.categories.map { category ->
                category.toUiModel()
            }

        val currentSelectedCategoryId =
            _uiState.value.selectedCategoryId

        val validSelectedCategoryId =
            currentSelectedCategoryId
                ?.takeIf { selectedId ->
                    categoryUiModels.any { category ->
                        category.id == selectedId
                    }
                }

        _uiState.value =
            _uiState.value.copy(
                status =
                    AddTransactionUiStatus.CONTENT,
                monthLabel = createMonthLabel(
                    monthNumber =
                        financialMonth.monthNumber,
                ),
                type = snapshot.type,
                categories = categoryUiModels,
                selectedCategoryId =
                    validSelectedCategoryId,
                categoryError = null,
                errorMessage = null,
                isSaving = false,
            )
    }

    private fun changeType(
        type: TransactionType,
    ) {
        if (selectedType.value == type) {
            return
        }

        selectedType.value = type

        _uiState.value =
            _uiState.value.copy(
                type = type,
                categories = emptyList(),
                selectedCategoryId = null,
                categoryError = null,
            )
    }

    private fun changeAmount(
        rawValue: String,
    ) {
        val normalizedInput =
            rawValue
                .replace(" ", "")
                .replace(
                    "\u00A0",
                    "",
                )

        if (
            normalizedInput.length >
            MAX_AMOUNT_INPUT_LENGTH
        ) {
            _uiState.value =
                _uiState.value.copy(
                    amountError =
                        MAX_AMOUNT_ERROR_MESSAGE,
                )

            return
        }

        if (
            normalizedInput.isNotEmpty() &&
            !AMOUNT_INPUT_REGEX.matches(
                normalizedInput,
            )
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                amountInput =
                    normalizedInput,
                amountError =
                    validateAmountWhileTyping(
                        input = normalizedInput,
                    ),
            )
    }

    private fun selectCategory(
        categoryId: Long,
    ) {
        val categoryExists =
            _uiState.value.categories.any { category ->
                category.id == categoryId
            }

        if (!categoryExists) {
            _uiState.value =
                _uiState.value.copy(
                    selectedCategoryId = null,
                    categoryError =
                        "Выбранная категория недоступна.",
                )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                selectedCategoryId =
                    categoryId,
                categoryError = null,
            )
    }

    private fun changeNote(
        value: String,
    ) {
        if (value.length <= MAX_NOTE_LENGTH) {
            _uiState.value =
                _uiState.value.copy(
                    noteInput = value,
                )

            return
        }

        val previousLength =
            _uiState.value.noteInput.length

        _uiState.value =
            _uiState.value.copy(
                noteInput =
                    value.take(
                        MAX_NOTE_LENGTH,
                    ),
            )

        if (
            previousLength <
            MAX_NOTE_LENGTH
        ) {
            _effect.trySend(
                AddTransactionEffect.ShowMessage(
                    message =
                        "Комментарий — не более $MAX_NOTE_LENGTH символов.",
                ),
            )
        }
    }

    private fun saveTransaction() {
        val currentState =
            _uiState.value

        if (
            currentState.status !=
            AddTransactionUiStatus.CONTENT ||
            currentState.isSaving
        ) {
            return
        }

        val amountValidation =
            validateAmountForSaving(
                input =
                    currentState.amountInput,
            )

        val amount =
            amountValidation.amount

        if (
            amount == null ||
            amountValidation.errorMessage != null
        ) {
            _uiState.value =
                currentState.copy(
                    amountError =
                        amountValidation.errorMessage,
                )

            return
        }

        val category =
            currentState.selectedCategory

        if (category == null) {
            _uiState.value =
                currentState.copy(
                    categoryError =
                        "Выберите категорию.",
                )

            return
        }

        val financialMonthId =
            activeFinancialMonthId

        if (financialMonthId == null) {
            _uiState.value =
                currentState.copy(
                    status =
                        AddTransactionUiStatus
                            .NO_ACTIVE_MONTH,
                    isSaving = false,
                )

            _effect.trySend(
                AddTransactionEffect.ShowMessage(
                    message =
                        "Активный финансовый месяц больше недоступен.",
                ),
            )

            return
        }

        val transaction =
            Transaction(
                financialMonthId =
                    financialMonthId,
                amount = amount,
                type = currentState.type,
                categoryId = category.id,
                note =
                    currentState.noteInput.trim(),
                createdAtMillis =
                    System.currentTimeMillis(),
            )

        _uiState.value =
            currentState.copy(
                amountError = null,
                categoryError = null,
                isSaving = true,
            )

        viewModelScope.launch {
            val result =
                try {
                    addTransactionUseCase(
                        transaction = transaction,
                    )
                } catch (_: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isSaving = false,
                        )

                    _effect.send(
                        AddTransactionEffect.ShowMessage(
                            message =
                                "Не удалось сохранить транзакцию.",
                        ),
                    )

                    return@launch
                }

            handleSaveResult(
                result = result,
            )
        }
    }

    private suspend fun handleSaveResult(
        result: AddTransactionResult,
    ) {
        when (result) {
            is AddTransactionResult.Success -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                    )

                _effect.send(
                    AddTransactionEffect.TransactionSaved(
                        message =
                            "Транзакция добавлена.",
                    ),
                )
            }

            is AddTransactionResult
            .TransactionAlreadySaved -> {
                finishSavingWithMessage(
                    message =
                        "Эта транзакция уже была сохранена.",
                )
            }

            is AddTransactionResult
            .FinancialMonthNotFound -> {
                activeFinancialMonthId = null

                _uiState.value =
                    _uiState.value.copy(
                        status =
                            AddTransactionUiStatus
                                .NO_ACTIVE_MONTH,
                        isSaving = false,
                    )

                _effect.send(
                    AddTransactionEffect.ShowMessage(
                        message =
                            "Финансовый месяц больше недоступен.",
                    ),
                )
            }

            is AddTransactionResult
            .FinancialMonthClosed -> {
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                    )

                _effect.send(
                    AddTransactionEffect.ShowMessage(
                        message =
                            "Месяц уже закрыт. Добавление транзакций недоступно.",
                    ),
                )
            }

            is AddTransactionResult
            .CategoryNotFound -> {
                _uiState.value =
                    _uiState.value.copy(
                        selectedCategoryId = null,
                        categoryError =
                            "Выбранная категория больше недоступна.",
                        isSaving = false,
                    )
            }

            is AddTransactionResult
            .CategoryTypeMismatch -> {
                _uiState.value =
                    _uiState.value.copy(
                        selectedCategoryId = null,
                        categoryError =
                            "Категория не подходит для выбранного типа операции.",
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
            AddTransactionEffect.ShowMessage(
                message = message,
            ),
        )
    }

    private fun validateAmountWhileTyping(
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

        val amount =
            input.toAmountOrNull()
                ?: return "Введите корректную сумму."

        return when {
            amount <= BigDecimal.ZERO -> {
                "Сумма должна быть больше нуля."
            }

            amount > MAX_TRANSACTION_AMOUNT -> {
                MAX_AMOUNT_ERROR_MESSAGE
            }

            else -> null
        }
    }

    private fun validateAmountForSaving(
        input: String,
    ): AmountValidationResult {
        if (input.isBlank()) {
            return AmountValidationResult(
                amount = null,
                errorMessage =
                    "Введите сумму.",
            )
        }

        if (
            input.endsWith(",") ||
            input.endsWith(".")
        ) {
            return AmountValidationResult(
                amount = null,
                errorMessage =
                    "Завершите ввод суммы.",
            )
        }

        val amount =
            input.toAmountOrNull()
                ?: return AmountValidationResult(
                    amount = null,
                    errorMessage =
                        "Введите корректную сумму.",
                )

        if (amount <= BigDecimal.ZERO) {
            return AmountValidationResult(
                amount = null,
                errorMessage =
                    "Сумма должна быть больше нуля.",
            )
        }

        if (
            amount >
            MAX_TRANSACTION_AMOUNT
        ) {
            return AmountValidationResult(
                amount = null,
                errorMessage =
                    MAX_AMOUNT_ERROR_MESSAGE,
            )
        }

        return AmountValidationResult(
            amount = amount,
            errorMessage = null,
        )
    }

    private fun String.toAmountOrNull():
            BigDecimal? {
        val normalized =
            replace(
                oldChar = ',',
                newChar = '.',
            ).let { value ->
                if (value.startsWith(".")) {
                    "0$value"
                } else {
                    value
                }
            }

        return normalized.toBigDecimalOrNull()
    }

    private fun TransactionCategory.toUiModel():
            AddTransactionCategoryUiModel {
        return AddTransactionCategoryUiModel(
            id = id,
            name = name,
            isCustom = isCustom,
        )
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

    private data class AddTransactionSnapshot(
        val financialMonth: FinancialMonth?,
        val type: TransactionType,
        val categories:
        List<TransactionCategory>,
    )

    private data class AmountValidationResult(
        val amount: BigDecimal?,
        val errorMessage: String?,
    )

    private companion object {

        val MAX_TRANSACTION_AMOUNT =
            BigDecimal("999999999999.99")

        const val MAX_AMOUNT_INPUT_LENGTH =
            15

        const val MAX_NOTE_LENGTH =
            120

        val AMOUNT_INPUT_REGEX =
            Regex(
                pattern =
                    """^\d*([.,]\d{0,2})?$""",
            )

        const val MAX_AMOUNT_ERROR_MESSAGE =
            "Максимальная сумма — 999 999 999 999,99 ₽."

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