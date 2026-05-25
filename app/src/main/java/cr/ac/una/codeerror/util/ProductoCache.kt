package cr.ac.una.codeerror.util

import android.content.Context


object ProductoCache {

    // BUG 2 ¿qué hay de malo aquí?
    var context: Context? = null

    var ultimaBusqueda: String = ""
    var resultadosCache: List<String> = emptyList()

    fun guardarBusqueda(ctx: Context, busqueda: String, resultados: List<String>) {
        this.context = ctx          //  Aquí ocurre la fuga
        this.ultimaBusqueda = busqueda
        this.resultadosCache = resultados
    }

    fun limpiar() {
        // BUG: nunca se llama limpiar() en onDestroy de la Activity
        context = null
        ultimaBusqueda = ""
        resultadosCache = emptyList()
    }
}
