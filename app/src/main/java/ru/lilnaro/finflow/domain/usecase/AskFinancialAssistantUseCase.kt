package ru.lilnaro.finflow.domain.usecase

import ru.lilnaro.finflow.domain.model.FinancialAiAnswerResult
import ru.lilnaro.finflow.domain.model.FinancialAiMessage
import ru.lilnaro.finflow.domain.model.FinancialAnalysisContext
import ru.lilnaro.finflow.domain.model.FinancialAnalyticsSnapshot
import ru.lilnaro.finflow.domain.repository.FinancialAiRepository

class AskFinancialAssistantUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    suspend operator fun invoke(
        question: String,
        analysisContext: FinancialAnalysisContext,
        analyticsSnapshot: FinancialAnalyticsSnapshot,
        conversationHistory:
        List<FinancialAiMessage>,
    ): FinancialAiAnswerResult {
        val normalizedQuestion =
            question.trim()

        if (normalizedQuestion.isEmpty()) {
            return FinancialAiAnswerResult.Failure(
                message =
                    "Введите вопрос для FinFlow AI.",
            )
        }

        if (
            normalizedQuestion.length >
            MAX_QUESTION_LENGTH
        ) {
            return FinancialAiAnswerResult.Failure(
                message =
                    "Вопрос слишком длинный. Максимум $MAX_QUESTION_LENGTH символов.",
            )
        }

        return financialAiRepository.ask(
            question = normalizedQuestion,
            analysisContext = analysisContext,
            analyticsSnapshot = analyticsSnapshot,
            conversationHistory =
                conversationHistory,
        )
    }

    private companion object {

        const val MAX_QUESTION_LENGTH =
            500
    }
}