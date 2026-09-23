package com.tuempresa.inventariovial.viewmodel

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

import com.tuempresa.inventariovial.data.database.InventoryDatabase

import com.tuempresa.inventariovial.data.entity.InventoryRecordEntity
import com.tuempresa.inventariovial.data.entity.PhotoEntity
import com.tuempresa.inventariovial.data.entity.Sic17Entity
import com.tuempresa.inventariovial.data.entity.Sic18Entity
import com.tuempresa.inventariovial.data.entity.Sic19Entity
import com.tuempresa.inventariovial.data.entity.Sic20Entity
import com.tuempresa.inventariovial.data.entity.Sic21Entity
import com.tuempresa.inventariovial.data.entity.Sic22Entity

import com.tuempresa.inventariovial.model.form.InventorySaveRequest
import com.tuempresa.inventariovial.model.form.SicFormDetail

import com.tuempresa.inventariovial.repository.InventoryRepository

import com.tuempresa.inventariovial.scheduleDriveUpload

import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


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


    // =========================================================
    // GUARDAR REGISTRO
    // =========================================================

    fun saveRecord(
        request: InventorySaveRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                // -------------------------------------------------
                // IDENTIFICADORES ÚNICOS
                // -------------------------------------------------

                val recordId =
                    UUID
                        .randomUUID()
                        .toString()


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


                        // Todavía no estamos recibiendo
                        // altitud desde el formulario.
                        altitudeM =
                            null,


                        gpsAccuracyM =
                            request
                                .gpsAccuracyM
                                ?.toDouble(),


                        surveyDate =
                            request.surveyDate,


                        observations =
                            request
                                .observations
                                .trim()
                                .ifBlank {
                                    null
                                },


                        status =
                            "ACTIVE",


                        photoSyncStatus =
                            "PENDING",


                        excelSyncStatus =
                            "PENDING",


                        createdAt =
                            now,


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


                            photo =
                                photo
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
                                                state.dimension2M,

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


                            photo =
                                photo
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


                            photo =
                                photo
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


                            photo =
                                photo
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


                            photo =
                                photo
                        )
                    }


                    // =================================================
                    // SIC-22
                    // =================================================

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


                            photo =
                                photo
                        )
                    }
                }


                // =================================================
                // ROOM YA SE GUARDÓ CORRECTAMENTE.
                //
                // SOLO DESPUÉS PROGRAMAMOS LA SUBIDA A DRIVE.
                // =================================================

                scheduleDriveUpload(

                    context =
                        getApplication(),


                    photoPath =
                        request.photoPath,


                    driveFileName =
                        driveFileName
                )


                // -------------------------------------------------
                // AVISAR A LA INTERFAZ
                // -------------------------------------------------

                onSuccess()


            } catch (
                error: Exception
            ) {

                onError(
                    error.message
                        ?: "No se pudo guardar el registro."
                )
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