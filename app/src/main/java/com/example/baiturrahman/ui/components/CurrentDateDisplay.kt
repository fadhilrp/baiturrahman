package com.example.baiturrahman.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baiturrahman.data.model.PrayerData
import com.example.baiturrahman.ui.theme.emeraldGreen
import com.example.baiturrahman.ui.theme.white
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CurrentDateDisplay(prayerData: PrayerData?) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while(true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    // Format the Hijri date from prayerData
    val hijriDay = prayerData?.date?.hijri?.day ?: ""
    val hijriMonth = prayerData?.date?.hijri?.month?.en ?: ""
    val hijriYear = prayerData?.date?.hijri?.year ?: ""

    // Replace "Shaʿbān" with "Sha'ban" if needed
    val formattedMonth = when (hijriMonth) {
        "Shaʿbān" -> "Sha'ban"
        else -> hijriMonth
    }

    // Build the Hijri date string (e.g., "23 Sha'ban 1446")
    val hijriDateText = if (hijriDay.isNotEmpty() && hijriMonth.isNotEmpty() && hijriYear.isNotEmpty()) {
        "$hijriDay $formattedMonth $hijriYear"
    } else {
        "- - -" // Fallback if data is not available
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(white)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = currentTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id"))),
            fontSize = 25.sp,
            color = emeraldGreen,
        )

        // Add a thin line
        HorizontalDivider(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .width(250.dp), // Adjust width to match design
            thickness = 2.dp,
            color = emeraldGreen
        )

        Text(
            text = hijriDateText,
            fontSize = 25.sp,
            color = emeraldGreen,
        )
    }
}