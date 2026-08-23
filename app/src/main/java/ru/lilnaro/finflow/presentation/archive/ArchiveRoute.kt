package ru.lilnaro.finflow.presentation.archive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import ru.lilnaro.finflow.presentation.archive.model.ArchiveEffect
import ru.lilnaro.finflow.presentation.archive.model.ArchiveMonthDetailsEffect

@Composable
fun ArchiveRoute(
    onNavigateBack: () -> Unit,
    onNavigateToNewMonth: () -> Unit,
    onNavigateToMonthDetails: (Long) -> Unit,
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

    val currentOnNavigateToNewMonth by
    rememberUpdatedState(
        onNavigateToNewMonth,
    )

    val currentOnNavigateToMonthDetails by
    rememberUpdatedState(
        onNavigateToMonthDetails,
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ArchiveEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }

                ArchiveEffect.NavigateToNewMonth -> {
                    currentOnNavigateToNewMonth()
                }

                is ArchiveEffect.NavigateToMonthDetails -> {
                    currentOnNavigateToMonthDetails(
                        effect.monthId,
                    )
                }
            }
        }
    }

    ArchiveScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        showBackButton = false,
        modifier = modifier,
    )
}

@Composable
fun ArchiveMonthDetailsRoute(
    monthId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveMonthDetailsViewModel =
        koinViewModel(),
) {
    val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()

    val currentOnNavigateBack by
    rememberUpdatedState(
        onNavigateBack,
    )

    LaunchedEffect(
        monthId,
        viewModel,
    ) {
        viewModel.loadMonth(
            monthId = monthId,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ArchiveMonthDetailsEffect.NavigateBack -> {
                    currentOnNavigateBack()
                }
            }
        }
    }

    ArchiveMonthDetailsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}