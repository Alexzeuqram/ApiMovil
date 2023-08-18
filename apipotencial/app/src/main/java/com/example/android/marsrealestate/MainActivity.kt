/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.example.android.marsrealestate



import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var edadActualEditText: EditText
    private lateinit var edadFuturaEditText: EditText
    private lateinit var potencialActualEditText: EditText
    private lateinit var predictButton: Button
    private lateinit var predictionsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edadActualEditText = findViewById(R.id.edad_actual)
        edadFuturaEditText = findViewById(R.id.edad_futura)
        potencialActualEditText = findViewById(R.id.potencial_actual)
        predictButton = findViewById(R.id.predictButton)
        predictionsTextView = findViewById(R.id.predictions)

        predictButton.setOnClickListener {
            val edadActual = edadActualEditText.text.toString().toInt()
            val edadFutura = edadFuturaEditText.text.toString().toInt()
            val potencialActual = potencialActualEditText.text.toString().toInt()

            if (edadFutura <= edadActual) {
                predictionsTextView.text = "La edad futura debe ser mayor que la edad actual."
                return@setOnClickListener
            }

            // Utilizar Coroutine para realizar la solicitud de red en un hilo separado
            GlobalScope.launch(Dispatchers.IO) {
                val response = performNetworkRequest(edadActual, edadFutura, potencialActual)
                runOnUiThread {
                    handleNetworkResponse(response)
                }
            }
        }
    }

    private fun performNetworkRequest(edadActual: Int, edadFutura: Int, potencialActual: Int): JSONObject {
        val url = URL("https://apipotencial-aecee55076f9.herokuapp.com/predict")
        val connection = url.openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")

        val data = JSONObject()
        data.put("edad_actual", edadActual)
        data.put("potencial_actual", potencialActual)
        data.put("edad_futura", edadFutura)

        val outputStreamWriter = OutputStreamWriter(connection.outputStream)
        outputStreamWriter.write(data.toString())
        outputStreamWriter.flush()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return parseResponse(connection)
        } else {
            throw Exception("Error en la solicitud")
        }
    }

    private fun parseResponse(connection: HttpURLConnection): JSONObject {
        val response = StringBuilder()
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        return JSONObject(response.toString())
    }

    private fun handleNetworkResponse(response: JSONObject) {
        val potencialEstimado = response.getDouble("prediccion")
        predictionsTextView.text = "Potencial estimado: $potencialEstimado"
    }
}
