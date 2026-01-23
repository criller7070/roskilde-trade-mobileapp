package dk.rosswap.mobile.feature.bugreport.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.feature.bugreport.domain.BugReportRepository
import dk.rosswap.mobile.core.utils.GetFileExtensionUtil
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BugReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BugReportRepository {

    companion object {
        private const val TAG = "FirebaseBugReportRepo"
    }

    override suspend fun uploadImage(
        imageUri: String
    ): String? {
        return try {
            val uri = Uri.parse(imageUri)
            val extension = GetFileExtensionUtil.getFileExtension(context, uri)
            val ref = storage.reference
                .child("bug-reports/${System.currentTimeMillis()}.$extension")

            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed", e)
            null
        }
    }

    override suspend fun saveBugReport(bugDoc: Map<String, Any?>) {
        firestore.collection("bugReports").add(bugDoc).await()
    }
}
