package ru.lilnaro.finflow.presentation.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsAction
import ru.lilnaro.finflow.presentation.transactions.model.TransactionsEffect

@Composable
fun TransactionsRoute(
    onNavigateBack: () -> Unit,
    successMessage: String? = null,
    onSuccessMessageShown: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = koinViewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val currentOnNavigateBack by rememberUpdatedState(
        onNavigateBack,
    )

    val currentOnSuccessMessageShown by rememberUpdatedState(
        onSuccessMessageShown,
    )

    BackHandler {
        viewModel.onAction(
            TransactionsAction.BackClicked,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                TransactionsEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }

                is TransactionsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                    )
                }
            }
        }
    }

    LaunchedEffect(successMessage) {
        val message =
            successMessage
                ?: return@LaunchedEffect

        snackbarHostState.showSnackbar(
            message = message,
        )

        currentOnSuccessMessageShown()
    }

    TransactionsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        showBackButton = false,
        modifier = modifier,
    )
}