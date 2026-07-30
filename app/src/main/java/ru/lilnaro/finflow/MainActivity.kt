package ru.lilnaro.finflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.lilnaro.finflow.navigation.FinFlowNavHost
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
    FinFlowNavHost(
        modifier = Modifier.fillMaxSize(),
    )
}