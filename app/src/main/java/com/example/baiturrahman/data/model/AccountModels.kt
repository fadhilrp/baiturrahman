package com.example.baiturrahman.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSession(
    @SerialName("id") val id: String,
    @SerialName("device_label") val deviceLabel: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("is_current") val isCurrent: Boolean
)
