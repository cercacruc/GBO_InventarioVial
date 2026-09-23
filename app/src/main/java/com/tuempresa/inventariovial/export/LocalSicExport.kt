package com.tuempresa.inventariovial.export

import android.content.Context
import android.net.Uri
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class PreparedSicExport(val file: File, val isZip: Boolean, val recordCount: Int, val workbookCount: Int)
data class LocalExportState(val busy: Boolean = false, val prepared: PreparedSicExport? = null, val message: String? = null)

class LocalSicExport(private val context: Context, private val database: InventoryDatabase, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(LocalExportState())
    val state = mutableState.asStateFlow()
    private val preferences = context.getSharedPreferences("sic_export", Context.MODE_PRIVATE)
    val savedHeading: ExportHeading get() = ExportHeading(preferences.getString("project", "").orEmpty(),
        preferences.getString("road", "").orEmpty(), preferences.getString("section", "").orEmpty())

    fun prepare(format: SicExportFormat?, route: String?, heading: ExportHeading) {
        if (mutableState.value.busy) return
        val previous = mutableState.value.prepared
        mutableState.value = LocalExportState(busy = true)
        scope.launch {
            try {
                val prepared = withContext(Dispatchers.IO) {
                    previous?.file?.delete()
                    val records = database.inventoryDao().recordsForExport(format?.code, route)
                    require(records.isNotEmpty()) { "No hay registros activos para exportar con esta selección." }
                    val directory = File(context.cacheDir, "sic_exports")
                    check(directory.isDirectory || directory.mkdirs()) { "No se pudo preparar la carpeta de exportación." }
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val file = File(directory, "${format?.code ?: "SIC_17-23"}_${timestamp}_${UUID.randomUUID().toString().take(8)}.${if (format == null) "zip" else "xlsx"}")
                    try {
                        file.outputStream().buffered().use { SicExcelWriter.write(it, records, format, heading) }
                    } catch (error: Throwable) { file.delete(); throw error }
                    preferences.edit().putString("project", heading.project).putString("road", heading.road)
                        .putString("section", heading.section).apply()
                    PreparedSicExport(file, format == null, records.size, records.map { it.record.sicCode }.distinct().size)
                }
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "Archivo preparado con ${prepared.recordCount} registros. Elige dónde guardarlo.")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableState.value = LocalExportState(message = error.message ?: "No se pudo generar el archivo.")
            }
        }
    }

    fun save(uri: Uri) {
        if (mutableState.value.busy) return
        val prepared = mutableState.value.prepared
        if (prepared == null) {
            mutableState.value = LocalExportState(message = "El archivo preparado ya no está disponible. Vuelve a generarlo.")
            return
        }
        mutableState.value = mutableState.value.copy(busy = true, message = "Guardando archivo…")
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    require(prepared.file.isFile) { "El archivo temporal ya no está disponible. Vuelve a generarlo." }
                    val stream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: throw IllegalStateException("No se pudo abrir el destino seleccionado.")
                    stream.use { out -> prepared.file.inputStream().use { it.copyTo(out) } }
                }
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "Guardado: ${prepared.file.name} (${prepared.recordCount} registros, ${prepared.workbookCount} Excel).")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                mutableState.value = LocalExportState(prepared = prepared,
                    message = "No se completó el guardado: ${error.message ?: "comprueba el espacio y el destino"}. Puedes reintentarlo.")
            }
        }
    }
}
