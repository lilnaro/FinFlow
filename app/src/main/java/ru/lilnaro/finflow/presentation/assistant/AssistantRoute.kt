package ru.lilnaro.finflow.presentation.assistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.assistant.model.AssistantEffect

@Composable
fun AssistantRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel =
        koinViewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnNavigateBack by
    rememberUpdatedState(
        onNavigateBack,
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AssistantEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }
            }
        }
    }

    AssistantScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        showBackButton = false,
        modifier = modifier,
    )
}