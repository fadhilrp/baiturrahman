package com.example.baiturrahman.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    fun formatDateForApi(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
    }

    fun formatDateForDisplay(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id")))
    }
}

