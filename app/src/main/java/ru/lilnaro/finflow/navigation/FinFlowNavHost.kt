package ru.lilnaro.finflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.lilnaro.finflow.presentation.archive.ArchiveScreen
import ru.lilnaro.finflow.presentation.assistant.AssistantScreen
import ru.lilnaro.finflow.presentation.home.HomeAction
import ru.lilnaro.finflow.presentation.home.HomeScreen
import ru.lilnaro.finflow.presentation.home.HomeUiState
import ru.lilnaro.finflow.presentation.newmonth.NewMonthScreen
import ru.lilnaro.finflow.presentation.transactions.TransactionsScreen


@Composable
fun FinFlowNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = FinFlowDestination.Home.route,
        modifier = modifier,
    ) {
        composable(
            route = FinFlowDestination.Home.route,
        ) {
            HomeScreen(
                uiState = HomeUiState(),
                onAction = { action ->
                    when (action) {
                        HomeAction.TransactionsClicked -> {
                            navController.navigate(
                                FinFlowDestination.Transactions.route,
                            )
                        }

                        HomeAction.AssistantClicked -> {
                            navController.navigate(
                                FinFlowDestination.Assistant.route,
                            )
                        }

                        HomeAction.NewMonthClicked -> {
                            navController.navigate(
                                FinFlowDestination.NewMonth.route,
                            )
                        }

                        HomeAction.ArchiveClicked -> {
                            navController.navigate(
                                FinFlowDestination.Archive.route,
                            )
                        }
                    }
                },
            )
        }

        composable(
            route = FinFlowDestination.Transactions.route,
        ) {
            TransactionsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = FinFlowDestination.Assistant.route,
        ) {
            AssistantScreen(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = FinFlowDestination.NewMonth.route,
        ) {
            NewMonthScreen(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = FinFlowDestination.Archive.route,
        ) {
            ArchiveScreen(
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}