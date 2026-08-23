package ru.lilnaro.finflow.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.lilnaro.finflow.data.ai.FinancialAiModelDownloader
import ru.lilnaro.finflow.data.ai.FinancialAiPromptBuilder
import ru.lilnaro.finflow.data.ai.GemmaFinancialAiRepository
import ru.lilnaro.finflow.data.local.database.FinFlowDatabase
import ru.lilnaro.finflow.data.local.initializer.DefaultTransactionCategoriesInitializer
import ru.lilnaro.finflow.data.repository.FinanceRepositoryImpl
import ru.lilnaro.finflow.domain.repository.FinanceRepository
import ru.lilnaro.finflow.domain.repository.FinancialAiRepository
import ru.lilnaro.finflow.domain.usecase.AddCategoryUseCase
import ru.lilnaro.finflow.domain.usecase.AddTransactionUseCase
import ru.lilnaro.finflow.domain.usecase.AnalyzeFinancialContextUseCase
import ru.lilnaro.finflow.domain.usecase.AskFinancialAssistantUseCase
import ru.lilnaro.finflow.domain.usecase.CalculateMonthSummaryUseCase
import ru.lilnaro.finflow.domain.usecase.CancelFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.domain.usecase.BuildFinancialAnalysisContextUseCase
import ru.lilnaro.finflow.domain.usecase.CloseFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.CreateFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.DeleteTransactionsUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveFinancialMonthUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveActiveMonthSummaryUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveArchivedFinancialMonthsUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveCategoriesByTypeUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAnalysisContextUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialAnalyticsUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveFinancialMonthReportUseCase
import ru.lilnaro.finflow.domain.usecase.ObserveTransactionsByMonthUseCase
import ru.lilnaro.finflow.domain.usecase.GetFinancialAiModelDownloadInfoUseCase
import ru.lilnaro.finflow.domain.usecase.InitializeFinancialAiUseCase
import ru.lilnaro.finflow.domain.usecase.StartFinancialAiModelDownloadUseCase
import ru.lilnaro.finflow.presentation.archive.ArchiveMonthDetailsViewModel
import ru.lilnaro.finflow.presentation.archive.ArchiveViewModel
import ru.lilnaro.finflow.presentation.assistant.AssistantViewModel
import ru.lilnaro.finflow.presentation.home.HomeViewModel
import ru.lilnaro.finflow.presentation.newmonth.NewMonthViewModel
import ru.lilnaro.finflow.presentation.transactions.TransactionsViewModel
import ru.lilnaro.finflow.presentation.transactions.addtransaction.AddTransactionViewModel

val appModule = module {

    single {
        FinFlowDatabase.getInstance(
            context = androidContext(),
        )
    }

    single {
        get<FinFlowDatabase>()
            .financialMonthDao()
    }

    single {
        get<FinFlowDatabase>()
            .transactionDao()
    }

    single {
        get<FinFlowDatabase>()
            .transactionCategoryDao()
    }

    single {
        DefaultTransactionCategoriesInitializer(
            transactionCategoryDao = get(),
        )
    }

    single<FinanceRepository> {
        FinanceRepositoryImpl(
            financialMonthDao = get(),
            transactionDao = get(),
            transactionCategoryDao = get(),
        )
    }

    single {
        FinancialAiModelDownloader(
            context = androidContext(),
        )
    }

    single {
        FinancialAiPromptBuilder()
    }

    single<FinancialAiRepository> {
        GemmaFinancialAiRepository(
            context = androidContext(),
            promptBuilder = get(),
            modelDownloader = get(),
        )
    }

    factory {
        AddCategoryUseCase(
            financeRepository = get(),
        )
    }

    factory {
        AddTransactionUseCase(
            financeRepository = get(),
        )
    }

    factory {
        CalculateMonthSummaryUseCase()
    }

    factory {
        CloseFinancialMonthUseCase(
            financeRepository = get(),
        )
    }

    factory {
        CreateFinancialMonthUseCase(
            financeRepository = get(),
        )
    }

    factory {
        DeleteTransactionsUseCase(
            financeRepository = get(),
        )
    }

    factory {
        ObserveActiveFinancialMonthUseCase(
            financeRepository = get(),
        )
    }

    factory {
        ObserveArchivedFinancialMonthsUseCase(
            financeRepository = get(),
        )
    }

    factory {
        ObserveCategoriesByTypeUseCase(
            financeRepository = get(),
        )
    }

    factory {
        ObserveTransactionsByMonthUseCase(
            financeRepository = get(),
        )
    }

    factory {
        ObserveFinancialMonthReportUseCase(
            financeRepository = get(),
            observeTransactionsByMonthUseCase =
                get(),
            observeCategoriesByTypeUseCase =
                get(),
            calculateMonthSummaryUseCase =
                get(),
        )
    }

    factory {
        BuildFinancialAnalysisContextUseCase()
    }

    factory {
        ObserveFinancialAnalysisContextUseCase(
            financeRepository = get(),
            observeFinancialMonthReportUseCase =
                get(),
            buildFinancialAnalysisContextUseCase =
                get(),
        )
    }

    factory {
        AnalyzeFinancialContextUseCase()
    }

    factory {
        ObserveFinancialAnalyticsUseCase(
            observeFinancialAnalysisContextUseCase =
                get(),
            analyzeFinancialContextUseCase =
                get(),
        )
    }

    factory {
        ObserveActiveMonthSummaryUseCase(
            observeActiveFinancialMonthUseCase =
                get(),
            observeTransactionsByMonthUseCase =
                get(),
            calculateMonthSummaryUseCase =
                get(),
        )
    }

    factory {
        InitializeFinancialAiUseCase(
            financialAiRepository =
                get(),
        )
    }

    factory {
        GetFinancialAiModelDownloadInfoUseCase(
            financialAiRepository =
                get(),
        )
    }

    factory {
        ObserveFinancialAiModelDownloadUseCase(
            financialAiRepository =
                get(),
        )
    }

    factory {
        StartFinancialAiModelDownloadUseCase(
            financialAiRepository =
                get(),
        )
    }

    factory {
        CancelFinancialAiModelDownloadUseCase(
            financialAiRepository =
                get(),
        )
    }

    factory {
        AskFinancialAssistantUseCase(
            financialAiRepository =
                get(),
        )
    }

    viewModel {
        AssistantViewModel(
            observeFinancialAnalysisContextUseCase =
                get(),
            observeFinancialAnalyticsUseCase =
                get(),
            initializeFinancialAiUseCase =
                get(),
            askFinancialAssistantUseCase =
                get(),
            getFinancialAiModelDownloadInfoUseCase =
                get(),
            observeFinancialAiModelDownloadUseCase =
                get(),
            startFinancialAiModelDownloadUseCase =
                get(),
            cancelFinancialAiModelDownloadUseCase =
                get(),
        )
    }

    viewModel {
        HomeViewModel(
            observeActiveFinancialMonthUseCase =
                get(),
            observeActiveMonthSummaryUseCase =
                get(),
            closeFinancialMonthUseCase =
                get(),
        )
    }

    viewModel {
        TransactionsViewModel(
            observeActiveFinancialMonthUseCase =
                get(),
            observeTransactionsByMonthUseCase =
                get(),
            observeCategoriesByTypeUseCase =
                get(),
            calculateMonthSummaryUseCase =
                get(),
            deleteTransactionsUseCase =
                get(),
        )
    }

    viewModel {
        AddTransactionViewModel(
            observeActiveFinancialMonthUseCase =
                get(),
            observeCategoriesByTypeUseCase =
                get(),
            addTransactionUseCase =
                get(),
            addCategoryUseCase =
                get(),
        )
    }

    viewModel {
        NewMonthViewModel(
            createFinancialMonthUseCase =
                get(),
            observeArchivedFinancialMonthsUseCase =
                get(),
        )
    }

    viewModel {
        ArchiveViewModel(
            observeArchivedFinancialMonthsUseCase =
                get(),
        )
    }

    viewModel {
        ArchiveMonthDetailsViewModel(
            observeFinancialMonthReportUseCase =
                get(),
        )
    }
}