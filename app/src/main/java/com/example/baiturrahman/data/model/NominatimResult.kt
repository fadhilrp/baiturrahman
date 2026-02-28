package com.example.baiturrahman.data.model

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    @SerializedName("display_name") val displayName: String,
    val lat: String,
    val lon: String
)
