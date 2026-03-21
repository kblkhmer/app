package com.lotterykh.app.model

/**
 * Represents a single lottery draw session (morning or evening).
 */
data class LotteryDraw(
    val session: Session,
    val date: String,
    val prizes: List<Prize>,
    val specialNumbers: SpecialNumbers? = null
)

enum class Session(val khmer: String) {
    MORNING("?????"),
    EVENING("?????")
}

/**
 * Standard prize: ?????? A/B/C/D with a 2-digit and 3-digit number.
 */
data class Prize(
    val label: String,       // e.g. "?????? A"
    val twoDigit: String,    // e.g. "43"
    val threeDigit: String   // e.g. "856"
)

/**
 * Special row for ?????? A with 3 three-digit numbers,
 * and ??????? A with 4 two-digit numbers.
 */
data class SpecialNumbers(
    val prizeAValues: List<String>,    // 3 three-digit numbers
    val twoDigitValues: List<String>   // 4 two-digit numbers  (??????? A)
)

/**
 * A complete lottery result page containing a title and draws.
 */
data class LotteryResult(
    val title: String,
    val draws: List<LotteryDraw>
)
