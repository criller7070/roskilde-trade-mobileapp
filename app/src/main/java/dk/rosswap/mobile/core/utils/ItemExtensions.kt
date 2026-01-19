package dk.rosswap.mobile.core.utils

import android.os.Bundle
import dk.rosswap.mobile.core.common.Item

/**
 * Extension function to convert an Item to a Bundle containing navigation arguments
 * for the item detail screen.
 */
fun Item.toDetailBundle(): Bundle {
    return Bundle().apply {
        putString("itemId", id)
        putString("itemTitle", title)
        putString("itemDescription", description)
        putString("itemImage", imageUrl)
        putString("itemUserId", userId)
        putString("itemUserName", userName)
    }
}