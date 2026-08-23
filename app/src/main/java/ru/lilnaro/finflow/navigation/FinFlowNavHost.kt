package ru.lilnaro.finflow.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.lilnaro.finflow.presentation.archive.ArchiveMonthDetailsRoute
import ru.lilnaro.finflow.presentation.archive.ArchiveRoute
import ru.lilnaro.finflow.presentation.assistant.AssistantRoute
import ru.lilnaro.finflow.presentation.home.HomeRoute
import ru.lilnaro.finflow.presentation.newmonth.NewMonthRoute
import ru.lilnaro.finflow.presentation.transactions.TransactionsRoute
import ru.lilnaro.finflow.presentation.transactions.addtransaction.AddTransactionRoute

@Composable
fun FinFlowNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController =
        rememberNavController(),
) {
    var transactionsResultMessage by
    rememberSaveable {
        mutableStateOf<String?>(null)
    }

    NavHost(
        navController = navController,
        startDestination =
            FinFlowDestination.Home.route,
        modifier = modifier,
    ) {
        composable(
            route =
                FinFlowDestination.Home.route,
        ) {
            HomeRoute(
                onNavigateToTransactions = {
                    navController.navigate(
                        FinFlowDestination
                            .Transactions.route,
                    )
                },
                onNavigateToAssistant = {
                    navController.navigate(
                        FinFlowDestination
                            .Assistant.route,
                    )
                },
                onNavigateToNewMonth = {
                    navController.navigate(
                        FinFlowDestination
                            .NewMonth.route,
                    ) {
                        launchSingleTop = true
                    }
                },
                onNavigateToArchive = {
                    navController.navigate(
                        FinFlowDestination
                            .Archive.route,
                    )
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .Transactions.route,
        ) {
            TransactionsRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddTransaction = {
                    navController.navigate(
                        FinFlowDestination
                            .AddTransaction.route,
                    ) {
                        launchSingleTop = true
                    }
                },
                successMessage =
                    transactionsResultMessage,
                onSuccessMessageShown = {
                    transactionsResultMessage =
                        null
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .AddTransaction.route,
        ) {
            AddTransactionRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionSaved = { message ->
                    transactionsResultMessage =
                        message

                    navController.popBackStack()
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .Assistant.route,
        ) {
            AssistantRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .NewMonth.route,
        ) {
            NewMonthRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMonthCreated = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .Archive.route,
        ) {
            ArchiveRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMonthDetails = { monthId ->
                    navController.navigate(
                        FinFlowDestination
                            .ArchiveMonthDetails
                            .createRoute(
                                monthId = monthId,
                            ),
                    )
                },
            )
        }

        composable(
            route =
                FinFlowDestination
                    .ArchiveMonthDetails.route,
            arguments = listOf(
                navArgument(
                    name =
                        FinFlowDestination
                            .ArchiveMonthDetails
                            .ARG_MONTH_ID,
                ) {
                    type = NavType.LongType
                },
            ),
        ) { backStackEntry ->
            val monthId =
                backStackEntry.arguments
                    ?.getLong(
                        FinFlowDestination
                            .ArchiveMonthDetails
                            .ARG_MONTH_ID,
                    )
                    ?: return@composable

            ArchiveMonthDetailsRoute(
                monthId = monthId,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}