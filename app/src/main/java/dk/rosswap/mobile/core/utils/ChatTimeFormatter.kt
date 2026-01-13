package dk.rosswap.mobile.core.utils

import android.content.Context
import dk.rosswap.mobile.R
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object ChatTimeFormatter {

    fun formatRelativeSeconds(context: Context, seconds: Long?): String {
        if (seconds == null) return ""

        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val diffSeconds = (nowSeconds - seconds).coerceAtLeast(0)

        if (diffSeconds < 60) {
            val safe = diffSeconds.coerceAtLeast(1)
            return context.resources.getQuantityString(R.plurals.chat_time_seconds_ago, safe.toInt(), safe.toInt())
        }

        val minutes = TimeUnit.SECONDS.toMinutes(diffSeconds)
        if (minutes < 60) {
            val safe = minutes.coerceAtLeast(1)
            return context.resources.getQuantityString(R.plurals.chat_time_minutes_ago, safe.toInt(), safe.toInt())
        }

        val hours = TimeUnit.SECONDS.toHours(diffSeconds)
        if (hours < 24) {
            val safe = hours.coerceAtLeast(1)
            return context.resources.getQuantityString(R.plurals.chat_time_hours_ago, safe.toInt(), safe.toInt())
        }

        val days = TimeUnit.SECONDS.toDays(diffSeconds)
        if (days < 30) {
            val safe = days.coerceAtLeast(1)
            return context.resources.getQuantityString(R.plurals.chat_time_days_ago, safe.toInt(), safe.toInt())
        }

        // Use java.time API for more precise month/year calculation
        val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val then = Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).toLocalDate()
        
        val months = ChronoUnit.MONTHS.between(then, now).toInt()
        if (months < 12) {
            val safe = months.coerceAtLeast(1)
            return context.resources.getQuantityString(R.plurals.chat_time_months_ago, safe, safe)
        }

        val years = ChronoUnit.YEARS.between(then, now).toInt().coerceAtLeast(1)
        return context.resources.getQuantityString(R.plurals.chat_time_years_ago, years, years)
    }
}
