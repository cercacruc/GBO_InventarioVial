package com.tuempresa.inventariovial.repository

import androidx.room.withTransaction
import com.tuempresa.inventariovial.data.entity.*
import com.tuempresa.inventariovial.supplementary.*

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


    fun observeHistory() = dao.observeHistory()

    fun observeAllRecords() =
        dao.observeAllRecords()


    suspend fun saveSic17(
        record: InventoryRecordEntity,
        detail: Sic17Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

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

            dao.insertOrCompleteDraft(record)

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

            dao.insertOrCompleteDraft(record)

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

            dao.insertOrCompleteDraft(record)

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

            dao.insertOrCompleteDraft(record)

            dao.insertSic21(detail)

            dao.insertPhotos(photos)
        }
    }


    suspend fun saveSic22(
        record: InventoryRecordEntity,
        detail: Sic22Entity,
        photos: List<PhotoEntity>
    ) {

        database.withTransaction {

            dao.insertOrCompleteDraft(record)

            dao.insertSic22(detail)

            dao.insertPhotos(photos)
        }
    }
    suspend fun saveSic23(record: InventoryRecordEntity, detail: Sic23Entity, photos: List<PhotoEntity>) {
        database.withTransaction {
            dao.insertOrCompleteDraft(record)
            dao.insertSic23(detail)
            dao.insertPhotos(photos)
        }
    }

    suspend fun saveSupplementary(recordId: String, state: SupplementaryFormState) {
        check(state.format.enabled) { "Formato deshabilitado." }
        require(state.validate().isEmpty()) { state.validate().joinToString("\n") }
        database.withTransaction {
            val snapshot=dao.snapshot(recordId) ?: error("Registro inexistente.")
            require(snapshot.record.status=="ACTIVE") { "Solo se pueden editar registros activos." }
            if(state.format!=SupplementaryFormat.SIC18A) {
                require(snapshot.record.sicCode=="SIC-17") { "El formato requiere un puente." }
                val bridge=snapshot.sic17?.bridgeCode.orEmpty()
                require(state.values["bridgeCode"].orEmpty()==bridge) { "El código de puente debe coincidir con SIC-17." }
            } else {
                require(snapshot.record.sicCode=="SIC-18") { "El formato requiere una alcantarilla." }
                val parent=requireNotNull(snapshot.sic18)
                require(state.values["classCode"]==parent.classCode && state.values["typeCode"]==parent.typeCode && state.values["spans"]==parent.spans?.toString()) {"Clase, tipo y ojos/vanos deben coincidir con SIC-18; vuelve a abrir la ficha."}
            }
            when(state.format) {
                SupplementaryFormat.SIC17A -> dao.saveSic17A(Sic17AEntity.from(recordId,state.values))
                SupplementaryFormat.SIC17B -> dao.saveSic17B(Sic17BEntity.from(recordId,state.values))
                SupplementaryFormat.SIC18A -> dao.saveSic18A(Sic18AEntity.from(recordId,state.values))
            }
            dao.markServerPending(recordId,maxOf(System.currentTimeMillis(),snapshot.record.updatedAt+1))
        }
    }
}