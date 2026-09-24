package com.tuempresa.inventariovial.viewmodel

import android.app.Application
import android.graphics.BitmapFactory
import com.tuempresa.inventariovial.field.FieldController
import com.tuempresa.inventariovial.road.*
import com.tuempresa.inventariovial.validation.*
import com.tuempresa.inventariovial.server.scheduleServerSync
import com.tuempresa.inventariovial.tracking.TrackCaptureService
import com.tuempresa.inventariovial.supplementary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

import com.tuempresa.inventariovial.data.database.InventoryDatabase

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

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail
import com.tuempresa.inventariovial.model.form.Sic23FormState

import com.tuempresa.inventariovial.repository.InventoryRepository

import com.tuempresa.inventariovial.scheduleDriveUpload

import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.tuempresa.inventariovial.DriveFolderRouter

class InventoryViewModel(
    application: Application
) : AndroidViewModel(
    application
) {

    // =========================================================
    // BASE DE DATOS
    // =========================================================

    private val database =
        InventoryDatabase.getInstance(
            application
        )


    private val repository =
        InventoryRepository(
            database
        )


    // =========================================================
    // FECHA ACTUAL
    // =========================================================

    private val today =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(
            Date()
        )


    // =========================================================
    // CONTADORES OBSERVADOS DESDE ROOM
    // =========================================================

    val recordsToday =
        repository
            .observeRecordCountByDate(
                today
            )
            .stateIn(
                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    0
            )


    val pendingSync =
        repository
            .observePendingPhotoCount()
            .stateIn(
                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    0
            )


    val field = FieldController(application, database, viewModelScope)
    val sequencedHistory = combine(repository.observeHistory(),field.reference) { records, reference ->
        RoadOrdering.order(records,ChainageCalculator(reference.prs))
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())
    val history: StateFlow<List<InventoryRecordWithPhotos>> = sequencedHistory.map { rows -> rows.map { it.item } }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),emptyList())

    data class PendingSave(val request: InventorySaveRequest, val warnings: List<ValidationWarning>,
        val onSuccess: ()->Unit, val onError: (String)->Unit)
    private val _pendingSave=MutableStateFlow<PendingSave?>(null)
    val pendingSave=_pendingSave.asStateFlow()
    fun dismissSaveWarnings() { _pendingSave.value=null }
    fun confirmSaveWarnings() {
        val pending=_pendingSave.value ?: return
        _pendingSave.value=null
        saveRecord(pending.request,pending.onSuccess,pending.onError,true)
    }
    suspend fun supplementaryState(recordId: String, format: SupplementaryFormat): SupplementaryFormState {
        val snapshot=field.dao.snapshot(recordId) ?: error("Registro inexistente.")
        val values=when(format) {
            SupplementaryFormat.SIC17A -> snapshot.sic17a?.values() ?: mapOf("bridgeCode" to snapshot.sic17?.bridgeCode.orEmpty())
            SupplementaryFormat.SIC17B -> snapshot.sic17b?.values() ?: mapOf("bridgeCode" to snapshot.sic17?.bridgeCode.orEmpty())
            SupplementaryFormat.SIC18A -> snapshot.sic18a?.values() ?: mapOf("classCode" to snapshot.sic18?.classCode.orEmpty(),
                "typeCode" to snapshot.sic18?.typeCode.orEmpty(),"spans" to snapshot.sic18?.spans?.toString().orEmpty())
        }
        return SupplementaryFormState(format,values)
    }
    fun saveSupplementary(recordId: String,state: SupplementaryFormState,onSuccess: ()->Unit,onError: (String)->Unit) {
        viewModelScope.launch {
            try { repository.saveSupplementary(recordId,state);scheduleServerSync(getApplication());onSuccess() }
            catch(error:Exception) { onError(error.message ?: "No se pudo guardar el formato.") }
        }
    }
    val localExport = com.tuempresa.inventariovial.export.LocalSicExport(application, database, viewModelScope)
    private var saving = false

    fun syncPending(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val photos = database.inventoryDao().pendingPhotos()
                photos.forEach { enqueuePhoto(it) }
                scheduleServerSync(getApplication())
                onResult("Fotografías programadas: ${photos.size}")
            } catch (error: Exception) {
                onResult(error.message ?: "No se pudo programar la sincronización.")
            }
        }
    }

    private suspend fun enqueuePhoto(
        photo: PhotoEntity
    ) {

        val record =
            database
                .inventoryDao()
                .recordById(
                    photo.recordId
                )
                ?: throw IllegalStateException(
                    "No se encontró el registro asociado a la fotografía."
                )


        val routeCode =
            record
                .routeCode
                .trim()
                .uppercase()


        val sicCode =
            record
                .sicCode
                .trim()
                .uppercase()


        val sibCode =
            DriveFolderRouter
                .resolveSib(

                    sicCode =
                        sicCode,

                    assetType =
                        record.assetType
                )


        val driveFileName =
            photo
                .generatedFileName

                ?: java.io.File(
                    photo.localPath
                ).name


        scheduleDriveUpload(

            context =
                getApplication(),

            photoPath =
                photo.localPath,

            driveFileName =
                driveFileName,

            routeCode =
                routeCode,

            sibCode =
                sibCode,

            sicCode =
                sicCode,

            photoId =
                photo.id,

            recordId =
                photo.recordId
        )
    }

    fun setRecordStatus(id: String, active: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val previous=database.inventoryDao().recordById(id) ?: error("Registro inexistente.")
                database.inventoryDao().setRecordStatus(id, if (active) "ACTIVE" else "ANNULLED", maxOf(System.currentTimeMillis(),previous.updatedAt+1))
                scheduleServerSync(getApplication())
            } catch (error: Exception) { onError(error.message ?: "No se pudo cambiar el estado.") }
        }
    }

    fun updateCoreFields(record: InventoryRecordEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                require(record.routeCode.isNotBlank() && record.roadbedCode.isNotBlank()) { "Ruta y calzada son obligatorias." }
                require(record.startDistanceM.isFinite() && record.startDistanceM >= 0) { "Distancia inicial inválida." }
                require(record.endDistanceM == null || (record.endDistanceM.isFinite() && record.endDistanceM >= 0)) { "Distancia final inválida." }
                if (record.sicCode == "SIC-23") {
                    Sic23FormState.locationError(record.routeCode, record.roadbedCode,
                        record.startPrCode, record.startDistanceM.toString(), record.endPrCode,
                        record.endDistanceM?.toString(), record.sideCode, checkEstimatedOrder = false)?.let { throw IllegalArgumentException(it) }
                }
                database.inventoryDao().updateCoreFields(record.id, record.routeCode.trim().uppercase(),
                    record.roadbedCode.trim().uppercase(), normalizeRequiredPr(record.startPrCode), record.startDistanceM,
                    normalizeOptionalPr(record.endPrCode), record.endDistanceM, record.sideCode, record.observations, maxOf(System.currentTimeMillis(),record.updatedAt+1))
                scheduleServerSync(getApplication())
                onSuccess()
            } catch (error: Exception) { onError(error.message ?: "No se pudo editar el registro.") }
        }
    }

    // =========================================================
    // GUARDAR REGISTRO
    // =========================================================

    fun saveRecord(
        request: InventorySaveRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        warningsAccepted: Boolean = false
    ) {

        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                if (request.detail is SicFormDetail.Sic23) {
                    require(request.sicCode == "SIC-23") { "Formato SIC incompatible." }
                    request.detail.state.validationError()?.let { throw IllegalArgumentException(it) }
                    Sic23FormState.locationError(request.routeCode, request.roadbedCode,
                        request.startPrCode, request.startDistanceM, request.endPrCode,
                        request.endDistanceM, request.sideCode, checkEstimatedOrder = false)?.let { throw IllegalArgumentException(it) }
                }
                com.tuempresa.inventariovial.field.SurveyPreferences(getApplication()).validate(request)?.let { error(it) }
                val errors=CaptureValidation.errors(request)
                require(errors.isEmpty()) { errors.joinToString("\n") { it.message } }
                require(TrackCaptureService.activeRecordId.value==null || TrackCaptureService.activeRecordId.value!=request.recordId) { "Finaliza el recorrido antes de guardar." }
                require(request.photoPaths.all { java.io.File(it).isFile }) { "No se encontró una fotografía." }
                if(!warningsAccepted) {
                    val dimensions=withContext(Dispatchers.IO) { request.photoPaths.map { path ->
                        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                        BitmapFactory.decodeFile(path,bounds)
                        PhotoDimensions(bounds.outWidth,bounds.outHeight)
                    } }
                    val warnings=CaptureValidation.warnings(request,field.reference.value,System.currentTimeMillis(),dimensions,
                        field.settings.value.qualityConfig(),field.settings.value.matchConfig())
                    _pendingSave.value=PendingSave(request,warnings,onSuccess,onError); return@launch
                }

                // -------------------------------------------------
                // IDENTIFICADORES ÚNICOS
                // -------------------------------------------------

                val recordId = request.recordId ?: UUID.randomUUID().toString()


                val photoId =
                    UUID
                        .randomUUID()
                        .toString()


                val now =
                    System.currentTimeMillis()


                // -------------------------------------------------
                // GENERAR NOMBRE PARA GOOGLE DRIVE
                // -------------------------------------------------

                val driveFileName =
                    buildDriveFileName(
                        request =
                            request,

                        photoIndex =
                            1
                    )


                // -------------------------------------------------
                // REGISTRO PRINCIPAL
                // -------------------------------------------------

                val record =
                    InventoryRecordEntity(

                        id =
                            recordId,
                        segment = request.segment, surveyDirection = request.direction,


                        sicCode =
                            request.sicCode,


                        assetType =
                            request.assetType,


                        routeCode =
                            request
                                .routeCode
                                .trim()
                                .uppercase(),


                        roadbedCode =
                            request
                                .roadbedCode
                                .trim()
                                .uppercase(),


                        startPrCode =
                            normalizeRequiredPr(
                                request.startPrCode
                            ),


                        startDistanceM =
                            requiredDouble(
                                value =
                                    request.startDistanceM,

                                fieldName =
                                    "Distancia de inicio"
                            ),


                        endPrCode =
                            normalizeOptionalPr(
                                request.endPrCode
                            ),


                        endDistanceM =
                            optionalDouble(
                                value =
                                    request.endDistanceM,

                                fieldName =
                                    "Distancia de fin"
                            ),


                        sideCode =
                            request
                                .sideCode
                                ?.trim()
                                ?.uppercase(),


                        latitude =
                            request.latitude,


                        longitude =
                            request.longitude,


                        altitudeM = request.location?.altitude?.takeIf { it.isFinite() },


                        gpsAccuracyM =
                            request
                                .gpsAccuracyM
                                ?.toDouble()?.takeIf { it.isFinite() && it>=0 },


                        surveyDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(now)),


                        observations =
                            request
                                .observations
                                .trim()
                                .ifBlank {
                                    null
                                },


                        status =
                            "ACTIVE",


                        photoSyncStatus = if(request.photoPaths.isEmpty()) "SYNCED" else "PENDING",


                        excelSyncStatus =
                            "PENDING",


                        createdAt =
                            now,


                        endLatitude = request.endLatitude,
                        endLongitude = request.endLongitude,
                        endGpsAccuracyM = request.endGpsAccuracyM?.toDouble()?.takeIf { it.isFinite() && it>=0 },
                        gpsTimestamp = request.location?.timestamp,
                        endGpsTimestamp = request.endLocation?.timestamp,
                        gnssProvider = request.location?.provider?.name ?: "TABLET",
                        gnssFixType = request.location?.fixType,
                        verticalAccuracyM = request.location?.verticalAccuracy?.toDouble(),
                        satellites = request.location?.satellites,
                        hdop = request.location?.hdop,
                        correctionAge = request.location?.correctionAge,
                        isRtkFixed = request.location?.isRtkFixed ?: false,
                        locationSource = request.locationSource,
                        sideSource = request.sideSource,
                        sessionId = request.sessionId,
                        updatedAt =
                            now
                    )


                // -------------------------------------------------
                // FOTO
                // -------------------------------------------------

                val photo =
                    PhotoEntity(

                        id =
                            photoId,


                        recordId =
                            recordId,


                        photoIndex =
                            1,


                        // Ruta física interna de Android.
                        // No necesariamente tiene el mismo
                        // nombre que veremos en Drive.
                        localPath =
                            request.photoPath,


                        isPrimary =
                            true,


                        // Nombre definitivo que usará Drive.
                        generatedFileName =
                            driveFileName,


                        driveFolderId =
                            null,


                        driveFileId =
                            null,


                        syncStatus =
                            "PENDING",


                        createdAt =
                            now
                    )


                // =================================================
                // SIC-17
                // =================================================

                val photos = request.photoPaths.distinct().mapIndexed { index, path ->
                    photo.copy(id = UUID.randomUUID().toString(), localPath = path,
                        originalPath = path, stampedPath = request.stampedPaths[path],
                        photoIndex = index + 1, isPrimary = index == 0,
                        generatedFileName = buildDriveFileName(request, index + 1))
                }
                when (
                    val detail =
                        request.detail
                ) {

                    is SicFormDetail.Sic17 -> {

                        val state =
                            detail.state


                        repository.saveSic17(

                            record =
                                record,


                            detail =
                                Sic17Entity(

                                    recordId =
                                        recordId,


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    bridgeCode =
                                        state
                                            .bridgeCode
                                            .trim()
                                            .uppercase()
                                            .ifBlank {
                                                null
                                            },


                                    inventoriedCode =
                                        state
                                            .inventoriedCode,


                                    spans =
                                        optionalInt(
                                            value =
                                                state.spans,

                                            fieldName =
                                                "Número de vanos"
                                        ),


                                    dimension1LengthM =
                                        optionalDouble(
                                            value =
                                                state.dimension1LengthM,

                                            fieldName =
                                                "Longitud del puente"
                                        ),


                                    dimension2LowerHeightM =
                                        optionalDouble(
                                            value =
                                                state.dimension2LowerHeightM,

                                            fieldName =
                                                "Altura inferior"
                                        ),


                                    structuralConditionCode =
                                        state
                                            .structuralConditionCode,


                                    functionalConditionCode =
                                        state
                                            .functionalConditionCode,


                                    serviceTypeCode =
                                        state
                                            .serviceTypeCode,


                                    singularityCode =
                                        state
                                            .singularityCode,


                                    singularityName =
                                        state
                                            .singularityName
                                            .trim()
                                            .uppercase()
                                            .ifBlank {
                                                null
                                            },


                                    dimension3UpperHeightM =
                                        optionalDouble(
                                            value =
                                                state.dimension3UpperHeightM,

                                            fieldName =
                                                "Altura superior"
                                        )
                                ),


                            photos = photos
                        )
                    }


                    // =================================================
                    // SIC-18
                    // =================================================

                    is SicFormDetail.Sic18 -> {

                        val state =
                            detail.state


                        repository.saveSic18(

                            record =
                                record,


                            detail =
                                Sic18Entity(

                                    recordId =
                                        recordId,
                                    sectionShape = if(state.crossSectionCode == "2") state.sectionShape else null,
                                    structuralDamagePercent = state.structuralDamagePercent.replace(',','.').toDoubleOrNull(),
                                    functionalObstructionPercent = state.functionalObstructionPercent.replace(',','.').toDoubleOrNull(),


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    spans =
                                        optionalInt(
                                            value =
                                                state.spans,

                                            fieldName =
                                                "Ojos / vanos"
                                        ),


                                    crossSectionCode =
                                        state
                                            .crossSectionCode,


                                    dimension1M =
                                        optionalDouble(
                                            value =
                                                state.dimension1M,

                                            fieldName =
                                                "Dimensión 1"
                                        ),


                                    dimension2M =
                                        optionalDouble(
                                            value =
                                                if(state.usesDimension2) state.dimension2M else "",

                                            fieldName =
                                                "Dimensión 2"
                                        ),


                                    structuralConditionCode =
                                        state
                                            .structuralConditionCode,


                                    functionalConditionCode =
                                        state
                                            .functionalConditionCode
                                ),


                            photos = photos
                        )
                    }


                    // =================================================
                    // SIC-19
                    // =================================================

                    is SicFormDetail.Sic19 -> {

                        val state =
                            detail.state


                        repository.saveSic19(

                            record =
                                record,


                            detail =
                                Sic19Entity(

                                    recordId =
                                        recordId,


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    crossSectionCode =
                                        state
                                            .crossSectionCode,


                                    structuralConditionCode =
                                        state
                                            .structuralConditionCode,


                                    functionalConditionCode =
                                        state
                                            .functionalConditionCode
                                ),


                            photos = photos
                        )
                    }


                    // =================================================
                    // SIC-20
                    // =================================================

                    is SicFormDetail.Sic20 -> {

                        val state =
                            detail.state


                        repository.saveSic20(

                            record =
                                record,


                            detail =
                                Sic20Entity(

                                    recordId =
                                        recordId,


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    dimension1M =
                                        optionalDouble(
                                            value =
                                                state.dimension1M,

                                            fieldName =
                                                "Dimensión 1"
                                        ),


                                    dimension2M =
                                        if (
                                            state.usesDimension2
                                        ) {

                                            optionalDouble(
                                                value =
                                                    state.dimension2M,

                                                fieldName =
                                                    "Dimensión 2"
                                            )

                                        } else {

                                            null
                                        },


                                    structuralConditionCode =
                                        state
                                            .structuralConditionCode,


                                    functionalConditionCode =
                                        if (
                                            state
                                                .usesFunctionalCondition
                                        ) {

                                            state
                                                .functionalConditionCode

                                        } else {

                                            null
                                        }
                                ),


                            photos = photos
                        )
                    }


                    // =================================================
                    // SIC-21
                    // =================================================

                    is SicFormDetail.Sic21 -> {

                        val state =
                            detail.state


                        repository.saveSic21(

                            record =
                                record,


                            detail =
                                Sic21Entity(

                                    recordId =
                                        recordId,


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    materialCode =
                                        state
                                            .materialCode,


                                    conditionCode =
                                        state
                                            .conditionCode
                                ),


                            photos = photos
                        )
                    }


                    // =================================================
                    // SIC-22
                    // =================================================

                    is SicFormDetail.Sic23 -> {
                        val state = detail.state
                        repository.saveSic23(record, Sic23Entity(
                            recordId = recordId, classCode = state.classCode,
                            typeCode = state.typeCode.takeIf { state.typeOptions.isNotEmpty() },
                            widthM = if (state.usesWidth) requiredDouble(state.widthM, "Ancho") else null,
                            description = state.normalizedDescription()
                        ), photos)
                    }

                    is SicFormDetail.Sic22 -> {

                        val state =
                            detail.state


                        repository.saveSic22(

                            record =
                                record,


                            detail =
                                Sic22Entity(

                                    recordId =
                                        recordId,


                                    classCode =
                                        state.classCode,


                                    typeCode =
                                        state.typeCode,


                                    materialCode =
                                        state
                                            .materialCode,


                                    signalCode =
                                        state
                                            .signalCode
                                            .trim()
                                            .uppercase()
                                            .ifBlank {
                                                null
                                            },


                                    kilometerPostNumber =
                                        state
                                            .kilometerPostNumber
                                            .trim()
                                            .ifBlank {
                                                null
                                            },


                                    conditionCode =
                                        state
                                            .conditionCode,


                                    signWidthM =
                                        optionalDouble(
                                            value =
                                                state.signWidthM,

                                            fieldName =
                                                "Ancho de señal"
                                        ),


                                    signHeightM =
                                        optionalDouble(
                                            value =
                                                state.signHeightM,

                                            fieldName =
                                                "Alto de señal"
                                        ),


                                    lowerEdgeHeightM =
                                        optionalDouble(
                                            value =
                                                state.lowerEdgeHeightM,

                                            fieldName =
                                                "Altura del borde inferior"
                                        )
                                ),


                            photos = photos
                        )
                    }
                }


                // =================================================
                // ROOM YA SE GUARDÓ CORRECTAMENTE.
                //
                // SOLO DESPUÉS PROGRAMAMOS LA SUBIDA A DRIVE.
                // =================================================

                // Local save has committed; scheduling can be retried from Home.
                com.tuempresa.inventariovial.field.SurveyPreferences(getApplication()).remember(request)
                runCatching { photos.forEach { enqueuePhoto(it) } }
                runCatching { scheduleServerSync(getApplication()) }
                onSuccess()


            } catch (
                error: Exception
            ) {

                onError(
                    error.message
                        ?: "No se pudo guardar el registro."
                )
            } finally {
                saving = false
            }
        }
    }


    // =========================================================
    // GENERACIÓN DEL NOMBRE DE LA FOTO
    // =========================================================

    private fun buildDriveFileName(
        request: InventorySaveRequest,
        photoIndex: Int
    ): String {

        // -----------------------------------------------------
        // RUTA
        // -----------------------------------------------------

        val route =
            cleanNamePart(
                request.routeCode
            )


        // -----------------------------------------------------
        // PROGRESIVA
        // -----------------------------------------------------

        val progressive =
            buildProgressive(

                startPrCode =
                    request.startPrCode,


                startDistanceM =
                    request.startDistanceM
            )


        // -----------------------------------------------------
        // LADO
        // -----------------------------------------------------

        val side =
            request
                .sideCode
                ?.trim()
                ?.uppercase()
                ?.takeIf {
                    it.isNotBlank()
                }


        // -----------------------------------------------------
        // TIPO DE ELEMENTO
        // -----------------------------------------------------

        val elementName =
            when (
                val detail =
                    request.detail
            ) {

                // -------------------------------------------------
                // SIC-17
                // -------------------------------------------------

                is SicFormDetail.Sic17 -> {

                    "PUENTE"
                }


                // -------------------------------------------------
                // SIC-18
                // -------------------------------------------------

                is SicFormDetail.Sic18 -> {

                    "ALCANTARILLA"
                }


                // -------------------------------------------------
                // SIC-19
                // -------------------------------------------------

                is SicFormDetail.Sic19 -> {

                    when (
                        detail.state.classCode
                    ) {

                        "08" ->
                            "CUNETA"

                        "09" ->
                            "CANAL"

                        "10" ->
                            "BAJADA_AGUA"

                        "11" ->
                            "ZANJA_DRENAJE"

                        "12" ->
                            "ZANJA_CORONACION"

                        "13" ->
                            "CUNETA_BANQUETA"

                        else ->
                            "DRENAJE"
                    }
                }


                // -------------------------------------------------
                // SIC-20
                // -------------------------------------------------

                is SicFormDetail.Sic20 -> {

                    when (
                        detail.state.classCode
                    ) {

                        "12" ->
                            "BADEN"

                        "13" ->
                            "TUNEL"

                        "14" ->
                            "MURO"

                        else ->
                            "SIC20"
                    }
                }


                // -------------------------------------------------
                // SIC-21
                // -------------------------------------------------

                is SicFormDetail.Sic21 -> {

                    when (
                        detail.state.classCode
                    ) {

                        "18" ->
                            "MARCA_HORIZONTAL"

                        "19" -> {

                            when (
                                detail.state.typeCode
                            ) {

                                "1" ->
                                    "GUARDAVIAS"

                                "2" ->
                                    "POSTE_DELINEADOR"

                                "3" ->
                                    "BARRERA_CONTENCION"

                                "4" ->
                                    "RESALTO"

                                else ->
                                    "SEGURIDAD"
                            }
                        }

                        "20" ->
                            "TACHA"

                        else ->
                            "SIC21"
                    }
                }


                // -------------------------------------------------
                // SIC-22
                // -------------------------------------------------

                is SicFormDetail.Sic23 -> detail.state.assetName

                is SicFormDetail.Sic22 -> {

                    when (
                        detail.state.typeCode
                    ) {

                        "1" ->
                            "REGLAMENTARIA"

                        "2" ->
                            "PREVENTIVA"

                        "3" ->
                            "INFORMATIVA"

                        "4" ->
                            "POSTE_KILOMETRICO"

                        "5" ->
                            "SEMAFORO"

                        "6" ->
                            "POSTE_SOS"

                        else ->
                            "SENAL_VERTICAL"
                    }
                }
            }


        // -----------------------------------------------------
        // CONSTRUIR NOMBRE
        // -----------------------------------------------------

        return buildString {

            append(
                route
            )


            append(
                "_"
            )


            append(
                elementName
            )


            // Para elementos que utilizan lado,
            // agregamos D / I / S.

            if (
                side != null
            ) {

                append(
                    "_"
                )

                append(
                    side
                )
            }


            append(
                "_"
            )


            append(
                progressive
            )


            // Por ahora cada registro tiene una foto.
            // Lo dejamos preparado para N fotos.

            append(
                " ("
            )

            append(
                photoIndex
            )

            append(
                ")"
            )


            append(
                ".jpg"
            )
        }
    }


    // =========================================================
    // GENERAR PROGRESIVA PARA EL NOMBRE
    // =========================================================

    private fun buildProgressive(
        startPrCode: String,
        startDistanceM: String
    ): String {

        val pr =
            startPrCode
                .trim()
                .toIntOrNull()
                ?.toString()
                ?: startPrCode
                    .trim()


        val rawDistance =
            startDistanceM
                .trim()
                .replace(
                    ",",
                    "."
                )


        val numericDistance =
            rawDistance
                .toDoubleOrNull()


        val distance =
            if (
                numericDistance == null
            ) {

                rawDistance

            } else if (
                numericDistance % 1.0 ==
                0.0
            ) {

                numericDistance
                    .toInt()
                    .toString()
                    .padStart(
                        3,
                        '0'
                    )

            } else {

                // Ejemplo:
                // 5 + 125.50
                //
                // Se conserva la parte decimal.

                val integerPart =
                    numericDistance
                        .toInt()
                        .toString()
                        .padStart(
                            3,
                            '0'
                        )


                val decimalPart =
                    rawDistance
                        .substringAfter(
                            ".",
                            ""
                        )
                        .trimEnd(
                            '0'
                        )


                if (
                    decimalPart.isBlank()
                ) {

                    integerPart

                } else {

                    "$integerPart.$decimalPart"
                }
            }


        return "$pr+$distance"
    }


    // =========================================================
    // LIMPIAR TEXTO PARA NOMBRES DE ARCHIVO
    // =========================================================

    private fun cleanNamePart(
        value: String
    ): String {

        return value
            .trim()
            .uppercase()
            .replace(
                "Á",
                "A"
            )
            .replace(
                "É",
                "E"
            )
            .replace(
                "Í",
                "I"
            )
            .replace(
                "Ó",
                "O"
            )
            .replace(
                "Ú",
                "U"
            )
            .replace(
                "Ü",
                "U"
            )
            .replace(
                "Ñ",
                "N"
            )
            .replace(
                " ",
                "_"
            )
            .replace(
                "/",
                "_"
            )
            .replace(
                "\\",
                "_"
            )
    }


    // =========================================================
    // NORMALIZAR PR OBLIGATORIO
    // =========================================================

    private fun normalizeRequiredPr(
        value: String
    ): String {

        val clean =
            value.trim()


        if (
            clean.isEmpty()
        ) {

            throw IllegalArgumentException(
                "El PR es obligatorio."
            )
        }


        if (
            !clean.all {
                it.isDigit()
            }
        ) {

            throw IllegalArgumentException(
                "El PR debe contener solamente números."
            )
        }


        if (
            clean.length > 4
        ) {

            throw IllegalArgumentException(
                "El PR debe tener máximo 4 dígitos."
            )
        }


        return clean.padStart(
            4,
            '0'
        )
    }


    // =========================================================
    // NORMALIZAR PR OPCIONAL
    // =========================================================

    private fun normalizeOptionalPr(
        value: String?
    ): String? {

        if (
            value.isNullOrBlank()
        ) {

            return null
        }


        return normalizeRequiredPr(
            value
        )
    }


    // =========================================================
    // DOUBLE OBLIGATORIO
    // =========================================================

    private fun requiredDouble(
        value: String,
        fieldName: String
    ): Double {

        return optionalDouble(
            value =
                value,

            fieldName =
                fieldName
        )
            ?: throw IllegalArgumentException(
                "$fieldName es obligatorio."
            )
    }


    // =========================================================
    // DOUBLE OPCIONAL
    // =========================================================

    private fun optionalDouble(
        value: String?,
        fieldName: String
    ): Double? {

        if (
            value.isNullOrBlank()
        ) {

            return null
        }


        return value
            .trim()
            .replace(
                ",",
                "."
            )
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?: throw IllegalArgumentException(
                "$fieldName debe ser un número válido."
            )
    }


    // =========================================================
    // INTEGER OPCIONAL
    // =========================================================

    private fun optionalInt(
        value: String?,
        fieldName: String
    ): Int? {

        if (
            value.isNullOrBlank()
        ) {

            return null
        }


        return value
            .trim()
            .toIntOrNull()
            ?: throw IllegalArgumentException(
                "$fieldName debe ser un número entero."
            )
    }
}
