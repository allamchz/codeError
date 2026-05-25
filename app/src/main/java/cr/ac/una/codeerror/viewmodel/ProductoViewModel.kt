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

    //  BUG 4: Carga inicial en hilo principal (bloqueante)
    // StrictMode detectará: "StrictMode policy violation: android.os.strictmode.DiskReadViolation"
    fun cargarProductos() {
        _isLoading.value = true
        AppLogger.logDirecto("Iniciando carga de productos") // ❌ BUG 3 también aquí

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

    //  BUG 5: Filtrado ineficiente — trae todos los registros y filtra en memoria
    // Con 10,000 productos esto es muy lento
    //  CORRECCIÓN: usar la consulta SQL con WHERE categoria = :categoria
    fun filtrarPorCategoria(categoria: String, activityContext: Context) {
        if (categoria == _categoriaSeleccionada.value) return
        _categoriaSeleccionada.value = categoria
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                //  Trae TODOS los productos aunque solo necesite una categoría
                val todos = dao.getAllParaFiltrar()

                //  BUG 2: Guarda el contexto de Activity en el singleton
                ProductoCache.guardarBusqueda(
                    activityContext,   //  Fuga de memoria
                    categoria,
                    todos.map { it.nombre }
                )

                //  Filtrado en memoria (debería hacerse en SQL)
                val filtrados = if (categoria == "Todos") {
                    todos
                } else {
                    todos.filter { it.categoria == categoria }  // ❌ O(n) en Kotlin
                }

                //  BUG 6: Operación costosa innecesaria en cada filtrado
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

    //  BUG 7: Inserta productos uno por uno en lugar de en lote
    // Con 500 productos: 500 transacciones vs 1 transacción
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
            //  BUG 7: Insert uno por uno — muy lento
            productos.forEach { dao.insert(it) }

            //  CORRECCIÓN sería:
            // dao.insertAll(productos)  — una sola transacción
        }
    }

    //  Versión corregida de cargarProductos (para mostrar en clase)
    fun cargarProductosCorregido() {
        _isLoading.value = true
        AppLogger.logCorrecto("Iniciando carga de productos") // ✅ Timber

        viewModelScope.launch(Dispatchers.IO) { // ✅ IO thread para base de datos
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
