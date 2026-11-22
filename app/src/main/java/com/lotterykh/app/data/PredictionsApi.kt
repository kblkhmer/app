package com.lotterykh.app.data

import com.lotterykh.app.model.PredictionResponse
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object PredictionsApi {

    private const val API_URL = "https://bkhlottery.cc/admin/api/predictions.php"

    fun fetchPredictions(): PredictionResponse {
        val url = URL(API_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        try {
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            return parseResponse(response)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseResponse(json: String): PredictionResponse {
        val root = JSONObject(json)
        val date = root.getString("date")

        val arr2d = root.getJSONArray("numbers_2d")
        val numbers2d = mutableListOf<String>()
        for (i in 0 until arr2d.length()) {
            numbers2d.add(arr2d.getString(i))
        }

        val arr3d = root.getJSONArray("numbers_3d")
        val numbers3d = mutableListOf<String>()
        for (i in 0 until arr3d.length()) {
            numbers3d.add(arr3d.getString(i))
        }

        return PredictionResponse(date = date, numbers2d = numbers2d, numbers3d = numbers3d)
    }
}
