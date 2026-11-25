package cl.costoStock.myapplication.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Logo tipográfico: COSTO + STOCK y, opcionalmente, el eslogan de la marca. */
@Composable
fun Wordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    costoColor: Color = MaterialTheme.colorScheme.onBackground,
    stockColor: Color = MaterialTheme.colorScheme.primary,
    showSlogan: Boolean = false
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = costoColor)) { append("COSTO") }
                withStyle(SpanStyle(color = stockColor)) { append("STOCK") }
            },
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        if (showSlogan) {
            Text(
                text = "NADA SE PIERDE, TODOS GANAMOS",
                fontSize = (fontSize.value * 0.32f).sp,
                fontWeight = FontWeight.Bold,
                color = costoColor
            )
        }
    }
}
