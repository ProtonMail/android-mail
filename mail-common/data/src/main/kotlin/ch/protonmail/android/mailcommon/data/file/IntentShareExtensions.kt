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

package ch.protonmail.android.mailcommon.data.file

import android.content.Intent
import android.net.Uri
import android.os.Build
import ch.protonmail.android.mailcommon.domain.model.IntentShareInfo
import timber.log.Timber

object IntentExtraKeys {
    const val EXTRA_EXTERNAL_SHARE = "external_share"
}

/**
 * Reads the shareable content out of an inbound intent.
 *
 * [uriValidator] rejects any attachment [Uri] the app must not dereference on the sender's behalf; see
 * [ShareUriValidator]. Every entry point that can carry a [Uri] runs through it, as all of them are exported.
 */
fun Intent.getShareInfo(uriValidator: ShareUriValidator): IntentShareInfo {

    return when (action) {
        Intent.ACTION_SEND -> getShareInfoForSingleSendAction(uriValidator)
        Intent.ACTION_SEND_MULTIPLE -> getShareInfoForMultipleSendAction(uriValidator)
        Intent.ACTION_VIEW,
        Intent.ACTION_SENDTO -> getShareInfoForViewAndSendToAction(uriValidator)

        else -> {
            Timber.d("Unhandled intent action: $action")
            IntentShareInfo.Empty
        }
    }
}

fun Intent.isExternal(): Boolean = getBooleanExtra(IntentExtraKeys.EXTRA_EXTERNAL_SHARE, false) &&
    action != Intent.ACTION_MAIN

fun Intent.isMailToIntent(): Boolean = this.scheme == MAILTO_SCHEME

private fun Intent.getShareInfoForSingleSendAction(uriValidator: ShareUriValidator): IntentShareInfo {
    val fileUriList = if (type == MIME_TYPE_TEXT_PLAIN) {
        emptyList()
    } else {
        getFileUriForActionSend(uriValidator)?.let {
            listOf(it.toString())
        } ?: emptyList()
    }

    return IntentShareInfo(
        attachmentUris = fileUriList,
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody(),
        isExternal = isExternal()
    )
}

private fun Intent.getShareInfoForMultipleSendAction(uriValidator: ShareUriValidator): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = getFileUrisForActionSendMultiple(uriValidator).map { it.toString() },
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody(),
        isExternal = isExternal()
    )
}

private fun Intent.getShareInfoForViewAndSendToAction(uriValidator: ShareUriValidator): IntentShareInfo {
    return IntentShareInfo(
        attachmentUris = getFileUrisForActionViewAndSendTo(uriValidator).map { it.toString() },
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody(),
        isExternal = isExternal()
    )
}

private fun Intent.getFileUrisForActionViewAndSendTo(uriValidator: ShareUriValidator): List<Uri> {
    val fileUris = mutableListOf<Uri>()

    data?.let { data ->
        fileUris.add(data)
    }

    return uriValidator.filterAllowed(fileUris)
}

private fun Intent.getFileUriForActionSend(uriValidator: ShareUriValidator): Uri? {
    val clipData = clipData
    val fileUri = if (clipData != null) {
        clipData.getItemAt(0)?.uri
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        }
    }

    return fileUri?.takeIf { uriValidator.isAllowed(it) }
}

private fun Intent.getFileUrisForActionSendMultiple(uriValidator: ShareUriValidator): List<Uri> {
    val fileUris = mutableListOf<Uri>()

    val clipData = clipData
    if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            clipData.getItemAt(i)?.uri?.run { fileUris.add(this) }
        }
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { fileUris.addAll(it) }
        } else {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { fileUris.addAll(it) }
        }
    }

    return uriValidator.filterAllowed(fileUris)
}

private fun Intent.getSubject(): String? = getStringExtra(Intent.EXTRA_SUBJECT)

private fun Intent.getRecipientTo(): List<String> = getStringArrayExtra(Intent.EXTRA_EMAIL)?.toList() ?: emptyList()

private fun Intent.getRecipientCc(): List<String> = getStringArrayExtra(Intent.EXTRA_CC)?.toList() ?: emptyList()

private fun Intent.getRecipientBcc(): List<String> = getStringArrayExtra(Intent.EXTRA_BCC)?.toList() ?: emptyList()

private fun Intent.getEmailBody(): String? = getStringExtra(Intent.EXTRA_TEXT)

/**
 * Intent data can be a [Uri] with a mailto scheme instead of a shared file.
 */
private const val MAILTO_SCHEME = "mailto"
private const val MIME_TYPE_TEXT_PLAIN = "text/plain"
