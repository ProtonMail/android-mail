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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailmessage.data.local.entity.MessageBodyEntity
import ch.protonmail.android.mailmessage.data.local.entity.UnsubscribeMethodsEntity
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.testdata.user.UserIdTestData

object MessageBodyEntityTestData {

    private const val RAW_ENCRYPTED_MESSAGE_BODY = "This is a raw encrypted message body."

    val messageBodyEntity = MessageBodyEntity(
        userId = UserIdTestData.userId,
        messageId = MessageId(MessageTestData.RAW_MESSAGE_ID),
        body = RAW_ENCRYPTED_MESSAGE_BODY,
        header = "",
        mimeType = "",
        spamScore = "",
        replyTo = RecipientSample.John,
        replyTos = emptyList(),
        unsubscribeMethodsEntity = UnsubscribeMethodsEntity(null, null, null)
    )
}
