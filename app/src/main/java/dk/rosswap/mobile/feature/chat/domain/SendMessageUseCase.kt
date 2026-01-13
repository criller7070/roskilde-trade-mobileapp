package dk.rosswap.mobile.feature.chat.domain

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.delay

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    private val lastSentTimestamps = ConcurrentHashMap<String, Long>()
    private val rateLimitMillis = 1500L

    // Burst limiting: track recent sends to detect rapid-fire messages
    private val recentSendTimes = ConcurrentHashMap<String, MutableList<Long>>()
    private val burstThresholdMillis = 1000L
    private val burstMessageLimit = 3
    private val burstCooldownMillis = 5000L
    private val burstCooldownEndTimes = ConcurrentHashMap<String, Long>()

    suspend operator fun invoke(chatId: String, senderId: String, text: String): Result<Unit> {
        val trimmed = text.trim()

        // Validation
        if (trimmed.isEmpty() || trimmed.length > 500) {
            return Result.failure(
                IllegalArgumentException("Message must be between 1 and 500 characters.")
            )
        }

        val now = System.currentTimeMillis()

        // Check burst cooldown first (highest priority)
        val burstCooldownEnd = burstCooldownEndTimes[senderId] ?: 0L
        if (now < burstCooldownEnd) {
            val remainingMs = (burstCooldownEnd - now).coerceAtLeast(0)
            return Result.failure(
                IllegalStateException("Too many messages sent too quickly. Wait ${(remainingMs / 1000).toInt() + 1}s.")
            )
        }

        // Check for burst pattern - synchronized to ensure thread-safe list operations
        val sendTimes = recentSendTimes.getOrPut(senderId) { mutableListOf() }
        val exceedsBurstLimit = synchronized(sendTimes) {
            sendTimes.removeAll { it < now - burstThresholdMillis }
            sendTimes.size >= burstMessageLimit
        }

        if (exceedsBurstLimit) {
            // User exceeded burst limit
            burstCooldownEndTimes[senderId] = now + burstCooldownMillis
            synchronized(sendTimes) {
                sendTimes.clear()
            }
            val remainingMs = burstCooldownMillis
            return Result.failure(
                IllegalStateException("Too many messages sent too quickly. Wait ${(remainingMs / 1000).toInt() + 1}s.")
            )
        }

        // Check standard rate limit
        val lastSent = lastSentTimestamps[senderId]
        if (lastSent != null && now - lastSent < rateLimitMillis) {
            return Result.failure(
                IllegalStateException("You're sending messages too quickly—give it a moment.")
            )
        }

        // Retry logic with exponential backoff
        return retryWithBackoff(
            maxAttempts = 3,
            initialDelayMs = 1000L
        ) {
            repository.sendTextMessage(chatId = chatId, senderId = senderId, text = trimmed)
        }.onSuccess {
            lastSentTimestamps[senderId] = now
            synchronized(sendTimes) {
                sendTimes.add(now)
            }
        }
    }

    private suspend inline fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        block: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e

                if (attempt < maxAttempts) {
                    // Exponential backoff with jitter
                    val delayMs = initialDelayMs * (1L shl (attempt - 1))
                    val jitterMs = (Math.random() * 500).toLong()
                    val totalDelayMs = delayMs + jitterMs
                    delay(totalDelayMs)
                }
            }
        }

        return Result.failure(lastException ?: Exception("Message send failed after $maxAttempts attempts"))
    }
}
