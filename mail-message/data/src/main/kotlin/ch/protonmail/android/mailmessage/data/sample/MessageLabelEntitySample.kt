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

package ch.protonmail.android.mailmessage.data.sample

import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

object MessageLabelEntitySample {

    val AugWeatherForecastArchive = build(
        messageId = MessageIdSample.AugWeatherForecast,
        labelId = LabelIdSample.Archive
    )

    val InvoiceArchive = build(
        messageId = MessageIdSample.Invoice,
        labelId = LabelIdSample.Archive
    )

    val InvoiceDocument = build(
        messageId = MessageIdSample.Invoice,
        labelId = LabelIdSample.Document
    )

    val SepWeatherForecastArchive = build(
        messageId = MessageIdSample.SepWeatherForecast,
        labelId = LabelIdSample.Archive
    )

    fun build(
        labelId: LabelId = LabelIdSample.build(),
        messageId: MessageId = MessageIdSample.build(),
        userId: UserId = UserIdSample.Primary
    ) = MessageLabelEntity(
        labelId = labelId,
        messageId = messageId,
        userId = userId
    )
}
