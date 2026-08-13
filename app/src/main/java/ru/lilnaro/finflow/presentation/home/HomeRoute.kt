package ru.lilnaro.finflow.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.home.model.HomeEffect

@Composable
fun HomeRoute(
    onNavigateToTransactions: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToNewMonth: () -> Unit,
    onNavigateToArchive: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnNavigateToTransactions by rememberUpdatedState(
        onNavigateToTransactions,
    )

    val currentOnNavigateToAssistant by rememberUpdatedState(
        onNavigateToAssistant,
    )

    val currentOnNavigateToNewMonth by rememberUpdatedState(
        onNavigateToNewMonth,
    )

    val currentOnNavigateToArchive by rememberUpdatedState(
        onNavigateToArchive,
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToTransactions -> {
                    currentOnNavigateToTransactions()
                }

                HomeEffect.NavigateToAssistant -> {
                    currentOnNavigateToAssistant()
                }

                HomeEffect.NavigateToNewMonth -> {
                    currentOnNavigateToNewMonth()
                }

                HomeEffect.NavigateToArchive -> {
                    currentOnNavigateToArchive()
                }
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}