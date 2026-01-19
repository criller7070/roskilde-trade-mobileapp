package dk.rosswap.mobile.feature.account.domain

import dk.rosswap.mobile.core.model.Item

data class AccountItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val mode: String = ""
)

fun AccountItem.toItem(): Item = Item(
    id = id,
    title = title,
    description = description,
    mode = mode,
    imageUrl = imageUrl,
    userId = "",
    userName = "",
    createdAt = null,
    price = 0.0
)

fun Item.toAccountItem(): AccountItem = AccountItem(
    id = id,
    title = title,
    description = description,
    imageUrl = imageUrl,
    mode = mode
)
