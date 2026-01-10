package dk.rosswap.mobile.feature.items.domain

import android.net.Uri
import javax.inject.Inject

class AddItemUseCase @Inject constructor(
    private val repository: ItemsRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        imageUri: Uri?,
        type: String
    ): Result<Unit> {
        val trimmedTitle = title.trim()
        val trimmedDescription = description.trim()

        if (trimmedTitle.isEmpty()) return Result.failure(IllegalArgumentException("Title is required"))
        if (trimmedTitle.length > 60) return Result.failure(IllegalArgumentException("Title must be at most 60 characters"))
        if (trimmedDescription.isEmpty()) return Result.failure(IllegalArgumentException("Description is required"))
        if (trimmedDescription.length > 500) return Result.failure(IllegalArgumentException("Description must be at most 500 characters"))

        val mode = type.lowercase()
        if (mode != "bytte" && mode != "sælge") {
            return Result.failure(IllegalArgumentException("Mode must be 'bytte' or 'sælge'"))
        }

        return repository.createItem(trimmedTitle, trimmedDescription, imageUri, mode)
    }
}
