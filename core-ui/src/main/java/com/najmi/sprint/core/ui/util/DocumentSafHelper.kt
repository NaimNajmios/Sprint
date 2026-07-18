package com.najmi.sprint.core.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Helper object for Storage Access Framework (SAF) URI permission persistence.
 */
object DocumentSafHelper {

    /**
     * Persists read permissions for the selected URI (e.g., an Obsidian markdown file).
     */
    fun takePersistableUriPermission(context: Context, uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates an intent to launch an external viewer for the given document URI.
     */
    fun createViewIntent(uriString: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            val uri = Uri.parse(uriString)
            setDataAndType(uri, "text/markdown")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
