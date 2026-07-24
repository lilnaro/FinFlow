package ru.lilnaro.finflow.presentation.home

sealed interface HomeAction {

    data object TransactionsClicked : HomeAction

    data object AssistantClicked : HomeAction

    data object NewMonthClicked : HomeAction

    data object ArchiveClicked : HomeAction
}