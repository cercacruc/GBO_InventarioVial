package com.tuempresa.inventariovial.gis

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.road.ChainageCalculator
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.field.FieldSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalKmlExport(private val context: Context,private val database: InventoryDatabase) {
    suspend fun prepare(calculator: ChainageCalculator, routeCode: String? = null, kmz: Boolean = false): File = withContext(Dispatchers.IO) {
        val allReference = RoadReferenceRepository(context).loadFromAssets()
        val reference = if (routeCode == null) allReference else allReference.forSelectedRoute(routeCode)
        val dao = database.inventoryDao()
        RoadAxisStorage(dao).refresh(allReference, FieldSettings.load(context).matchConfig(), routeCode)
        val snapshots=dao.activeSnapshots().filter { it.record.sicCode != "SCAP" &&
            (routeCode == null || normalizedRoadCode(it.record.routeCode) == normalizedRoadCode(routeCode)) }
        require(snapshots.isNotEmpty() || reference.segments.isNotEmpty()) { "No hay registros ni eje para exportar." }
        val inspectors = snapshots.mapNotNull { it.record.sessionId }.distinct().mapNotNull { id ->
            dao.sessionById(id)?.let { id to it.operator }
        }.toMap()
        val directory=File(context.cacheDir,"kml").apply { mkdirs() }
        val resources = if (kmz) KmlMapImages.resources() else emptyMap()
        val kml = KmlExporter.render(snapshots, calculator, reference,
            iconHref = if (kmz) "marker.png" else null, inspectors = inspectors)
        File(directory,"inventario-${UUID.randomUUID()}.${if (kmz) "kmz" else "kml"}").apply {
            if (kmz) outputStream().use { KmlExporter.writeKmz(it, kml, resources) } else writeText(kml,Charsets.UTF_8)
        }
    }
    fun intent(file: File, open: Boolean): Intent {
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        val mime=if (file.extension == "kmz") "application/vnd.google-earth.kmz" else "application/vnd.google-earth.kml+xml"
        return Intent(if(open) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply {
            if(open) setDataAndType(uri,mime) else { type=mime;putExtra(Intent.EXTRA_STREAM,uri) }
            clipData=ClipData.newRawUri("Inventario KML",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
