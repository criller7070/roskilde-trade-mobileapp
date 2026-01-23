package dk.rosswap.mobile.core.mappers

import com.google.firebase.Timestamp
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.data.ItemDto

object ItemMapper {
    fun fromDto(dto: ItemDto, id: String): Item {
        return Item(
            id = id,
            title = dto.title ?: "",
            description = dto.description ?: "",
            mode = dto.mode ?: "",
            imageUrl = dto.imageUrl ?: "",
            userId = dto.userId ?: "",
            userName = dto.userName ?: "",
            createdAt = dto.createdAt,
            price = dto.price
        )
    }

    fun toDto(item: Item): ItemDto {
        return ItemDto(
            title = item.title,
            description = item.description,
            mode = item.mode,
            imageUrl = item.imageUrl,
            userId = item.userId,
            userName = item.userName,
            createdAt = item.createdAt,
            price = item.price
        )
    }
    fun fromMap(map: Map<String, Any?>, id: String): Item {

        val dto = ItemDto(
            title = (map["title"] as? String)?.trim(),
            description = (map["description"] as? String)?.trim(),
            mode = (map["mode"] as? String)?.trim(),
            imageUrl = (map["imageUrl"] as? String)?.trim(),
            userId = (map["userId"] as? String)?.trim(),
            userName = (map["userName"] as? String)?.trim(),
            createdAt = map["createdAt"] as? Timestamp,
            price = 0.0 // just a placeholder for now, no price feature in the app
        )

        return fromDto(dto, id)
    }
}
