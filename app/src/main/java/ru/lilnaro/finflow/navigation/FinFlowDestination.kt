package ru.lilnaro.finflow.navigation

sealed interface FinFlowDestination {

    val route: String

    data object Home : FinFlowDestination {
        override val route: String = "home"
    }

    data object Transactions : FinFlowDestination {
        override val route: String = "transactions"
    }

    data object Assistant : FinFlowDestination {
        override val route: String = "assistant"
    }

    data object NewMonth : FinFlowDestination {
        override val route: String = "new_month"
    }

    data object Archive : FinFlowDestination {
        override val route: String = "archive"
    }
}