package ru.lilnaro.finflow.presentation.newmonth

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
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthAction
import ru.lilnaro.finflow.presentation.newmonth.model.NewMonthEffect

@Composable
fun NewMonthRoute(
    onNavigateBack: () -> Unit,
    onMonthCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewMonthViewModel =
        koinViewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val currentOnNavigateBack by rememberUpdatedState(
        onNavigateBack,
    )

    val currentOnMonthCreated by rememberUpdatedState(
        onMonthCreated,
    )

    BackHandler {
        if (!uiState.isSaving) {
            viewModel.onAction(
                NewMonthAction.BackClicked,
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                NewMonthEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }

                is NewMonthEffect.MonthCreated -> {
                    currentOnMonthCreated()
                }

                is NewMonthEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                    )
                }
            }
        }
    }

    NewMonthScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}