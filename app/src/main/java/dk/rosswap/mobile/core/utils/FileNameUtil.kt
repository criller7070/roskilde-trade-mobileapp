package dk.rosswap.mobile.core.utils

// simple util for sanitizing filenames, currently just used for /items. There wasn't
// really a strict reason to separate this away, but if we ever want to add proper
// validation this can be repurposed. Plus it's pure util so might as well

object FileNameUtil {
    fun sanitizeFilename(original: String): String {
        val trimmed = original.trim().take(120)
        val replaced = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val fallback = "image"
        val safe = replaced.trim('-').trim().takeIf { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
            ?: fallback
        return safe
    }
}
