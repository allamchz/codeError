package cr.ac.una.codeerror.util

import android.content.Context

/**
 *  BUG 2: FUGA DE MEMORIA
 *
 * Este singleton guarda una referencia directa a un Context (que puede ser una Activity).
 * Cuando la Activity se destruye (rotación, navegación), el GC no puede liberarla
 * porque este objeto estático la sigue reteniendo.
 *
 * LeakCanary detectará:
 *   ProductoCache → context → MainActivity (LEAKED)
 *
 *  CORRECCIÓN:
 *   - Usar applicationContext en lugar de activity context
 *   - O eliminar la referencia al contexto si no es necesaria
 *   - O usar WeakReference<Context>
 */
object ProductoCache {

    // Retiene el contexto — posible fuga de memoria
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
