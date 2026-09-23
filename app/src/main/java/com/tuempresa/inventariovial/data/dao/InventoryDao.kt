package com.tuempresa.inventariovial.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.InventoryRecordWithPhotos
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity
import com.tuempresa.inventariovial.data.entity.Sic23Entity

import kotlinx.coroutines.flow.Flow


@Dao
interface InventoryDao {
    @androidx.room.Transaction
    @Query("""SELECT * FROM inventory_records
        WHERE status = 'ACTIVE' AND sicCode IN ('SIC-17','SIC-18','SIC-19','SIC-20','SIC-21','SIC-22','SIC-23')
        AND (:sicCode IS NULL OR sicCode = :sicCode) AND (:route IS NULL OR routeCode = :route)
        ORDER BY routeCode, roadbedCode, CAST(startPrCode AS REAL) * 1000 + startDistanceM, createdAt, id""")
    suspend fun recordsForExport(sicCode: String?, route: String?): List<com.tuempresa.inventariovial.data.entity.InventoryRecordForExport>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSic23(detail: Sic23Entity)

    @androidx.room.Transaction
    @Query("SELECT * FROM inventory_records ORDER BY routeCode, roadbedCode, CAST(startPrCode AS REAL) * 1000 + startDistanceM, createdAt")
    fun observeHistory(): Flow<List<InventoryRecordWithPhotos>>

    @Query("SELECT photos.* FROM photos INNER JOIN inventory_records ON photos.recordId = inventory_records.id WHERE photos.syncStatus != 'SYNCED' AND inventory_records.status = 'ACTIVE'")
    suspend fun pendingPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    fun photoById(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE localPath = :path LIMIT 1")
    fun photoByPath(path: String): PhotoEntity?

    @androidx.room.Transaction
    fun completePhotoUpload(photoPath: String, driveFileId: String?) {
        markPhotoSynced(photoPath, driveFileId)
        photoByPath(photoPath)?.let { refreshRecordPhotoStatus(it.recordId) }
    }

    @Query("SELECT status FROM inventory_records WHERE id = :recordId")
    fun recordStatus(recordId: String): String?

    @Query(
        """
    SELECT *
    FROM inventory_records
    WHERE id = :recordId
    LIMIT 1
    """
    )
    suspend fun recordById(
        recordId: String
    ): InventoryRecordEntity?


    @Query("UPDATE inventory_records SET status = :status, excelSyncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun setRecordStatus(id: String, status: String, now: Long)

    @Query("UPDATE inventory_records SET routeCode = :route, roadbedCode = :roadbed, startPrCode = :startPr, startDistanceM = :startDistance, endPrCode = :endPr, endDistanceM = :endDistance, sideCode = :side, observations = :observations, excelSyncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun updateCoreFields(id: String, route: String, roadbed: String, startPr: String, startDistance: Double, endPr: String?, endDistance: Double?, side: String?, observations: String?, now: Long)

    @Query("UPDATE inventory_records SET photoSyncStatus = CASE WHEN EXISTS (SELECT 1 FROM photos WHERE photos.recordId = inventory_records.id AND syncStatus != 'SYNCED') THEN 'PENDING' ELSE 'SYNCED' END WHERE id = :recordId")
    fun refreshRecordPhotoStatus(recordId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhotos(photos: List<PhotoEntity>)


    // =====================================================
    // INSERTAR REGISTRO PRINCIPAL
    // =====================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertRecord(
        record: InventoryRecordEntity
    )


    // =====================================================
    // FOTOGRAFÍAS
    // =====================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertPhoto(
        photo: PhotoEntity
    )


    // =====================================================
    // DETALLES SIC
    // =====================================================

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic17(
        detail: Sic17Entity
    )


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic18(
        detail: Sic18Entity
    )


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic19(
        detail: Sic19Entity
    )


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic20(
        detail: Sic20Entity
    )


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic21(
        detail: Sic21Entity
    )


    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertSic22(
        detail: Sic22Entity
    )


    // =====================================================
    // CONSULTAS
    // =====================================================

    @Query(
        """
        SELECT *
        FROM inventory_records
        ORDER BY createdAt DESC
        """
    )
    fun observeAllRecords():
            Flow<List<InventoryRecordEntity>>


    @Query(
        """
        SELECT COUNT(*)
        FROM inventory_records
        WHERE surveyDate = :surveyDate
        AND status = 'ACTIVE'
        """
    )
    fun observeRecordCountByDate(
        surveyDate: String
    ): Flow<Int>


    @Query(
        """
        SELECT COUNT(*)
        FROM photos INNER JOIN inventory_records ON photos.recordId = inventory_records.id
        WHERE photos.syncStatus != 'SYNCED' AND inventory_records.status = 'ACTIVE'
        """
    )
    fun observePendingPhotoCount():
            Flow<Int>


    // =====================================================
    // SINCRONIZACIÓN DRIVE
    // =====================================================

    @Query(
        """
        UPDATE photos
        SET syncStatus = :status
        WHERE localPath = :photoPath
        """
    )
    fun updatePhotoSyncStatusByPath(
        photoPath: String,
        status: String
    )


    @Query(
        """
        UPDATE photos
        SET
            syncStatus = 'SYNCED',
            driveFileId = :driveFileId
        WHERE localPath = :photoPath
        """
    )
    fun markPhotoSynced(
        photoPath: String,
        driveFileId: String?
    )
}
