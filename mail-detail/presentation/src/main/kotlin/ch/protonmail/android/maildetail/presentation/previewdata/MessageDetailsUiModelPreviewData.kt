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

package ch.protonmail.android.maildetail.presentation.previewdata

import ch.protonmail.android.maildetail.presentation.model.MessageUiModel
import ch.protonmail.android.mailmessage.domain.entity.MessageId

object MessageDetailsUiModelPreviewData {

    val FirstWeekOfAugWeatherForecast = MessageUiModel(
        messageId = MessageId("Weather Forecast for the first week of August"),
        subject = "Weather Forecast for the first week of August",
        isStarred = true
    )

    val LoremIpsum30words = MessageUiModel(
        messageId = MessageId("Lorem Ipsum"),
        subject = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. In volutpat elit vitae massa bibendum, " +
            "sed vestibulum velit feugiat. Suspendisse molestie purus at ornare cursus. Mauris placerat tortor est, " +
            "et elementum.",
        isStarred = false
    )
}
