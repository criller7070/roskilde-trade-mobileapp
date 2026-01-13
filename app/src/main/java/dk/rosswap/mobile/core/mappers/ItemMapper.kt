package dk.rosswap.mobile.core.mappers

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
}

