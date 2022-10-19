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

package ch.protonmail.android.testdata.label

import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.testdata.message.MessageIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object MessageLabelEntityTestData {

    val AugWeatherForecastArchive = build(
        messageId = MessageIdTestData.AugWeatherForecast,
        labelId = LabelIdTestData.Archive
    )

    val InvoiceArchive = build(
        messageId = MessageIdTestData.Invoice,
        labelId = LabelIdTestData.Archive
    )

    val InvoiceDocument = build(
        messageId = MessageIdTestData.Invoice,
        labelId = LabelIdTestData.Document
    )

    val SepWeatherForecastArchive = build(
        messageId = MessageIdTestData.SepWeatherForecast,
        labelId = LabelIdTestData.Archive
    )

    fun build(
        labelId: LabelId = LabelIdTestData.build(),
        messageId: MessageId = MessageIdTestData.build(),
        userId: UserId = UserIdTestData.Primary
    ) = MessageLabelEntity(
        labelId = labelId,
        messageId = messageId,
        userId = userId
    )
}
