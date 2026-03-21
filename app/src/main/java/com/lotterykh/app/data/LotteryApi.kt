package com.lotterykh.app.data

import com.lotterykh.app.model.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LotteryApi {

    private const val API_URL = "https://lottery.yournews.asia/admin/api/result.php"

    fun fetchResults(): ApiResponse {
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

    private fun parseResponse(json: String): ApiResponse {
        val root = JSONObject(json)
        val date = root.getString("date")
        val lotteriesArr = root.getJSONArray("lotteries")

        val lotteries = mutableListOf<ApiLottery>()
        for (i in 0 until lotteriesArr.length()) {
            val lObj = lotteriesArr.getJSONObject(i)
            val resultsArr = lObj.getJSONArray("results")

            val timeSlots = mutableListOf<ApiTimeSlot>()
            for (j in 0 until resultsArr.length()) {
                val tsObj = resultsArr.getJSONObject(j)
                val groupsArr = tsObj.getJSONArray("groups")

                val groups = mutableListOf<ApiGroup>()
                for (k in 0 until groupsArr.length()) {
                    val gObj = groupsArr.getJSONObject(k)
                    groups.add(
                        ApiGroup(
                            group = gObj.getString("group"),
                            result2d = gObj.optString("result_2d", ""),
                            result3d = if (gObj.isNull("result_3d")) null else gObj.optString("result_3d", ""),
                            status = gObj.optString("status", ""),
                            resultLo = if (gObj.isNull("result_lo")) null else gObj.optString("result_lo", "")
                        )
                    )
                }

                timeSlots.add(
                    ApiTimeSlot(
                        timeSlot = tsObj.getString("time_slot"),
                        groups = groups
                    )
                )
            }

            lotteries.add(
                ApiLottery(
                    lottery = lObj.getString("lottery"),
                    name = lObj.getString("name"),
                    results = timeSlots
                )
            )
        }

        return ApiResponse(date = date, lotteries = lotteries)
    }

    fun convertToResults(apiResponse: ApiResponse): List<LotteryResult> {
        return apiResponse.lotteries
            .filter { it.results.isNotEmpty() }
            .map { lottery ->
                val draws = lottery.results.map { timeSlot ->
                    val mainGroups = timeSlot.groups.filter { g ->
                        g.group.length <= 2 && !g.group.startsWith("LO")
                    }
                    val prizes = mainGroups.map { g ->
                        Prize(
                            label = g.group,
                            twoDigit = g.result2d,
                            threeDigit = g.result3d ?: ""
                        )
                    }

                    LotteryDraw(
                        timeSlot = timeSlot.timeSlot,
                        date = apiResponse.date,
                        prizes = prizes
                    )
                }

                LotteryResult(
                    lotteryCode = lottery.lottery,
                    title = lottery.name,
                    draws = draws
                )
            }
    }
}
