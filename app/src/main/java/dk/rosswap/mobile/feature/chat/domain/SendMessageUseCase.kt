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
    
    // Lock objects per sender to ensure consistent synchronization
    private val senderLocks = ConcurrentHashMap<String, Any>()

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

        // Use a dedicated lock per sender to ensure consistent synchronization
        val lock = senderLocks.computeIfAbsent(senderId) { Any() }
        
        // Check for burst pattern and standard rate limit - synchronized to ensure thread-safe operations
        val exceedsBurstLimit: Boolean
        val shouldCheckRateLimit: Boolean
        
        synchronized(lock) {
            val sendTimes = recentSendTimes.computeIfAbsent(senderId) { mutableListOf() }
            sendTimes.removeAll { it < now - burstThresholdMillis }
            
            exceedsBurstLimit = sendTimes.size >= burstMessageLimit
            
            if (exceedsBurstLimit) {
                burstCooldownEndTimes[senderId] = now + burstCooldownMillis
                sendTimes.clear()
            }
            
            val lastSent = lastSentTimestamps[senderId]
            shouldCheckRateLimit = lastSent != null && now - lastSent < rateLimitMillis
        }

        if (exceedsBurstLimit) {
            // User exceeded burst limit
            val remainingMs = burstCooldownMillis
            return Result.failure(
                IllegalStateException("Too many messages sent too quickly. Wait ${(remainingMs / 1000).toInt() + 1}s.")
            )
        }

        // Check standard rate limit
        if (shouldCheckRateLimit) {
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
            synchronized(lock) {
                lastSentTimestamps[senderId] = now
                val sendTimes = recentSendTimes.computeIfAbsent(senderId) { mutableListOf() }
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
