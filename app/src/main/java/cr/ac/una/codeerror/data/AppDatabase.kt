package cr.ac.una.codeerror.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.Executors

@Database(entities = [Producto::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rendimiento_db"
                ).also { builder ->

                        builder.setQueryCallback(
                            { sql, args -> Log.d("RoomQuery", "SQL: $sql | Args: $args") },
                            Executors.newSingleThreadExecutor()
                        )

                }.build().also { INSTANCE = it }
            }
        }
    }
}
