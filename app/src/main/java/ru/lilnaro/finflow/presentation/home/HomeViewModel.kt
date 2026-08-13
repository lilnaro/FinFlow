package ru.lilnaro.finflow.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import ru.lilnaro.finflow.presentation.home.model.HomeAction
import ru.lilnaro.finflow.presentation.home.model.HomeEffect
import ru.lilnaro.finflow.presentation.home.model.HomeUiState

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(),
    )

    val uiState: StateFlow<HomeUiState> =
        _uiState.asStateFlow()

    private val _effect = Channel<HomeEffect>(
        capacity = Channel.BUFFERED,
    )

    val effect: Flow<HomeEffect> =
        _effect.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.TransactionsClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToTransactions,
                )
            }

            HomeAction.AssistantClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToAssistant,
                )
            }

            HomeAction.NewMonthClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToNewMonth,
                )
            }

            HomeAction.ArchiveClicked -> {
                _effect.trySend(
                    HomeEffect.NavigateToArchive,
                )
            }
        }
    }
}