package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction

import com.tuempresa.inventariovial.data.database.InventoryDatabase
import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity
import com.tuempresa.inventariovial.data.entity.Sic23Entity


class InventoryRepository(
    private val database:
    InventoryDatabase
) {

    private val dao =
        database.inventoryDao()


    fun observeRecordCountByDate(
        date: String
    ) =
        dao.observeRecordCountByDate(
            date
        )


    fun observePendingPhotoCount() =
        dao.observePendingPhotoCount()


    fun observeHistory() =
        dao.observeHistory()


    fun observeAllRecords() =
        dao.observeAllRecords()


    suspend fun saveSic17(
        record: InventoryRecordEntity,
        detail: Sic17Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic17(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic18(
        record: InventoryRecordEntity,
        detail: Sic18Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic18(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic19(
        record: InventoryRecordEntity,
        detail: Sic19Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic19(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic20(
        record: InventoryRecordEntity,
        detail: Sic20Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic20(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic21(
        record: InventoryRecordEntity,
        detail: Sic21Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic21(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic23(record: InventoryRecordEntity, detail: Sic23Entity, photos: List<PhotoEntity>) {
        database.withTransaction {
            dao.insertRecord(record)
            dao.insertSic23(detail)
            dao.insertPhotos(photos)
        }
    }

    suspend fun saveSic22(
        record: InventoryRecordEntity,
        detail: Sic22Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic22(detail)

            dao.insertPhotos(photos)
        }
    }
}