package ru.lilnaro.finflow.navigation

sealed interface FinFlowDestination {

    val route: String

    data object Home : FinFlowDestination {
        override val route: String = "home"
    }

    data object Transactions : FinFlowDestination {
        override val route: String = "transactions"
    }

    data object AddTransaction : FinFlowDestination {
        override val route: String = "transactions/add"
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

    data object ArchiveMonthDetails : FinFlowDestination {

        const val ARG_MONTH_ID = "monthId"

        private const val BASE_ROUTE =
            "archive/month"

        override val route: String =
            "$BASE_ROUTE/{$ARG_MONTH_ID}"

        fun createRoute(
            monthId: Long,
        ): String {
            return "$BASE_ROUTE/$monthId"
        }
    }
}