package com.tuempresa.inventariovial

import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.road.*
import java.util.Locale

data class PlannedPhoto(val photoId: String, val recordId: String, val photoIndex: Int,
    val localPath: String, val photoName: String, val plannedPath: String)
data class PlannedAssetFolder(val asset: OrderedAsset, val plannedPath: String?, val photos: List<PlannedPhoto>)
data class DrivePathPlan(val folders: List<PlannedAssetFolder>, val warnings: List<String>)

/** Pure plan only: never creates folders, moves files, or enqueues uploads. */
object DrivePathPlanner {
    fun plan(records: List<InventoryRecordWithPhotos>, calculator: ChainageCalculator = ChainageCalculator()): DrivePathPlan {
        val warnings = mutableListOf<String>()
        val folders = RoadOrdering.orderAssetsForRoute(records, calculator).map { asset ->
            warnings += asset.warnings.map { "${asset.recordId}: $it" }
            val route = asset.routeCode
            val path = if (asset.sibCode != null && route.isNotBlank() && route.none { it == '/' || it == '\\' } && route !in setOf(".", ".."))
                "$route/${asset.sibCode}/${asset.folderName}" else null
            if (path == null) warnings += "${asset.recordId}: ruta de carpetas no planificable."
            val photos = if (path == null) emptyList() else asset.item.photos.sortedWith(compareBy({ it.photoIndex }, { it.id }))
                .mapIndexed { index, photo ->
                    require(photo.recordId == asset.recordId) { "Fotografía asociada a otro registro: ${photo.id}" }
                    val name = String.format(Locale.ROOT, "Foto %02d.jpg", index + 1)
                    PlannedPhoto(photo.id, photo.recordId, photo.photoIndex, photo.localPath, name, "$path/$name")
                }
            PlannedAssetFolder(asset, path, photos)
        }
        return DrivePathPlan(folders, warnings)
    }
}
