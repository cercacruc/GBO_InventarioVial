package com.tuempresa.inventariovial

import android.content.Context
import android.util.Base64
import android.util.Log

import androidx.work.Worker
import androidx.work.WorkerParameters

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


    override fun doWork(): Result {

        Log.d(
            TAG,
            "=============================="
        )

        Log.d(
            TAG,
            "DriveUploadWorker iniciado"
        )


        // -----------------------------------------
        // OBTENER RUTA DE LA FOTO
        // -----------------------------------------

        val photoPath =
            inputData.getString(
                "photoPath"
            )


        if (photoPath == null) {

            Log.e(
                TAG,
                "ERROR: WorkManager no recibió photoPath"
            )

            return Result.failure()
        }


        Log.d(
            TAG,
            "Ruta recibida: $photoPath"
        )


        // -----------------------------------------
        // COMPROBAR ARCHIVO
        // -----------------------------------------

        val file =
            File(photoPath)


        if (!file.exists()) {

            Log.e(
                TAG,
                "ERROR: La fotografía no existe en: $photoPath"
            )

            return Result.failure()
        }


        Log.d(
            TAG,
            "Archivo encontrado: ${file.name}"
        )

        Log.d(
            TAG,
            "Tamaño: ${file.length()} bytes"
        )


        // -----------------------------------------
        // SUBIR
        // -----------------------------------------

        return try {

            uploadFile(file)

        } catch (error: Exception) {

            Log.e(
                TAG,
                "EXCEPCIÓN durante la sincronización",
                error
            )

            Result.retry()
        }
    }


    private fun uploadFile(
        file: File
    ): Result {

        Log.d(
            TAG,
            "Preparando imagen..."
        )


        val bytes =
            file.readBytes()


        Log.d(
            TAG,
            "Imagen leída correctamente"
        )


        val base64 =
            Base64.encodeToString(
                bytes,
                Base64.NO_WRAP
            )


        Log.d(
            TAG,
            "Imagen convertida a Base64"
        )


        // -----------------------------------------
        // CREAR JSON
        // -----------------------------------------

        val json =
            JSONObject().apply {

                put(
                    "token",
                    DriveConfig.API_TOKEN
                )

                put(
                    "fileName",
                    file.name
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


        // -----------------------------------------
        // URL
        // -----------------------------------------

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

            return Result.failure()
        }


        // -----------------------------------------
        // BODY
        // -----------------------------------------

        val requestBody =
            json
                .toString()
                .toRequestBody(
                    "application/json; charset=utf-8"
                        .toMediaType()
                )


        val request =
            Request.Builder()
                .url(
                    DriveConfig.WEB_APP_URL
                )
                .post(
                    requestBody
                )
                .build()


        // -----------------------------------------
        // CLIENTE HTTP
        // -----------------------------------------

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
                .followRedirects(true)
                .followSslRedirects(true)
                .build()


        Log.d(
            TAG,
            "Enviando fotografía a Apps Script..."
        )


        // -----------------------------------------
        // PETICIÓN
        // -----------------------------------------

        client
            .newCall(request)
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


                // ---------------------------------
                // ERROR HTTP
                // ---------------------------------

                if (!response.isSuccessful) {

                    Log.e(
                        TAG,
                        "ERROR HTTP ${response.code}"
                    )

                    return Result.retry()
                }


                // ---------------------------------
                // RESPUESTA VACÍA
                // ---------------------------------

                if (responseText.isBlank()) {

                    Log.e(
                        TAG,
                        "ERROR: Apps Script respondió vacío"
                    )

                    return Result.retry()
                }


                // ---------------------------------
                // INTERPRETAR JSON
                // ---------------------------------

                val jsonResponse =
                    try {

                        JSONObject(
                            responseText
                        )

                    } catch (error: Exception) {

                        Log.e(
                            TAG,
                            "ERROR: La respuesta no es JSON válido",
                            error
                        )

                        return Result.retry()
                    }


                val success =
                    jsonResponse
                        .optBoolean(
                            "ok",
                            false
                        )


                // ---------------------------------
                // ÉXITO
                // ---------------------------------

                if (success) {

                    val alreadyExists =
                        jsonResponse
                            .optBoolean(
                                "alreadyExists",
                                false
                            )


                    if (alreadyExists) {

                        Log.d(
                            TAG,
                            "ARCHIVO YA EXISTÍA EN DRIVE: ${file.name}"
                        )

                    } else {

                        Log.d(
                            TAG,
                            "SUBIDA EXITOSA A DRIVE: ${file.name}"
                        )
                    }


                    Log.d(
                        TAG,
                        "=============================="
                    )


                    return Result.success()
                }


                // ---------------------------------
                // ERROR DEVUELTO POR APPS SCRIPT
                // ---------------------------------

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

                    return Result.failure()
                }


                return Result.retry()
            }
    }
}