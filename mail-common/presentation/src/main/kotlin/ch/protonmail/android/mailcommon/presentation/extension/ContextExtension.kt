/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailcommon.presentation.extension

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import timber.log.Timber

fun Context.copyTextToClipboard(label: String, text: String) {
    val clipboardManager = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)

    clipboardManager?.setPrimaryClip(clip) ?: Timber.w("Unable to copy data to clipboard")
}

fun Context.openShareIntentForUri(uri: Uri, shareViaTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, uri.toString())
        setType("text/plain")
    }

    startActivity(Intent.createChooser(intent, shareViaTitle))
}
