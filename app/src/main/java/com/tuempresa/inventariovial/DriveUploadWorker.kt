package com.tuempresa.inventariovial

import android.content.Context
import android.util.Base64
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters

import com.tuempresa.inventariovial.data.database.InventoryDatabase

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import org.json.JSONObject

import java.io.File
import java.util.concurrent.TimeUnit


class DriveUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(
    context,
    workerParams
) {

    companion object {

        const val TAG = "DriveSync"
    }


    // =========================================================
    // BASE DE DATOS
    // =========================================================

    private val inventoryDao =
        InventoryDatabase
            .getInstance(
                applicationContext
            )
            .inventoryDao()


    // =========================================================
    // TRABAJO PRINCIPAL
    // =========================================================

    override fun doWork(): Result {
        val photoId = inputData.getString("photoId")
        val photo = photoId?.let { inventoryDao.photoById(it) }
            ?: inputData.getString("photoPath")?.let { inventoryDao.photoByPath(it) }
        if (photoId != null && photo == null) return Result.failure()
        if (photo?.syncStatus == "SYNCED") return Result.success()
        if (photo != null && inventoryDao.recordStatus(photo.recordId) != "ACTIVE") return Result.success()
        if (DriveConfig.API_TOKEN.isBlank()) return Result.failure()


        Log.d(
            TAG,
            "=============================="
        )

        Log.d(
            TAG,
            "DriveUploadWorker iniciado"
        )


        // -----------------------------------------------------
        // 1. OBTENER DATOS DESDE WORKMANAGER
        // -----------------------------------------------------

        val photoPath =
            inputData.getString(
                "photoPath"
            )


        val driveFileName =
            inputData.getString(
                "driveFileName"
            )


        // -----------------------------------------------------
        // 2. VALIDAR RUTA LOCAL
        // -----------------------------------------------------

        if (
            photoPath.isNullOrBlank()
        ) {

            Log.e(
                TAG,
                "ERROR: WorkManager no recibió photoPath"
            )

            return Result.failure()
        }


        // -----------------------------------------------------
        // 3. VALIDAR NOMBRE DEFINITIVO DE DRIVE
        // -----------------------------------------------------

        if (
            driveFileName.isNullOrBlank()
        ) {

            Log.e(
                TAG,
                "ERROR: WorkManager no recibió driveFileName"
            )


            inventoryDao
                .updatePhotoSyncStatusByPath(
                    photoPath = photoPath,
                    status = "ERROR"
                )


            return Result.failure()
        }


        Log.d(
            TAG,
            "Ruta local recibida: $photoPath"
        )


        Log.d(
            TAG,
            "Nombre para Drive: $driveFileName"
        )


        // -----------------------------------------------------
        // 4. MARCAR COMO UPLOADING
        // -----------------------------------------------------

        inventoryDao
            .updatePhotoSyncStatusByPath(
                photoPath = photoPath,
                status = "UPLOADING"
            )


        // -----------------------------------------------------
        // 5. COMPROBAR ARCHIVO LOCAL
        // -----------------------------------------------------

        val file =
            File(
                photoPath
            )


        if (
            !file.exists()
        ) {

            Log.e(
                TAG,
                "ERROR: La fotografía no existe en: $photoPath"
            )


            inventoryDao
                .updatePhotoSyncStatusByPath(
                    photoPath = photoPath,
                    status = "ERROR"
                )


            return Result.failure()
        }


        Log.d(
            TAG,
            "Archivo local encontrado: ${file.name}"
        )


        Log.d(
            TAG,
            "Nombre definitivo: $driveFileName"
        )


        Log.d(
            TAG,
            "Tamaño: ${file.length()} bytes"
        )


        // -----------------------------------------------------
        // 6. INTENTAR SUBIDA
        // -----------------------------------------------------

        return try {

            uploadFile(
                file = file,
                photoPath = photoPath,
                driveFileName = driveFileName
            )

        } catch (
            error: Exception
        ) {

            Log.e(
                TAG,
                "EXCEPCIÓN durante la sincronización",
                error
            )


            // Error posiblemente temporal.
            // WorkManager podrá reintentar.

            inventoryDao
                .updatePhotoSyncStatusByPath(
                    photoPath = photoPath,
                    status = "PENDING"
                )


            Result.retry()
        }
    }


    // =========================================================
    // SUBIDA A DRIVE
    // =========================================================

    private fun uploadFile(
        file: File,
        photoPath: String,
        driveFileName: String
    ): Result {

        Log.d(
            TAG,
            "Preparando imagen..."
        )


        // -----------------------------------------------------
        // LEER ARCHIVO
        // -----------------------------------------------------

        val bytes =
            file.readBytes()


        Log.d(
            TAG,
            "Imagen leída correctamente"
        )


        // -----------------------------------------------------
        // CONVERTIR A BASE64
        // -----------------------------------------------------

        val base64 =
            Base64.encodeToString(
                bytes,
                Base64.NO_WRAP
            )


        Log.d(
            TAG,
            "Imagen convertida a Base64"
        )


        // -----------------------------------------------------
        // CREAR JSON PARA APPS SCRIPT
        // -----------------------------------------------------

        val json =
            JSONObject().apply {

                put(
                    "token",
                    DriveConfig.API_TOKEN
                )


                // IMPORTANTE:
                // NO enviamos file.name.
                //
                // file.name es el nombre temporal local.
                //
                // driveFileName es el nombre oficial
                // que queremos ver en Google Drive.

                put(
                    "fileName",
                    driveFileName
                )


                put(
                    "mimeType",
                    "image/jpeg"
                )


                put(
                    "base64",
                    base64
                )
            }


        Log.d(
            TAG,
            "Archivo local: ${file.name}"
        )


        Log.d(
            TAG,
            "Archivo enviado como: $driveFileName"
        )


        // -----------------------------------------------------
        // VALIDAR URL
        // -----------------------------------------------------

        Log.d(
            TAG,
            "URL configurada: ${DriveConfig.WEB_APP_URL}"
        )


        if (
            DriveConfig.WEB_APP_URL.isBlank() ||
            DriveConfig.WEB_APP_URL.contains(
                "PEGA_AQUI",
                ignoreCase = true
            )
        ) {

            Log.e(
                TAG,
                "ERROR: WEB_APP_URL no está configurada"
            )


            inventoryDao
                .updatePhotoSyncStatusByPath(
                    photoPath = photoPath,
                    status = "ERROR"
                )


            return Result.failure()
        }


        // -----------------------------------------------------
        // CREAR BODY HTTP
        // -----------------------------------------------------

        val requestBody =
            json
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaType()
                )


        // -----------------------------------------------------
        // CREAR REQUEST
        // -----------------------------------------------------

        val request =
            Request.Builder()
                .url(
                    DriveConfig.WEB_APP_URL
                )
                .post(
                    requestBody
                )
                .build()


        // -----------------------------------------------------
        // CLIENTE HTTP
        // -----------------------------------------------------

        val client =
            OkHttpClient.Builder()
                .connectTimeout(
                    30,
                    TimeUnit.SECONDS
                )
                .readTimeout(
                    120,
                    TimeUnit.SECONDS
                )
                .writeTimeout(
                    120,
                    TimeUnit.SECONDS
                )
                .followRedirects(
                    true
                )
                .followSslRedirects(
                    true
                )
                .build()


        Log.d(
            TAG,
            "Enviando fotografía a Apps Script..."
        )


        // -----------------------------------------------------
        // EJECUTAR PETICIÓN
        // -----------------------------------------------------

        client
            .newCall(
                request
            )
            .execute()
            .use { response ->


                Log.d(
                    TAG,
                    "Código HTTP recibido: ${response.code}"
                )


                val responseText =
                    response
                        .body
                        ?.string()
                        ?: ""


                Log.d(
                    TAG,
                    "Respuesta del servidor: $responseText"
                )


                // -------------------------------------------------
                // ERROR HTTP
                // -------------------------------------------------

                if (
                    !response.isSuccessful
                ) {

                    Log.e(
                        TAG,
                        "ERROR HTTP ${response.code}"
                    )


                    inventoryDao
                        .updatePhotoSyncStatusByPath(
                            photoPath = photoPath,
                            status = "PENDING"
                        )


                    return Result.retry()
                }


                // -------------------------------------------------
                // RESPUESTA VACÍA
                // -------------------------------------------------

                if (
                    responseText.isBlank()
                ) {

                    Log.e(
                        TAG,
                        "ERROR: Apps Script respondió vacío"
                    )


                    inventoryDao
                        .updatePhotoSyncStatusByPath(
                            photoPath = photoPath,
                            status = "PENDING"
                        )


                    return Result.retry()
                }


                // -------------------------------------------------
                // INTERPRETAR JSON
                // -------------------------------------------------

                val jsonResponse =
                    try {

                        JSONObject(
                            responseText
                        )

                    } catch (
                        error: Exception
                    ) {

                        Log.e(
                            TAG,
                            "ERROR: La respuesta no es JSON válido",
                            error
                        )


                        inventoryDao
                            .updatePhotoSyncStatusByPath(
                                photoPath = photoPath,
                                status = "PENDING"
                            )


                        return Result.retry()
                    }


                val success =
                    jsonResponse
                        .optBoolean(
                            "ok",
                            false
                        )


                // -------------------------------------------------
                // SUBIDA EXITOSA
                // -------------------------------------------------

                if (
                    success
                ) {

                    val alreadyExists =
                        jsonResponse
                            .optBoolean(
                                "alreadyExists",
                                false
                            )


                    // Apps Script puede devolver el ID
                    // del archivo creado.
                    //
                    // Si actualmente no lo devuelve,
                    // quedará null y no hay problema.

                    val driveFileId =
                        jsonResponse
                            .optString(
                                "fileId",
                                ""
                            )
                            .ifBlank {
                                null
                            }


                    // -------------------------------------------------
                    // ACTUALIZAR ROOM
                    // -------------------------------------------------

                    inventoryDao
                        .completePhotoUpload(
                            photoPath = photoPath,
                            driveFileId = driveFileId
                        )


                    // -------------------------------------------------
                    // LOG
                    // -------------------------------------------------

                    if (
                        alreadyExists
                    ) {

                        Log.d(
                            TAG,
                            "ARCHIVO YA EXISTÍA EN DRIVE: $driveFileName"
                        )

                    } else {

                        Log.d(
                            TAG,
                            "SUBIDA EXITOSA A DRIVE: $driveFileName"
                        )
                    }


                    Log.d(
                        TAG,
                        "Nombre local: ${file.name}"
                    )


                    Log.d(
                        TAG,
                        "Nombre Drive: $driveFileName"
                    )


                    Log.d(
                        TAG,
                        "Estado Room actualizado a SYNCED"
                    )


                    Log.d(
                        TAG,
                        "=============================="
                    )


                    return Result.success()
                }


                // -------------------------------------------------
                // ERROR DEVUELTO POR APPS SCRIPT
                // -------------------------------------------------

                val serverError =
                    jsonResponse
                        .optString(
                            "error",
                            "Error desconocido"
                        )


                Log.e(
                    TAG,
                    "Apps Script devolvió error: $serverError"
                )


                // -------------------------------------------------
                // TOKEN INCORRECTO
                // -------------------------------------------------

                if (
                    serverError.equals(
                        "No autorizado",
                        ignoreCase = true
                    )
                ) {

                    Log.e(
                        TAG,
                        "REVISA EL API_TOKEN DE DriveConfig Y APPS SCRIPT"
                    )


                    inventoryDao
                        .updatePhotoSyncStatusByPath(
                            photoPath = photoPath,
                            status = "ERROR"
                        )


                    return Result.failure()
                }


                // -------------------------------------------------
                // OTRO ERROR DEL SERVIDOR
                // -------------------------------------------------

                inventoryDao
                    .updatePhotoSyncStatusByPath(
                        photoPath = photoPath,
                        status = "PENDING"
                    )


                return Result.retry()
            }
    }
}
