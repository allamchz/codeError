package cr.ac.una.codeerror.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    //  Consulta correcta — usa índice, retorna Flow para reactividad
    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAll(): Flow<List<Producto>>

    //  BUG 1:
    // ¿qué hay de malo en la consulta?
    @Query("SELECT * FROM productos")
    suspend fun getAllParaFiltrar(): List<Producto>

    @Insert
    suspend fun insert(producto: Producto)

    @Insert
    suspend fun insertAll(productos: List<Producto>)

    @Query("DELETE FROM productos")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int
}
