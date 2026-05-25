package cr.ac.una.codeerror.util

import android.util.Log
import timber.log.Timber

object AppLogger {

    private const val TAG = "RendimientoApp"

    //  BUG 3: ¿qué hay de malo en este log?
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
