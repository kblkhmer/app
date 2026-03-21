package com.lotterykh.app.data

import com.lotterykh.app.model.*

/**
 * Provides sample lottery data matching the app screenshots.
 */
object SampleData {

    fun getVietnamResults(): LotteryResult {
        return LotteryResult(
            title = "??????????? ??????",
            draws = listOf(
                LotteryDraw(
                    session = Session.MORNING,
                    date = "18.11.2018",
                    prizes = listOf(
                        Prize("?????? A", "43", "856"),
                        Prize("?????? B", "12", "653"),
                        Prize("?????? C", "53", "126"),
                        Prize("?????? D", "13", "265")
                    )
                ),
                LotteryDraw(
                    session = Session.EVENING,
                    date = "18.11.2018",
                    prizes = listOf(
                        Prize("?????? A", "", ""),
                        Prize("?????? B", "", ""),
                        Prize("?????? C", "", ""),
                        Prize("?????? D", "", "")
                    ),
                    specialNumbers = SpecialNumbers(
                        prizeAValues = listOf("636", "486", "856"),
                        twoDigitValues = listOf("70", "54", "05", "77")
                    )
                )
            )
        )
    }

    fun getMalaysiaResults(): LotteryResult {
        return LotteryResult(
            title = "??????????? ????????",
            draws = listOf(
                LotteryDraw(
                    session = Session.MORNING,
                    date = "21.08.2018",
                    prizes = listOf(
                        Prize("?????? A", "13", "234"),
                        Prize("?????? B", "56", "567"),
                        Prize("?????? C", "44", "884"),
                        Prize("?????? D", "67", "789")
                    )
                ),
                LotteryDraw(
                    session = Session.EVENING,
                    date = "21.08.2018",
                    prizes = listOf(
                        Prize("?????? B", "24", "123"),
                        Prize("?????? C", "22", "222"),
                        Prize("?????? D", "33", "333")
                    ),
                    specialNumbers = SpecialNumbers(
                        prizeAValues = listOf("777", "888", "999"),
                        twoDigitValues = listOf("11", "22", "33", "44")
                    )
                )
            )
        )
    }

    fun getAllResults(): List<LotteryResult> = listOf(
        getVietnamResults(),
        getMalaysiaResults()
    )
}
