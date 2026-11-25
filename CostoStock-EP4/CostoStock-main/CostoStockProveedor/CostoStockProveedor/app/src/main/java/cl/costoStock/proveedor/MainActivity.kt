package cl.costoStock.proveedor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.costoStock.proveedor.ui.ProveedorRoot
import cl.costoStock.proveedor.ui.theme.CostoStockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CostoStockTheme {
                ProveedorRoot()
            }
        }
    }
}
