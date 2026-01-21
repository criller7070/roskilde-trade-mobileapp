package dk.rosswap.mobile.core.common

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

object GetFileExtensionUtil {

    // currently used in bug reports, should probably also be used for uploading posts.

    private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")

    // MIME = Multipurpose Internet Mail Extensions = .jpeg etc. These are specific types
    // of "extensions", i.e. "the part that comes after a dot in a file" e.g. .exe etc
    fun getFileExtension(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)

        val extension = if (mimeType != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        } else {
            val path = uri.path
            if (path != null && path.contains('.')) {
                // take text after last dot, and strip query/fragment if present
                val raw = path.substringAfterLast('.')
                val cleaned = raw.substringBefore('?').substringBefore('#')
                if (!cleaned.contains('/') && !cleaned.contains('\\')) {
                    cleaned
                } else {
                    null
                }
            } else {
                null
            }
        }

        return if (extension != null && extension.lowercase() in ALLOWED_IMAGE_EXTENSIONS) {
            extension.lowercase()
        } else {
            "jpg"
        }
    }
}
