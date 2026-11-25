package cl.costoStock.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.costoStock.myapplication.ui.CostoStockRoot
import cl.costoStock.myapplication.ui.theme.CostoStockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CostoStockTheme {
                CostoStockRoot()
            }
        }
    }
}
