package cr.ac.una.codeerror.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import cr.ac.una.codeerror.data.AppDatabase
import cr.ac.una.codeerror.data.Producto
import cr.ac.una.codeerror.util.AppLogger
import cr.ac.una.codeerror.util.ProductoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductoViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.productoDao()

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos: StateFlow<List<Producto>> = _productos

    private val _productosFiltrados = MutableStateFlow<List<Producto>>(emptyList())
    val productosFiltrados: StateFlow<List<Producto>> = _productosFiltrados

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _categoriaSeleccionada = MutableStateFlow("Todos")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada

    init {
        cargarProductos()
        observarProductos()
    }

    //  Observa cambios reactivamente con Flow
    private fun observarProductos() {
        viewModelScope.launch {
            dao.getAll().collect { lista ->
                _productos.value = lista
            }
        }
    }

    //  BUG 4: ¿ que pasa con esta carga?
    fun cargarProductos() {
        _isLoading.value = true
        AppLogger.logDirecto("Iniciando carga de productos") //  BUG 3 ¿que pasa con este log?

        //  Esta operación de BD debería estar en Dispatchers.IO
        viewModelScope.launch(Dispatchers.Main) {  //  Main thread para IO
            try {
                val count = dao.count()  //  Viola StrictMode — IO en Main
                if (count == 0) {
                    sembrarDatos()
                }
            } catch (e: Exception) {
                AppLogger.logError("Error cargando productos", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    //  BUG 5: ¿que pasa con ese filtro?
    fun filtrarPorCategoria(categoria: String, activityContext: Context) {
        if (categoria == _categoriaSeleccionada.value) return
        _categoriaSeleccionada.value = categoria
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                //  Trae TODOS los productos aunque solo necesite una categoría
                val todos = dao.getAllParaFiltrar()

                //  BUG 2: ¿quie pasa con este llamado?
                ProductoCache.guardarBusqueda(
                    activityContext,   //  Fuga de memoria
                    categoria,
                    todos.map { it.nombre }
                )

                //  ¿está bien este filtrado?
                val filtrados = if (categoria == "Todos") {
                    todos
                } else {
                    todos.filter { it.categoria == categoria }
                }

                //  BUG 6: ¿está bien  ordenar así?
                val ordenados = filtrados.sortedWith(
                    compareBy({ it.categoria }, { it.nombre }, { it.precio })
                )

                withContext(Dispatchers.Main) {
                    _productosFiltrados.value = ordenados
                    _isLoading.value = false
                }

                AppLogger.logDirecto("Filtrados: ${ordenados.size} de ${todos.size} total") // ❌ BUG 3

            } catch (e: Exception) {
                AppLogger.logError("Error filtrando", e)
                withContext(Dispatchers.Main) { _isLoading.value = false }
            }
        }
    }

    //  BUG 7: ¿está bien insertar así?

    private suspend fun sembrarDatos() {
        val categorias = listOf("Electrónica", "Ropa", "Hogar", "Deportes", "Alimentos")
        val productos = mutableListOf<Producto>()

        repeat(500) { i ->
            val categoria = categorias[i % categorias.size]
            productos.add(
                Producto(
                    nombre = "Producto ${i + 1}",
                    precio = (100..50000).random().toDouble(),
                    categoria = categoria,
                    descripcion = "Descripción del producto ${i + 1} en la categoría $categoria"
                )
            )
        }

        withContext(Dispatchers.IO) {
            //  BUG 7: ¿que está mal?
            productos.forEach { dao.insert(it) }

            //  CORRECCIÓN sería:
            // dao.insertAll(productos)  — una sola transacción
        }
    }

    //  Versión corregida de cargarProductos (para mostrar en clase)
    fun cargarProductosCorregido() {
        _isLoading.value = true
        AppLogger.logCorrecto("Iniciando carga de productos") //  Timber

        viewModelScope.launch(Dispatchers.IO) { // IO thread para base de datos
            try {
                val count = dao.count()
                if (count == 0) {
                    sembrarDatosCorregido()
                }
            } catch (e: Exception) {
                AppLogger.logError("Error cargando productos", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    //  Versión corregida de sembrar datos
    private suspend fun sembrarDatosCorregido() {
        val categorias = listOf("Electrónica", "Ropa", "Hogar", "Deportes", "Alimentos")
        val productos = (1..500).map { i ->
            Producto(
                nombre = "Producto $i",
                precio = (100..50000).random().toDouble(),
                categoria = categorias[i % categorias.size],
                descripcion = "Descripción del producto $i"
            )
        }
        dao.insertAll(productos) //  Una sola transacción
    }
}
