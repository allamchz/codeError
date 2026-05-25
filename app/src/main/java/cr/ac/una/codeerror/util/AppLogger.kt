package cr.ac.una.codeerror.util

import android.util.Log
import timber.log.Timber

/**
 * Utilidad de logging que muestra la diferencia entre Log directo y Timber.
 *
 *  PROBLEMA con Log directo:
 *   - Los logs quedan activos en producción (expone datos sensibles)
 *   - No hay control por entorno (debug vs release)
 *   - No se integra con herramientas externas como Crashlytics
 *
 *  SOLUCIÓN con Timber:
 *   - Se configura una sola vez en Application
 *   - En debug: imprime en Logcat
 *   - En release: puede enviarse a Crashlytics automáticamente
 */
object AppLogger {

    private const val TAG = "RendimientoApp"

    //  BUG 3: Logging directo — activo en producción, sin control de entorno
    fun logDirecto(mensaje: String) {
        Log.d(TAG, mensaje)                    //  Siempre imprime
        Log.d(TAG, "Datos: $mensaje")          //  Podría exponer datos sensibles
    }

    // CORRECCIÓN: Timber respeta la configuración del entorno
    fun logCorrecto(mensaje: String) {
        Timber.d("Datos: %s", mensaje)         //  Solo en debug
    }

    fun logError(mensaje: String, error: Throwable? = null) {
        if (error != null) {
            Timber.e(error, mensaje)           // ✅ En release va a Crashlytics
        } else {
            Timber.e(mensaje)
        }
    }
}
