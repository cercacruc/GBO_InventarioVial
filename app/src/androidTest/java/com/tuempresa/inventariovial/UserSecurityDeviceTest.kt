package com.tuempresa.inventariovial

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.tuempresa.inventariovial.auth.KeystoreCredentialProtector
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class UserSecurityDeviceTest {
    @Test fun androidKeystoreRoundtripRejectsDifferentUserAndTampering() {
        val protector = KeystoreCredentialProtector()
        val blob = protector.encrypt("synthetic verifier", "instrumentation:user1")
        assertEquals("synthetic verifier", KeystoreCredentialProtector().decrypt(blob, "instrumentation:user1"))
        assertTrue(runCatching { protector.decrypt(blob, "instrumentation:user2") }.isFailure)
        val bytes = java.util.Base64.getDecoder().decode(blob)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertTrue(runCatching { protector.decrypt(java.util.Base64.getEncoder().encodeToString(bytes), "instrumentation:user1") }.isFailure)
    }

    @Test fun migration7to8PreservesAllLegacyTablesOnDevice() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val schema = instrumentation.context.assets.open("com.tuempresa.inventariovial.data.database.InventoryDatabase/7.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val entities = schema.getJSONArray("entities")
        val name = "user-migration-device.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                val fields = entity.getJSONArray("fields")
                val columns = (0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
                val values = (0 until fields.length()).map { when(fields.getJSONObject(it).getString("affinity")) { "INTEGER" -> "1"; "REAL" -> "1.5"; "BLOB" -> "X'00'"; else -> "'legacy'" } }
                old.execSQL("INSERT INTO `$table` (${columns.joinToString { "`$it`" }}) VALUES (${values.joinToString()})")
            }
            old.version = 7
        }
        val migrated = Room.databaseBuilder(context, InventoryDatabase::class.java, name).addMigrations(InventoryDatabase.MIGRATION_7_8,InventoryDatabase.MIGRATION_8_9,InventoryDatabase.MIGRATION_9_10).build()
        try {
            assertEquals(0, migrated.userDao().countUsers())
            for (i in 0 until entities.length()) {
                val table = entities.getJSONObject(i).getString("tableName")
                migrated.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use {
                    assertTrue(it.moveToFirst()); assertEquals(table, 1, it.getInt(0))
                }
            }
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
