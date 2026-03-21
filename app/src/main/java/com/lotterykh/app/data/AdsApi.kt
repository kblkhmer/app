package com.lotterykh.app.data

import com.lotterykh.app.model.Ad
import com.lotterykh.app.model.AdsResponse
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AdsApi {

    private const val ADS_URL = "http://localhost:8080/ads"

    fun fetchAds(): AdsResponse {
        val url = URL(ADS_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        try {
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            return parseAds(response)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseAds(json: String): AdsResponse {
        val root = JSONObject(json)
        val total = root.getInt("total")
        val adsArr = root.getJSONArray("ads")

        val ads = mutableListOf<Ad>()
        for (i in 0 until adsArr.length()) {
            val obj = adsArr.getJSONObject(i)
            ads.add(
                Ad(
                    id = obj.getInt("id"),
                    title = obj.optString("title", ""),
                    imageUrl = obj.getString("image_url"),
                    linkUrl = obj.optString("link_url", ""),
                    position = obj.optString("position", "popup"),
                    duration = obj.optInt("duration", 10),
                    displayOrder = obj.optInt("display_order", 0)
                )
            )
        }

        return AdsResponse(total = total, ads = ads)
    }
}
