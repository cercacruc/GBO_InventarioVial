package com.tuempresa.inventariovial.data.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import com.tuempresa.inventariovial.data.dao.InventoryDao
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity


@Database(
    entities = [
        InventoryRecordEntity::class,
        PhotoEntity::class,
        Sic17Entity::class,
        Sic18Entity::class,
        Sic19Entity::class,
        Sic20Entity::class,
        Sic21Entity::class,
        Sic22Entity::class
    ],

    version = 1,

    exportSchema = false
)
abstract class InventoryDatabase :
    RoomDatabase() {

    abstract fun inventoryDao():
            InventoryDao


    companion object {

        @Volatile
        private var INSTANCE:
                InventoryDatabase? = null


        fun getInstance(
            context: Context
        ): InventoryDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            InventoryDatabase::class.java,
                            "inventario_vial.db"
                        )
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}