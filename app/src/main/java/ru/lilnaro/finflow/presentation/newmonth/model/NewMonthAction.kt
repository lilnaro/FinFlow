package ru.lilnaro.finflow.presentation.newmonth.model

sealed interface NewMonthAction {

    data object BackClicked :
        NewMonthAction

    data class InitialBudgetChanged(
        val value: String,
    ) : NewMonthAction

    data object CreateMonthClicked :
        NewMonthAction
}