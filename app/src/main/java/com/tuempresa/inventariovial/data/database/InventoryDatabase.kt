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
import com.tuempresa.inventariovial.data.entity.Sic23Entity
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.scap.data.*


@Database(
    entities = [
        InventoryRecordEntity::class,
        PhotoEntity::class,
        Sic17Entity::class,
        Sic18Entity::class,
        Sic19Entity::class,
        Sic20Entity::class,
        Sic21Entity::class,
        Sic22Entity::class,
        Sic23Entity::class,
        Sic17AEntity::class, Sic17BEntity::class, Sic18AEntity::class,
        TrackPointEntity::class, FieldSession::class,
        ScapInspectionEntity::class, ScapFieldValueEntity::class, ScapSpanEntity::class,
        ScapSubstructureEntity::class, ScapSupportEntity::class, ScapElementEntity::class,
        ScapElementConditionEntity::class, ScapDefectEntity::class, ScapSketchEntity::class,
        ScapProfilePointEntity::class, ScapJointEntity::class
    ],

    version = 7,

    exportSchema = true
)
abstract class InventoryDatabase :
    RoomDatabase() {

    abstract fun scapDao(): ScapDao

    abstract fun inventoryDao():
            InventoryDao


    companion object {
        val MIGRATION_6_7 = EngineeringMigration.MIGRATION_6_7
        val MIGRATION_5_6 = ScapMigration.MIGRATION_5_6
        val MIGRATION_4_5 = InventoryMigrations.MIGRATION_4_5
        val MIGRATION_3_4 = InventoryMigrations.MIGRATION_3_4
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS sic23_details (
                    recordId TEXT NOT NULL, classCode TEXT NOT NULL, typeCode TEXT,
                    widthM REAL, description TEXT NOT NULL, PRIMARY KEY(recordId),
                    FOREIGN KEY(recordId) REFERENCES inventory_records(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent())
            }
        }

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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                            .build()

                    INSTANCE = instance

                    instance
                }
        }
    }
}
