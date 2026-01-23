package dk.rosswap.mobile.feature.account.domain

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

class ExportAccountUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ExportAccountUseCase"
        private const val USERS_COLLECTION = "users"
        private const val ITEMS_COLLECTION = "items"
    }

    // this isn't a "sanitizer" per se, but a converter of firebase types to JSON types.
    private fun sanitizeValue(value: Any?): Any? {
        return when (value) {
            null -> JSONObject.NULL
            is com.google.firebase.Timestamp -> value.toDate().time // there you go
            is Map<*, *> -> {
                val jo = JSONObject()
                for ((k, v) in value) {
                    if (k is String) jo.put(k, sanitizeValue(v)) // recursion
                }
                jo
            }
            is List<*> -> {
                val ja = JSONArray()
                value.forEach { ja.put(sanitizeValue(it)) } // recursion
                ja
            }
            else -> value
        }
    }

    suspend operator fun invoke(targetUserId: String): Result<String> {
        val current = sessionManager.currentUserId() ?: return Result.failure(IllegalStateException("Not signed in"))
        if (current != targetUserId) return Result.failure(SecurityException("Not authorized"))

        return try {
            // collect user doc
            val userSnap = firestore.collection(USERS_COLLECTION).document(targetUserId).get().await()
            val userMap = userSnap.data ?: emptyMap<String, Any?>()

            // collect user's items
            val itemsSnap = firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("userId", targetUserId)
                .get()
                .await()

            val itemsList = itemsSnap.documents.mapNotNull { doc ->
                try {
                    val raw = doc.data ?: emptyMap<String, Any?>()
                    // sanitize map values
                    val jo = JSONObject()
                    for ((k, v) in raw) {
                        if (k is String) jo.put(k, sanitizeValue(v))
                    }
                    jo
                } catch (_: Exception) {
                    null
                }
            }

            val exportObj = JSONObject()
            // put sanitized user
            val userJo = JSONObject()
            for ((k, v) in userMap) {
                if (k is String) userJo.put(k, sanitizeValue(v))
            }
            exportObj.put("user", userJo)

            // put items array
            val itemsJa = JSONArray()
            itemsList.forEach { itemsJa.put(it) }
            exportObj.put("items", itemsJa)
            exportObj.put("exportedAt", System.currentTimeMillis())

            val json = exportObj.toString(2)

            // write to a local file in cache dir
            val fileName = "export_${targetUserId}_${System.currentTimeMillis()}.json"
            val file = File(context.cacheDir, fileName)
            file.writeText(json)

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed export account: ${e.message}", e)
            Result.failure(e)
        }
    }
}
