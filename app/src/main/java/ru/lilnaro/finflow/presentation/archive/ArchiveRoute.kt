package ru.lilnaro.finflow.presentation.archive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveEffect

@Composable
fun ArchiveRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel =
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
                ArchiveEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }
            }
        }
    }

    ArchiveScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}