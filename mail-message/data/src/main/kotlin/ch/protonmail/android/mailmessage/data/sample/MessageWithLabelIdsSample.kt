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
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import me.proton.core.label.domain.entity.LabelId

object MessageWithLabelIdsSample {

    val AugWeatherForecast = build(
        message = MessageEntitySample.AugWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive)
    )

    val Invoice = build(
        message = MessageEntitySample.Invoice,
        labelIds = listOf(LabelIdSample.Archive, LabelIdSample.Document)
    )

    val SepWeatherForecast = build(
        message = MessageEntitySample.SepWeatherForecast,
        labelIds = listOf(LabelIdSample.Archive)
    )

    fun build(message: MessageEntity = MessageEntitySample.build(), labelIds: List<LabelId> = emptyList()) =
        MessageWithLabelIds(
            message = message,
            labelIds = labelIds
        )
}
