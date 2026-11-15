package com.edgeglow.aod.updater

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String = "",
    val releaseDate: String = "",
    val mandatory: Boolean = false
)
