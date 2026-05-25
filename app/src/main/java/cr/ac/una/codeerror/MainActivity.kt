package cr.ac.una.codeerror



import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import cr.ac.una.codeerror.ui.screen.ProductoListScreen
import cr.ac.una.codeerror.viewmodel.ProductoViewModel

class MainActivity : ComponentActivity() {

    //  BUG 13: ViewModel creado manualmente con contexto de Activity
    // Si se rota la pantalla, se crea un nuevo ViewModel con el contexto viejo
    //  CORRECCIÓN: usar viewModels() delegate de androidx
    private lateinit var viewModel: ProductoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  Pasa 'this' (Activity context) al ViewModel
        viewModel = ProductoViewModel(this)

        setContent {
            MaterialTheme {
                Surface {
                    ProductoListScreen(viewModel = viewModel)
                }
            }
        }
    }

    //  BUG 14: onDestroy nunca llama ProductoCache.limpiar()
    // La referencia al contexto en el singleton nunca se libera
    // CORRECCIÓN: descomentar el bloque de abajo
    /*
    override fun onDestroy() {
        super.onDestroy()
        ProductoCache.limpiar()  //  Libera la referencia al contexto
    }
    */
}
