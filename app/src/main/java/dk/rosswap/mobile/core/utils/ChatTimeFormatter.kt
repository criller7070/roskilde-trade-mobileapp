package dk.rosswap.mobile.core.utils

import android.content.Context
import dk.rosswap.mobile.R
import java.util.concurrent.TimeUnit

object ChatTimeFormatter {

    fun formatRelativeSeconds(context: Context, seconds: Long?): String {
        if (seconds == null) return ""

        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        val diffSeconds = (nowSeconds - seconds).coerceAtLeast(0)

        val minutes = TimeUnit.SECONDS.toMinutes(diffSeconds)
        if (minutes < 60) {
            val safeMinutes = minutes.coerceAtLeast(1)
            return context.getString(R.string.chat_time_minutes_ago, safeMinutes.toInt())
        }

        val hours = TimeUnit.SECONDS.toHours(diffSeconds)
        val safeHours = hours.coerceAtLeast(1)
        return context.getString(R.string.chat_time_hours_ago, safeHours.toInt())
    }
}

