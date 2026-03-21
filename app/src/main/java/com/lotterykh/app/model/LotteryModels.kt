package com.lotterykh.app.model

data class LotteryDraw(
    val timeSlot: String,
    val date: String,
    val prizes: List<Prize>
)

data class Prize(
    val label: String,
    val twoDigit: String,
    val threeDigit: String
)

data class LotteryResult(
    val lotteryCode: String,
    val title: String,
    val draws: List<LotteryDraw>
)

// API response models
data class ApiResponse(
    val date: String,
    val lotteries: List<ApiLottery>
)

data class ApiLottery(
    val lottery: String,
    val name: String,
    val results: List<ApiTimeSlot>
)

data class ApiTimeSlot(
    val timeSlot: String,
    val groups: List<ApiGroup>
)

data class ApiGroup(
    val group: String,
    val result2d: String,
    val result3d: String?,
    val status: String,
    val resultLo: String? = null
)

// Ads models
data class AdsResponse(
    val total: Int,
    val ads: List<Ad>
)

data class Ad(
    val id: Int,
    val title: String,
    val imageUrl: String,
    val linkUrl: String,
    val position: String,
    val duration: Int,
    val displayOrder: Int
)
