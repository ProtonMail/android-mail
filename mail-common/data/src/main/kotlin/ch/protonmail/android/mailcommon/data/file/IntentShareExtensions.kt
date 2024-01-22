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
import androidx.core.net.MailTo
import ch.protonmail.android.mailcommon.domain.model.FileShareInfo
import me.proton.core.util.kotlin.takeIfNotEmpty
import timber.log.Timber

fun Intent.getFileShareInfo(): FileShareInfo {

    return when (action) {
        Intent.ACTION_SEND -> getShareInfoForActionSendSingleFile()
        Intent.ACTION_SEND_MULTIPLE -> getShareInfoForActionSendMultipleFiles()
        Intent.ACTION_VIEW -> getShareInfoForActionView()
        Intent.ACTION_SENDTO -> getShareInfoForActionSendTo()

        else -> {
            Timber.d("Unhandled intent action: $action")
            FileShareInfo.Empty
        }
    }
}

fun Intent.isStartedFromLauncher(): Boolean = action == Intent.ACTION_MAIN

private fun Intent.getShareInfoForActionSendSingleFile(): FileShareInfo {

    return FileShareInfo(
        attachmentUris = getFileUriForActionSendSingleFile().map { it.toString() },
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody()
    )
}

private fun Intent.getShareInfoForActionSendMultipleFiles(): FileShareInfo {
    return FileShareInfo(
        attachmentUris = getFileUrisForActionSendMultipleFiles().map { it.toString() },
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody()
    )
}


private fun Intent.getShareInfoForActionView(): FileShareInfo {
    val intentUri = getFileUrisForActionViewAndSendTo().takeIfNotEmpty()?.firstOrNull()

    return if (intentUri?.scheme == MAILTO_SCHEME) {
        val mailTo = MailTo.parse(intentUri)

        val toRecipients = mailTo.to
            ?.split(",")
            ?.map { it.trim() }
            ?: getRecipientTo()

        val ccRecipients: List<String> = mailTo.cc
            ?.split(",")
            ?: getRecipientCc()

        val bccRecipients: List<String> = mailTo.bcc
            ?.split(",")
            ?: getRecipientBcc()

        val subject = mailTo.subject ?: getSubject()
        val body = mailTo.body ?: getEmailBody()

        FileShareInfo(
            attachmentUris = emptyList(),
            emailSubject = subject,
            emailRecipientTo = toRecipients,
            emailRecipientCc = ccRecipients,
            emailRecipientBcc = bccRecipients,
            emailBody = body
        )
    } else {
        getShareInfoForActionSendTo()
    }

}


private fun Intent.getShareInfoForActionSendTo(): FileShareInfo {
    return FileShareInfo(
        attachmentUris = getFileUrisForActionViewAndSendTo().map { it.toString() },
        emailSubject = getSubject(),
        emailRecipientTo = getRecipientTo(),
        emailRecipientCc = getRecipientCc(),
        emailRecipientBcc = getRecipientBcc(),
        emailBody = getEmailBody()
    )
}

private fun Intent.getFileUrisForActionViewAndSendTo(): List<Uri> {
    val fileUris = mutableListOf<Uri>()

    data?.let { data ->
        fileUris.add(data)
    }

    return fileUris
}

private fun Intent.getFileUriForActionSendSingleFile(): List<Uri> {
    val fileUris = mutableListOf<Uri>()

    val clipData = clipData
    if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            fileUris.add(clipData.getItemAt(i).uri)
        }
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { fileUris.add(it) }
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { fileUris.add(it) }
        }
    }

    return fileUris
}

private fun Intent.getFileUrisForActionSendMultipleFiles(): List<Uri> {
    val fileUris = mutableListOf<Uri>()

    val clipData = clipData
    if (clipData != null) {
        for (i in 0 until clipData.itemCount) {
            fileUris.add(clipData.getItemAt(i).uri)
        }
    } else {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { fileUris.addAll(it) }
        } else {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { fileUris.addAll(it) }
        }
    }

    return fileUris
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
