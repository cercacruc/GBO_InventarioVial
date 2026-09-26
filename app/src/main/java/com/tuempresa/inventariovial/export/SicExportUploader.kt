package com.tuempresa.inventariovial.export


import android.util.Base64

import com.tuempresa.inventariovial.DriveConfig

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType

import org.json.JSONObject

import java.io.File
import java.io.IOException

import okhttp3.Response

object SicExportUploader {


    fun upload(
        file: File,
        callback: (Boolean, String?) -> Unit
    ) {


        try {


            val bytes =
                file.readBytes()


            val base64 =
                Base64.encodeToString(
                    bytes,
                    Base64.NO_WRAP
                )


            val json =
                JSONObject().apply {


                    put(
                        "token",
                        DriveConfig.API_TOKEN
                    )


                    put(
                        "action",
                        "uploadSicExport"
                    )


                    put(
                        "fileName",
                        file.name
                    )


                    put(
                        "base64",
                        base64
                    )

                }



            val body =
                json
                    .toString()
                    .toRequestBody(
                        "application/json"
                            .toMediaType()
                    )


            val request =
                Request.Builder()

                    .url(
                        DriveConfig.WEB_APP_URL
                    )

                    .post(body)

                    .build()



            OkHttpClient()
                .newCall(request)
                .enqueue(

                    object: Callback {


                        override fun onFailure(
                            call: Call,
                            e: java.io.IOException
                        ) {

                            callback(
                                false,
                                e.message
                            )
                        }



                        override fun onResponse(
                            call: Call,
                            response: Response
                        ) {


                            val text =
                                response.body
                                    ?.string()


                            val result =
                                JSONObject(
                                    text ?: "{}"
                                )


                            callback(
                                result.optBoolean(
                                    "ok"
                                ),

                                result.optString(
                                    "fileUrl"
                                )
                            )
                        }
                    }
                )



        } catch(e: Exception){

            callback(
                false,
                e.message
            )

        }

    }

}