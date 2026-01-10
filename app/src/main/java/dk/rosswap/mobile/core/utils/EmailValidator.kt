package dk.rosswap.mobile.core.utils

/**
 * Produces a ValidationResult with a reason and user-facing message.
 */
object EmailValidator {

    enum class Reason {
        INVALID_FORMAT,
        NO_DOMAIN,
        DISPOSABLE_EMAIL,
        SUSPICIOUS_PATTERN,
        UNKNOWN_DOMAIN,
        VALID
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: Reason,
        val message: String
    )

    private val LEGITIMATE_DOMAINS = setOf(
        // Major providers
        "gmail.com", "outlook.com", "yahoo.com", "hotmail.com", "icloud.com",
        "protonmail.com", "aol.com", "live.com", "msn.com", "yahoo.co.uk",

        // Danish providers
        "jubii.dk", "post.dk", "ofir.dk", "stofanet.dk", "tdc.dk", "webspeed.dk",
        "mail.dk", "spray.dk", "get2net.dk", "sol.dk",

        // European providers
        "web.de", "gmx.de", "t-online.de", "freenet.de", "mail.ru",
        "yandex.com", "orange.fr", "wanadoo.fr", "libero.it", "virgilio.it",

        // Business/Education domains patterns (kept as literal tokens to match subdomains/TLDs)
        "edu", "ac.uk", "edu.au", "edu.dk", "gov", "mil",

        // Other legitimate providers
        "zoho.com", "fastmail.com", "tutanota.com", "hey.com"
    )

    private val DISPOSABLE_DOMAINS = setOf(
        "10minutemail.com", "tempmail.org", "guerrillamail.com", "mailinator.com",
        "yopmail.com", "temp-mail.org", "throwaway.email", "getnada.com",
        "maildrop.cc", "sharklasers.com", "guerrillamail.de", "guerrillamail.net",
        "guerrillamail.org", "guerrillamail.biz", "spam4.me", "grr.la",
        "guerrillamailblock.com", "pokemail.net", "spamgourmet.com",
        "mailnesia.com", "trashmail.com", "33mail.com", "emailondeck.com",
        "fakeinbox.com", "tempail.com", "tempr.email", "dispostable.com",
        "mohmal.com", "emkei.cf", "thankyou2010.com", "trash-mail.com",
        "mytrashmail.com", "tempinbox.com", "temporarymail.com"
    )

    private val LEGITIMATE_TLDS = setOf(
        "com", "org", "net", "edu", "gov", "mil", "int",
        // country codes
        "dk", "se", "no", "fi", "de", "uk", "fr", "it", "es", "nl", "be",
        "ca", "au", "nz", "jp", "br", "in", "ch", "at", "pl", "ru"
    )

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validate(email: String): ValidationResult {
        val trimmed = email.trim().lowercase()

        if (trimmed.isEmpty()) {
            return ValidationResult(false, Reason.INVALID_FORMAT, "Indtast en gyldig email-adresse (f.eks. din@email.dk)")
        }

        if (!emailRegex.matches(trimmed)) {
            return ValidationResult(false, Reason.INVALID_FORMAT, "Indtast en gyldig email-adresse (f.eks. din@email.dk)")
        }

        val domain = getDomainFromEmail(trimmed)
            ?: return ValidationResult(false, Reason.NO_DOMAIN, "Email-adressen mangler et gyldigt domæne")

        if (isDisposableDomain(domain)) {
            return ValidationResult(false, Reason.DISPOSABLE_EMAIL, "Midlertidige email-adresser er ikke tilladt. Brug venligst din rigtige email.")
        }

        if (hasSuspiciousPatterns(trimmed)) {
            return ValidationResult(false, Reason.SUSPICIOUS_PATTERN, "Denne email-adresse ser ikke ægte ud. Brug venligst din rigtige email.")
        }

        if (!isLegitimateDomain(domain)) {
            return ValidationResult(false, Reason.UNKNOWN_DOMAIN, "Dette email-domæne genkendes ikke. Kontroller venligst din email-adresse.")
        }

        return ValidationResult(true, Reason.VALID, "Email-adresse er gyldig")
    }

    private fun getDomainFromEmail(email: String): String? = email.split('@').getOrNull(1)?.lowercase()

    private fun isDisposableDomain(domain: String): Boolean = DISPOSABLE_DOMAINS.contains(domain)

    private fun isLegitimateDomain(domain: String): Boolean {
        val parts = domain.split('.')
        if (parts.size < 2) return false

        val tld = parts.last()
        val sld = parts[parts.size - 2]

        val isDirectMatch = LEGITIMATE_DOMAINS.contains(domain)
        val isSubdomainMatch = LEGITIMATE_DOMAINS.any { legit -> domain.endsWith("." + legit) }
        val isTldLegitimate = LEGITIMATE_TLDS.contains(tld)
        val isSldValid = sld.length >= 2 && !Regex("^(test|fake|temp|spam|mail|email)").containsMatchIn(sld)

        return if (isDirectMatch) {
            isTldLegitimate && isSldValid
        } else {
            isSubdomainMatch && isTldLegitimate && isSldValid
        }
    }

    private fun hasSuspiciousPatterns(email: String): Boolean {
        val suspiciousPatterns = listOf(
            Regex("test.*@", RegexOption.IGNORE_CASE),
            Regex("fake.*@", RegexOption.IGNORE_CASE),
            Regex("spam.*@", RegexOption.IGNORE_CASE),
            Regex("temp.*@", RegexOption.IGNORE_CASE),
            Regex("throw.*away.*@", RegexOption.IGNORE_CASE),
            Regex("disposable.*@", RegexOption.IGNORE_CASE),
            Regex("@.*test", RegexOption.IGNORE_CASE),
            Regex("@.*fake", RegexOption.IGNORE_CASE),
            Regex("@.*temp", RegexOption.IGNORE_CASE),
            Regex("@.*spam", RegexOption.IGNORE_CASE)
        )

        return suspiciousPatterns.any { it.containsMatchIn(email) }
    }
}

