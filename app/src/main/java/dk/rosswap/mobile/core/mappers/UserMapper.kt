package dk.rosswap.mobile.core.mappers

import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.core.data.UserDto

object UserMapper {

    fun fromDto(dto: UserDto): User {
        return User(
            uid = dto.uid,
            name = dto.name,
            email = dto.email,
            photoURL = dto.photoURL,
            createdAt = dto.createdAt,
            gdprConsent = dto.gdprConsent,
            consentedAt = dto.consentedAt,
            likedItemIds = dto.likedItemIds,
            dislikedItemIds = dto.dislikedItemIds,
            emailVerified = dto.emailVerified,
            isAnonymous = dto.isAnonymous
        )
    }

    fun toDto(user: User): UserDto {
        return UserDto(
            uid = user.uid,
            name = user.name,
            email = user.email,
            photoURL = user.photoURL,
            createdAt = user.createdAt,
            gdprConsent = user.gdprConsent,
            consentedAt = user.consentedAt,
            likedItemIds = user.likedItemIds,
            dislikedItemIds = user.dislikedItemIds,
            emailVerified = user.emailVerified,
            isAnonymous = user.isAnonymous
        )
    }
}

