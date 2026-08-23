package ru.lilnaro.finflow.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.lilnaro.finflow.presentation.archive.ArchiveMonthDetailsRoute
import ru.lilnaro.finflow.presentation.archive.ArchiveRoute
import ru.lilnaro.finflow.presentation.assistant.AssistantRoute
import ru.lilnaro.finflow.presentation.home.HomeRoute
import ru.lilnaro.finflow.presentation.newmonth.NewMonthRoute
import ru.lilnaro.finflow.presentation.transactions.TransactionsRoute
import ru.lilnaro.finflow.presentation.transactions.addtransaction.AddTransactionRoute
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowBackground

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

    val currentBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute =
        currentBackStackEntry
            ?.destination
            ?.route

    val showBottomBar =
        currentRoute in MAIN_TAB_ROUTES

    Scaffold(
        modifier = modifier,
        containerColor = FinFlowBackground,
        contentWindowInsets = WindowInsets(
            0,
            0,
            0,
            0,
        ),
        bottomBar = {
            if (showBottomBar) {
                FinFlowBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { destination ->
                        navigateToMainTab(
                            navController = navController,
                            destination = destination,
                        )
                    },
                    onAddTransaction = {
                        navController.navigate(
                            FinFlowDestination
                                .AddTransaction.route,
                        ) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination =
                FinFlowDestination.Home.route,
            modifier =
                Modifier.padding(
                    contentPadding,
                ),
            enterTransition = {
                createEnterTransition(
                    initialRoute =
                        initialState.destination.route,
                    targetRoute =
                        targetState.destination.route,
                )
            },
            exitTransition = {
                createExitTransition(
                    initialRoute =
                        initialState.destination.route,
                    targetRoute =
                        targetState.destination.route,
                )
            },
            popEnterTransition = {
                createEnterTransition(
                    initialRoute =
                        initialState.destination.route,
                    targetRoute =
                        targetState.destination.route,
                )
            },
            popExitTransition = {
                createExitTransition(
                    initialRoute =
                        initialState.destination.route,
                    targetRoute =
                        targetState.destination.route,
                )
            },
        ) {
            composable(
                route =
                    FinFlowDestination.Home.route,
            ) {
                HomeRoute(
                    onNavigateToTransactions = {
                        navigateToMainTab(
                            navController = navController,
                            destination =
                                FinFlowDestination.Transactions,
                        )
                    },
                    onNavigateToAssistant = {
                        navigateToMainTab(
                            navController = navController,
                            destination =
                                FinFlowDestination.Assistant,
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
                        navigateToMainTab(
                            navController = navController,
                            destination =
                                FinFlowDestination.Archive,
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

                        navigateToMainTab(
                            navController = navController,
                            destination =
                                FinFlowDestination.Transactions,
                        )
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
                    onNavigateToNewMonth = {
                        navController.navigate(
                            FinFlowDestination
                                .NewMonth.route,
                        ) {
                            launchSingleTop = true
                        }
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
}

private fun navigateToMainTab(
    navController: NavHostController,
    destination: FinFlowDestination,
) {
    val currentRoute =
        navController.currentDestination?.route

    if (currentRoute == destination.route) {
        return
    }

    navController.navigate(
        destination.route,
    ) {
        popUpTo(
            navController.graph
                .findStartDestination()
                .id,
        ) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}

private fun createEnterTransition(
    initialRoute: String?,
    targetRoute: String?,
): EnterTransition {
    if (
        targetRoute ==
        FinFlowDestination.AddTransaction.route
    ) {
        return scaleIn(
            initialScale = 0.82f,
            transformOrigin =
                TransformOrigin(
                    pivotFractionX = 0.5f,
                    pivotFractionY = 1f,
                ),
            animationSpec =
                tween(
                    durationMillis = 420,
                    easing =
                        FastOutSlowInEasing,
                ),
        ) + slideInVertically(
            animationSpec =
                tween(
                    durationMillis = 420,
                    easing =
                        FastOutSlowInEasing,
                ),
            initialOffsetY = { fullHeight ->
                fullHeight / 5
            },
        ) + fadeIn(
            animationSpec =
                tween(
                    durationMillis = 280,
                ),
        )
    }

    if (
        initialRoute ==
        FinFlowDestination.AddTransaction.route
    ) {
        return scaleIn(
            initialScale = 0.96f,
            animationSpec =
                tween(
                    durationMillis = 340,
                    easing =
                        FastOutSlowInEasing,
                ),
        ) + fadeIn(
            animationSpec =
                tween(
                    durationMillis = 260,
                ),
        )
    }

    val initialIndex =
        mainTabIndex(
            route = initialRoute,
        )

    val targetIndex =
        mainTabIndex(
            route = targetRoute,
        )

    if (
        initialIndex == null ||
        targetIndex == null ||
        initialIndex == targetIndex
    ) {
        return fadeIn(
            animationSpec =
                tween(
                    durationMillis = 260,
                    easing =
                        FastOutSlowInEasing,
                ),
        )
    }

    val enterFromRight =
        targetIndex > initialIndex

    return slideInHorizontally(
        animationSpec =
            tween(
                durationMillis = 420,
                easing =
                    FastOutSlowInEasing,
            ),
        initialOffsetX = { fullWidth ->
            if (enterFromRight) {
                fullWidth
            } else {
                -fullWidth
            }
        },
    ) + fadeIn(
        animationSpec =
            tween(
                durationMillis = 300,
            ),
    )
}

private fun createExitTransition(
    initialRoute: String?,
    targetRoute: String?,
): ExitTransition {
    if (
        initialRoute ==
        FinFlowDestination.AddTransaction.route
    ) {
        return scaleOut(
            targetScale = 0.88f,
            transformOrigin =
                TransformOrigin(
                    pivotFractionX = 0.5f,
                    pivotFractionY = 1f,
                ),
            animationSpec =
                tween(
                    durationMillis = 360,
                    easing =
                        FastOutSlowInEasing,
                ),
        ) + slideOutVertically(
            animationSpec =
                tween(
                    durationMillis = 360,
                    easing =
                        FastOutSlowInEasing,
                ),
            targetOffsetY = { fullHeight ->
                fullHeight / 6
            },
        ) + fadeOut(
            animationSpec =
                tween(
                    durationMillis = 240,
                ),
        )
    }

    if (
        targetRoute ==
        FinFlowDestination.AddTransaction.route
    ) {
        return scaleOut(
            targetScale = 0.97f,
            animationSpec =
                tween(
                    durationMillis = 320,
                    easing =
                        FastOutSlowInEasing,
                ),
        ) + fadeOut(
            animationSpec =
                tween(
                    durationMillis = 260,
                ),
        )
    }

    val initialIndex =
        mainTabIndex(
            route = initialRoute,
        )

    val targetIndex =
        mainTabIndex(
            route = targetRoute,
        )

    if (
        initialIndex == null ||
        targetIndex == null ||
        initialIndex == targetIndex
    ) {
        return fadeOut(
            animationSpec =
                tween(
                    durationMillis = 240,
                    easing =
                        FastOutSlowInEasing,
                ),
        )
    }

    val movingRight =
        targetIndex > initialIndex

    return slideOutHorizontally(
        animationSpec =
            tween(
                durationMillis = 420,
                easing =
                    FastOutSlowInEasing,
            ),
        targetOffsetX = { fullWidth ->
            if (movingRight) {
                -fullWidth
            } else {
                fullWidth
            }
        },
    ) + fadeOut(
        animationSpec =
            tween(
                durationMillis = 300,
            ),
    )
}

private fun mainTabIndex(
    route: String?,
): Int? {
    return when (route) {
        FinFlowDestination.Home.route -> 0
        FinFlowDestination.Transactions.route -> 1
        FinFlowDestination.Assistant.route -> 2
        FinFlowDestination.Archive.route -> 3
        else -> null
    }
}

private val MAIN_TAB_ROUTES = setOf(
    FinFlowDestination.Home.route,
    FinFlowDestination.Transactions.route,
    FinFlowDestination.Assistant.route,
    FinFlowDestination.Archive.route,
)