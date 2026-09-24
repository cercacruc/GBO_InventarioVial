package com.tuempresa.inventariovial.gis

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.road.ChainageCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LocalKmlExport(private val context: Context,private val database: InventoryDatabase) {
    suspend fun prepare(calculator: ChainageCalculator): File = withContext(Dispatchers.IO) {
        val snapshots=database.inventoryDao().activeSnapshots()
        require(snapshots.isNotEmpty()) { "No hay registros activos para exportar." }
        val directory=File(context.cacheDir,"kml").apply { mkdirs() }
        File(directory,"inventario-${UUID.randomUUID()}.kml").apply { writeText(KmlExporter.render(snapshots,calculator),Charsets.UTF_8) }
    }
    fun intent(file: File, open: Boolean): Intent {
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        val mime="application/vnd.google-earth.kml+xml"
        return Intent(if(open) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply {
            if(open) setDataAndType(uri,mime) else { type=mime;putExtra(Intent.EXTRA_STREAM,uri) }
            clipData=ClipData.newRawUri("Inventario KML",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
