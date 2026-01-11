// kotlin
package dk.rosswap.mobile.feature.items.presentation

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.items.domain.Post

object MockPosts {

    fun getMockPosts(): List<Post> {
        val now = Timestamp.now()
        return listOf(
            Post(
                title = "Plastic ring",
                description = "M green, lavet af recycled plastic!!!",
                imageUrl = "https://picsum.photos/seed/plastic/800/600",
                mode = "bytte",
                userId = "r8yTEyOVrddlcXjSOkmaVGTJVZ92",
                userName = "Criller Hyllested",
                createdAt = now
            ),
            Post(
                title = "Vintage Lamp",
                description = "Works fine. Slight scratches on base.",
                imageUrl = "https://picsum.photos/seed/lamp/800/600",
                mode = "salg",
                userId = "user_test_1",
                userName = "Maja Jensen",
                createdAt = now
            ),
            Post(
                title = "Bike Basket",
                description = "Detachable basket for city bikes. Like new.",
                imageUrl = "https://picsum.photos/seed/basket/800/600",
                mode = "bytte",
                userId = "user_test_2",
                userName = "Lars Nielsen",
                createdAt = now
            ),
            Post(
                title = "Houseplants x3",
                description = "Three small succulents. Free to pick up.",
                imageUrl = "https://picsum.photos/seed/plants/800/600",
                mode = "gratis",
                userId = "user_test_3",
                userName = "Sofie Holm",
                createdAt = now
            ),
            Post(
                title = "Winter Coat",
                description = "Warm coat, size M. Only used one winter.",
                imageUrl = "https://picsum.photos/seed/coat/800/600",
                mode = "salg",
                userId = "user_test_4",
                userName = "Anders Kristensen",
                createdAt = now
            )
        )
    }

    /**
     * Upload mock posts to Firestore collection "posts".
     * Use only in debug/testing.
     * Example usage:
     *   MockPosts.uploadMockPosts(FirebaseFirestore.getInstance()) { success ->
     *     // handle result
     *   }
     */
    fun uploadMockPosts(firestore: FirebaseFirestore, onComplete: (Boolean) -> Unit = {}) {
        val posts = getMockPosts()
        if (posts.isEmpty()) {
            onComplete(true)
            return
        }

        var remaining = posts.size
        var overallSuccess = true

        for (post in posts) {
            // Firestore will map the Kotlin data class fields to document fields
            firestore.collection("posts")
                .add(post)
                .addOnSuccessListener {
                    remaining -= 1
                    if (remaining == 0) onComplete(overallSuccess)
                }
                .addOnFailureListener {
                    overallSuccess = false
                    remaining -= 1
                    if (remaining == 0) onComplete(overallSuccess)
                }
        }
    }
}
