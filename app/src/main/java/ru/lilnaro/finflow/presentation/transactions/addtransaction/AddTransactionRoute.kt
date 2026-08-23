package ru.lilnaro.finflow.presentation.transactions.addtransaction

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionAction
import ru.lilnaro.finflow.presentation.transactions.addtransaction.model.AddTransactionEffect

@Composable
fun AddTransactionRoute(
    onNavigateBack: () -> Unit,
    onTransactionSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel =
        koinViewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val snackbarScope = rememberCoroutineScope()

    val currentOnNavigateBack by rememberUpdatedState(
        onNavigateBack,
    )

    val currentOnTransactionSaved by rememberUpdatedState(
        onTransactionSaved,
    )

    BackHandler {
        if (!uiState.isSaving) {
            viewModel.onAction(
                AddTransactionAction.BackClicked,
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AddTransactionEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }

                is AddTransactionEffect.TransactionSaved -> {
                    currentOnTransactionSaved(
                        effect.message,
                    )
                }

                is AddTransactionEffect.ShowMessage -> {
                    snackbarScope.launch {
                        snackbarHostState
                            .currentSnackbarData
                            ?.dismiss()

                        snackbarHostState.showSnackbar(
                            message = effect.message,
                        )
                    }
                }
            }
        }
    }

    AddTransactionScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}