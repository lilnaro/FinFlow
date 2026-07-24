package ru.lilnaro.finflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import ru.lilnaro.finflow.presentation.home.HomeAction
import ru.lilnaro.finflow.presentation.home.HomeScreen
import ru.lilnaro.finflow.presentation.home.HomeUiState
import ru.lilnaro.finflow.ui.theme.FinFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            FinFlowTheme {
                FinFlowApp()
            }
        }
    }
}

@Composable
private fun FinFlowApp() {
    val homeUiState = HomeUiState()

    HomeScreen(
        uiState = homeUiState,
        onAction = { action ->
            when (action) {
                HomeAction.TransactionsClicked -> {

                }

                HomeAction.AssistantClicked -> {

                }

                HomeAction.NewMonthClicked -> {

                }

                HomeAction.ArchiveClicked -> {

                }
            }
        },
    )
}