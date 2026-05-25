package cr.ac.una.codeerror.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cr.ac.una.codeerror.data.Producto
import cr.ac.una.codeerror.viewmodel.ProductoViewModel

@Composable
fun ProductoListScreen(viewModel: ProductoViewModel) {

    val context = LocalContext.current
    val productos by viewModel.productos.collectAsState()
    val productosFiltrados by viewModel.productosFiltrados.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsState()

    var busqueda by remember { mutableStateOf("") }

    val categorias = listOf("Todos", "Electrónica", "Ropa", "Hogar", "Deportes", "Alimentos")

    //  BUG 8: ¿qué pasa calculando las estadísticas así?

    val estadisticas = calcularEstadisticas(productos)  //  sin remember ni derivedStateOf

    //  CORRECCIÓN sería:
    // val estadisticas by remember(productos) { derivedStateOf { calcularEstadisticas(productos) } }

    val listaActual = if (productosFiltrados.isEmpty() && categoriaSeleccionada == "Todos") {
        productos
    } else {
        productosFiltrados
    }

    //  BUG 9: ¿La búsqueda se hace en cada recomposición?

    val listaFinal = if (busqueda.isEmpty()) {
        listaActual
    } else {
        listaActual.filter {  //  llamado en cada recomposición
            it.nombre.contains(busqueda, ignoreCase = true) ||
            it.descripcion.contains(busqueda, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(
            text = "📦 Productos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Estadísticas (recalculadas en cada recomposición )
        EstadisticasCard(estadisticas)

        Spacer(modifier = Modifier.height(2.dp))

        // Campo de búsqueda
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text("Buscar producto") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categorias) { categoria ->
                FilterChip(
                    selected = categoria == categoriaSeleccionada,
                    onClick = { viewModel.filtrarPorCategoria(categoria, context) },
                    label = { Text(categoria, fontSize = 11.sp) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = listaFinal,

                        key = { it.id }
                    ) { producto ->


                        ProductoCard(
                            producto = producto,
                            //  BUG 11:
                            // ¿Qué crees que pasa?
                            onClick =  { /* navegar */ }
                            //  CORRECCIÓN: onClick = remember(producto.id) { { /* navegar */ } }
                        )
                    }
                }
            }
        }
    }
}

//  BUG 8 — ¿es bueno tener eso sin caché?
fun calcularEstadisticas(productos: List<Producto>): Map<String, Any> {
    // Simula un cálculo costoso
    Thread.sleep(0) // En producción real podría ser más costoso
    return mapOf(
        "total" to productos.size,
        "precioPromedio" to if (productos.isEmpty()) 0.0
                           else productos.sumOf { it.precio } / productos.size,
        "categorias" to productos.map { it.categoria }.distinct().size,
        "masCaros" to productos.sortedByDescending { it.precio }.take(3) // ❌ sortedBy en cada recomposición
    )
}

@Composable
fun EstadisticasCard(estadisticas: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${estadisticas["total"]}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Total", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val promedio = estadisticas["precioPromedio"] as? Double ?: 0.0
                Text("₡${"%.0f".format(promedio)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Promedio", fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${estadisticas["categorias"]}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Categorías", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProductoCard(producto: Producto, onClick: () -> Unit) {

    SideEffect {
        Log.d("Recomposicion", "ProductoCard recompuesto: ${producto.nombre}")
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "₡${"%.0f".format(producto.precio)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = producto.descripcion,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            //  BUG 12: ¿qué pasa con el color?
            Box(
                modifier = Modifier
                    .background(
                        color = when (producto.categoria) {
                            "Electrónica" -> Color(0xFFE8F5E9)
                            "Ropa"        -> Color(0xFFFCE4EC)
                            "Hogar"       -> Color(0xFFFFF8E1)
                            "Deportes"    -> Color(0xFFE3F2FD)
                            else          -> Color(0xFFF3E5F5)
                        }
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(text = producto.categoria, fontSize = 11.sp)
            }
        }
    }
}
