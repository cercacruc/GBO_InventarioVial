package com.tuempresa.inventariovial.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity

import kotlinx.coroutines.flow.Flow


@Dao
interface InventoryDao {

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
        FROM photos
        WHERE syncStatus != 'SYNCED'
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