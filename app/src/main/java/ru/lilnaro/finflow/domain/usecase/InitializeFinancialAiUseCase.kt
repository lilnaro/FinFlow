package ru.lilnaro.finflow.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.lilnaro.finflow.domain.model.FinancialAiInitializationResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadInfo
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadStartResult
import ru.lilnaro.finflow.domain.repository.FinancialAiModelDownloadState
import ru.lilnaro.finflow.domain.repository.FinancialAiRepository

class InitializeFinancialAiUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    suspend operator fun invoke():
            FinancialAiInitializationResult {
        return financialAiRepository.initialize()
    }
}

class GetFinancialAiModelDownloadInfoUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    operator fun invoke():
            FinancialAiModelDownloadInfo {
        return financialAiRepository
            .getModelDownloadInfo()
    }
}

class ObserveFinancialAiModelDownloadUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    operator fun invoke():
            Flow<FinancialAiModelDownloadState> {
        return financialAiRepository
            .observeModelDownloadState()
    }
}

class StartFinancialAiModelDownloadUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    suspend operator fun invoke():
            FinancialAiModelDownloadStartResult {
        return financialAiRepository
            .startModelDownload()
    }
}

class CancelFinancialAiModelDownloadUseCase(
    private val financialAiRepository:
    FinancialAiRepository,
) {

    suspend operator fun invoke() {
        financialAiRepository
            .cancelModelDownload()
    }
}