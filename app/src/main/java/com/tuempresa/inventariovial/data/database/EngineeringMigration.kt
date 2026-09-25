package com.tuempresa.inventariovial.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object EngineeringMigration {
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sic19_details ADD COLUMN structuralCriterion TEXT")
            db.execSQL("ALTER TABLE sic20_details ADD COLUMN wallLengthMeters REAL")
            db.execSQL("ALTER TABLE photos ADD COLUMN description TEXT")
            db.execSQL("CREATE TABLE IF NOT EXISTS scap_joints (id TEXT NOT NULL, inspectionId TEXT NOT NULL, jointIndex INTEGER NOT NULL, type TEXT NOT NULL, material TEXT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(inspectionId) REFERENCES scap_inspections(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scap_joints_inspectionId_jointIndex ON scap_joints(inspectionId,jointIndex)")
            // Preserve both original field values and the first editable joint. No guessed codes/defaults.
            db.execSQL("""INSERT INTO scap_joints SELECT i.id || '-joint-1', i.id, 1,
                COALESCE((SELECT value FROM scap_values WHERE inspectionId=i.id AND ownerId='inspection' AND key='jointType'),''),
                COALESCE((SELECT value FROM scap_values WHERE inspectionId=i.id AND ownerId='inspection' AND key='jointMaterial'),'')
                FROM scap_inspections i WHERE EXISTS (SELECT 1 FROM scap_values WHERE inspectionId=i.id AND ownerId='inspection' AND key IN ('jointType','jointMaterial') AND value!='')""")
            db.execSQL("""INSERT INTO scap_values SELECT inspectionId, inspectionId || '-joint-1', key, value, source
                FROM scap_values WHERE ownerId='inspection' AND key IN ('jointType','jointMaterial') AND inspectionId IN (SELECT inspectionId FROM scap_joints)""")
        }
    }
}
