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


    fun observeAllRecords() =
        dao.observeAllRecords()


    suspend fun saveSic17(
        record: InventoryRecordEntity,
        detail: Sic17Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic17(detail)

            dao.insertPhoto(photo)
        }
    }


    suspend fun saveSic18(
        record: InventoryRecordEntity,
        detail: Sic18Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic18(detail)

            dao.insertPhoto(photo)
        }
    }


    suspend fun saveSic19(
        record: InventoryRecordEntity,
        detail: Sic19Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic19(detail)

            dao.insertPhoto(photo)
        }
    }


    suspend fun saveSic20(
        record: InventoryRecordEntity,
        detail: Sic20Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic20(detail)

            dao.insertPhoto(photo)
        }
    }


    suspend fun saveSic21(
        record: InventoryRecordEntity,
        detail: Sic21Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic21(detail)

            dao.insertPhoto(photo)
        }
    }


    suspend fun saveSic22(
        record: InventoryRecordEntity,
        detail: Sic22Entity,
        photo: PhotoEntity
    ) {

        database.withTransaction {

            dao.insertRecord(record)

            dao.insertSic22(detail)

            dao.insertPhoto(photo)
        }
    }
}