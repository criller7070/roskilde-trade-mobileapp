package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.firestore.DocumentSnapshot
import dk.rosswap.mobile.core.data.ItemDto as CoreItemDto
import dk.rosswap.mobile.core.mappers.ItemMapper as CoreItemMapper
import dk.rosswap.mobile.core.model.Item

object LikedMapper {
    fun fromDoc(doc: DocumentSnapshot): Item {
        val coreDto = CoreItemDto(
            title = doc.getString("title"),
            description = doc.getString("description"),
            mode = doc.getString("mode"),
            imageUrl = doc.getString("imageUrl"),
            userId = doc.getString("userId"),
            userName = doc.getString("userName"),
            createdAt = doc.getTimestamp("createdAt"),
            price = doc.getDouble("price") ?: 0.0
        )
        return CoreItemMapper.fromDto(coreDto, doc.id)
    }
}
