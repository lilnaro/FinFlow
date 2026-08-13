package ru.lilnaro.finflow.presentation.home.model

sealed interface HomeEffect {

    data object NavigateToTransactions : HomeEffect

    data object NavigateToAssistant : HomeEffect

    data object NavigateToNewMonth : HomeEffect

    data object NavigateToArchive : HomeEffect
}