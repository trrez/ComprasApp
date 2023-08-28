package com.example.comprasapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductoDao {

    @Query("SELECT * FROM producto ORDER BY realizada")
    fun findAll(): List<Producto>

    @Query("SELECT COUNT(*) FROM producto")
    fun contar(): Int

    @Insert
    fun insertar(producto: Producto):Long

    @Update
    fun actualizar(producto: Producto)

    @Delete
    fun eliminar(producto: Producto)
}