package ru.lilnaro.finflow.presentation.newmonth.model

sealed interface NewMonthEffect {

    data object NavigateBack :
        NewMonthEffect

    data class MonthCreated(
        val message: String,
    ) : NewMonthEffect

    data class ShowMessage(
        val message: String,
    ) : NewMonthEffect
}