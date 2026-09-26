package com.moblin.android.platform.uikit

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.Bookmark
import com.moblin.android.platform.host.MoblinDocumentsProvider
import com.moblin.android.platform.host.SystemEvents
import java.io.File
import java.net.URI

fun UIApplication.canOpenURL(url: String): Boolean = OpenUrl.canOpen(AppDelegate.context, url)

fun UIApplication.canOpenURL(url: URI): Boolean = canOpenURL(url.toString())

fun UIApplication.open(url: String, completionHandler: ((Boolean) -> Unit)? = null) {
    val success = OpenUrl.open(SystemEvents.activity ?: AppDelegate.context, url)
    completionHandler?.invoke(success)
}

fun UIApplication.open(url: URI, completionHandler: ((Boolean) -> Unit)? = null) {
    open(url.toString(), completionHandler)
}

internal object OpenUrl {
    private const val TAG = "OpenUrl"
    private const val sharedDocumentsScheme = "shareddocuments"
    private const val externalStorageAuthority = "com.android.externalstorage.documents"
    private const val rootMimeType = "vnd.android.document/root"

    fun canOpen(context: Context, url: String): Boolean {
        return intents(context, url).any { context.packageManager.resolveActivity(it, 0) != null }
    }

    fun open(context: Context, url: String): Boolean {
        for (intent in intents(context, url)) {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (error: ActivityNotFoundException) {
                Log.i(TAG, "Nothing opens $url with ${intent.type ?: intent.data}")
            } catch (error: SecurityException) {
                Log.i(TAG, "Not allowed to open $url: $error")
            }
        }
        return false
    }

    internal fun intents(context: Context, url: String): List<Intent> {
        val scheme = url.substringBefore(':', "").lowercase()
        return when (scheme) {
            sharedDocumentsScheme -> directoryIntents(context, Uri.decode(url.substringAfter(':').removePrefix("//")))
            "file" -> directoryIntents(context, Uri.parse(url).path.orEmpty())
            "" -> emptyList()
            else -> listOf(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun directoryIntents(context: Context, path: String): List<Intent> {
        if (path.isEmpty()) {
            return emptyList()
        }
        val file = File(path)
        val provided = MoblinDocumentsProvider.documentUri(context, file)
        if (provided != null) {
            val intents = mutableListOf(view(provided, if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else null))
            if (file.isDirectory) {
                intents.add(view(MoblinDocumentsProvider.rootUri(context), rootMimeType))
            }
            return intents
        }
        val bookmarked = bookmarkedDocumentUri(context, file.path) ?: return emptyList()
        return listOf(view(bookmarked, DocumentsContract.Document.MIME_TYPE_DIR))
    }

    private fun view(uri: Uri, type: String?): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        if (type != null) {
            intent.setDataAndType(uri, type)
        } else {
            intent.data = uri
        }
        return intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun bookmarkedDocumentUri(context: Context, path: String): Uri? {
        val permissions = try {
            context.contentResolver.persistedUriPermissions
        } catch (error: Exception) {
            return null
        }
        for (permission in permissions) {
            val tree = permission.uri
            val treeId = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
            val directory = Bookmark.resolve(context, tree.toString().toByteArray()) ?: continue
            val relative = when {
                path == directory.path -> ""
                path.startsWith(directory.path + "/") -> path.removePrefix(directory.path + "/")
                else -> continue
            }
            val documentId = when {
                relative.isEmpty() -> treeId
                tree.authority != externalStorageAuthority -> continue
                treeId.endsWith(":") -> treeId + relative
                else -> "$treeId/$relative"
            }
            return DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        }
        return null
    }
}
