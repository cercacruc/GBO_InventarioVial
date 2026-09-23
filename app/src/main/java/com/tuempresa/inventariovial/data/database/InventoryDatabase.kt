package com.tuempresa.inventariovial.data.database

import android.content.Context

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    version = 2,

    exportSchema = false
)
abstract class InventoryDatabase :
    RoomDatabase() {

    abstract fun inventoryDao():
            InventoryDao


    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endLatitude REAL")
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endLongitude REAL")
                db.execSQL("ALTER TABLE inventory_records ADD COLUMN endGpsAccuracyM REAL")
            }
        }


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
                            .addMigrations(MIGRATION_1_2)
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}