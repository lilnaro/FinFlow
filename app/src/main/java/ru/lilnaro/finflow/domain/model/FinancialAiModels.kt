package ru.lilnaro.finflow.domain.model

enum class FinancialAiMessageRole {
    USER,
    ASSISTANT,
}

data class FinancialAiMessage(
    val role: FinancialAiMessageRole,
    val text: String,
)

sealed interface FinancialAiInitializationResult {

    data object Ready :
        FinancialAiInitializationResult

    data class ModelMissing(
        val expectedFileName: String,
    ) : FinancialAiInitializationResult

    data class Failure(
        val message: String,
    ) : FinancialAiInitializationResult
}

sealed interface FinancialAiAnswerResult {

    data class Success(
        val answer: String,
    ) : FinancialAiAnswerResult

    data class Failure(
        val message: String,
    ) : FinancialAiAnswerResult
}