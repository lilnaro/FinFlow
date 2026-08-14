package ru.lilnaro.finflow.presentation.home.model

sealed interface HomeAction {

    data object TransactionsClicked : HomeAction

    data object AssistantClicked : HomeAction

    data object NewMonthClicked : HomeAction

    data object ArchiveClicked : HomeAction

    data object CloseMonthConfirmed : HomeAction

    data object CloseMonthCancelled : HomeAction

    data object RetryClicked : HomeAction
}