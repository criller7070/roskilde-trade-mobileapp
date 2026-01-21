package dk.rosswap.mobile.core.utils

object GenerateChatIdUtil {
    fun generate(userAId: String, userBId: String, itemId: String): Result<String> {
        val a = userAId.trim()
        val b = userBId.trim()
        val item = itemId.trim()

        if (a.isBlank()) return Result.failure(IllegalArgumentException("Missing userAId"))
        if (b.isBlank()) return Result.failure(IllegalArgumentException("Missing userBId"))
        if (a == b) return Result.failure(IllegalArgumentException("You can’t chat with yourself"))
        if (item.isBlank()) return Result.failure(IllegalArgumentException("Missing itemId"))

        val (minId, maxId) = if (a <= b) a to b else b to a
        return Result.success("${minId}_${maxId}_${item}")
    }
}

