package ru.lilnaro.finflow.presentation.assistant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.lilnaro.finflow.presentation.ui.theme.FinFlowTheme

@Composable
fun AssistantScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AssistantColors.Background,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "ИИ-ассистент",
                color = AssistantColors.PrimaryText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Здесь можно будет задавать вопросы о личных финансах",
                color = AssistantColors.SecondaryText,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBackClick,
                border = BorderStroke(
                    width = 1.dp,
                    color = AssistantColors.Border,
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = AssistantColors.PrimaryText,
                ),
            ) {
                Text(
                    text = "Назад",
                    fontSize = 15.sp,
                )
            }
        }
    }
}


private object AssistantColors {

    val Background = Color(0xFF171817)

    val PrimaryText = Color(0xFFF7F7F7)

    val SecondaryText = Color(0xFFD5D7D3)

    val Border = Color(0xFF555753)
}

@Preview(
    name = "Экран ИИ-ассистента",
    showBackground = true,
    backgroundColor = 0xFF171817,
    widthDp = 360,
    heightDp = 760,
)
@Composable
private fun AssistantScreenPreview() {
    FinFlowTheme {
        AssistantScreen(
            onBackClick = {},
        )
    }
}