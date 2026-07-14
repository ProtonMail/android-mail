/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.maildetail.presentation.usecase.print

import android.content.Context
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyContent
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class PrintMessageDocumentBuilder @Inject constructor(
    private val headerBuilder: PrintMessageHeaderBuilder,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun buildDocument(
        context: Context,
        subject: String,
        messageHeader: MessageDetailHeaderUiModel,
        messageBody: MessageBodyUiModel
    ): String {
        val header = headerBuilder.buildHeader(
            subject = subject,
            messageHeader = messageHeader,
            attachments = messageBody.attachments
        )

        return header + messageBody.messageBody.resolve(context)
    }

    private suspend fun MessageBodyContent.resolve(context: Context): String = when (this) {
        is MessageBodyContent.Text -> value
        is MessageBodyContent.File -> withContext(ioDispatcher) {
            runCatching {
                context.contentResolver.openInputStream(contentUri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
            }.getOrElse {
                Timber.e(it, "Unable to read message body file for printing.")
                null
            }.orEmpty()
        }
    }
}
