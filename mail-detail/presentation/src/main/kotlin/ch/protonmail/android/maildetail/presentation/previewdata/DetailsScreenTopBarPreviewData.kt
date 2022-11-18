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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

object DetailsScreenTopBarPreviewData {

    val LoadingExpanded = DetailsScreenTopBarPreview(
        title = " ",
        isStarred = null,
        messageCount = null,
        isExpanded = true
    )

    val LoadingCollapsed = DetailsScreenTopBarPreview(
        title = " ",
        isStarred = null,
        messageCount = null,
        isExpanded = false
    )

    val WeatherForecastExpanded = DetailsScreenTopBarPreview(
        title = ConversationDetailsUiModelPreviewData.WeatherForecast.subject,
        isStarred = ConversationDetailsUiModelPreviewData.WeatherForecast.isStarred,
        messageCount = ConversationDetailsUiModelPreviewData.WeatherForecast.messageCount,
        isExpanded = true
    )

    val WeatherForecastCollapsed = DetailsScreenTopBarPreview(
        title = ConversationDetailsUiModelPreviewData.WeatherForecast.subject,
        isStarred = ConversationDetailsUiModelPreviewData.WeatherForecast.isStarred,
        messageCount = ConversationDetailsUiModelPreviewData.WeatherForecast.messageCount,
        isExpanded = false
    )

    val FirstWeekOfAugWeatherForecastExpanded = DetailsScreenTopBarPreview(
        title = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast.subject,
        isStarred = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast.isStarred,
        messageCount = null,
        isExpanded = true
    )

    val FirstWeekOfAugWeatherForecastCollapsed = DetailsScreenTopBarPreview(
        title = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast.subject,
        isStarred = MessageDetailActionBarUiModelPreviewData.FirstWeekOfAugWeatherForecast.isStarred,
        messageCount = null,
        isExpanded = false
    )

    val LoremIpsum30wordsExpanded = DetailsScreenTopBarPreview(
        title = MessageDetailActionBarUiModelPreviewData.LoremIpsum30words.subject,
        isStarred = MessageDetailActionBarUiModelPreviewData.LoremIpsum30words.isStarred,
        messageCount = null,
        isExpanded = true
    )

    val LoremIpsum30wordsCollapsed = DetailsScreenTopBarPreview(
        title = MessageDetailActionBarUiModelPreviewData.LoremIpsum30words.subject,
        isStarred = MessageDetailActionBarUiModelPreviewData.LoremIpsum30words.isStarred,
        messageCount = null,
        isExpanded = false
    )
}

data class DetailsScreenTopBarPreview(
    val title: String,
    val isStarred: Boolean?,
    val messageCount: Int?,
    val isExpanded: Boolean
)

class DetailsScreenTopBarPreviewProvider : PreviewParameterProvider<DetailsScreenTopBarPreview> {

    override val values = sequenceOf(
        DetailsScreenTopBarPreviewData.LoadingExpanded,
        DetailsScreenTopBarPreviewData.LoadingCollapsed,
        DetailsScreenTopBarPreviewData.WeatherForecastExpanded,
        DetailsScreenTopBarPreviewData.WeatherForecastCollapsed,
        DetailsScreenTopBarPreviewData.FirstWeekOfAugWeatherForecastExpanded,
        DetailsScreenTopBarPreviewData.FirstWeekOfAugWeatherForecastCollapsed,
        DetailsScreenTopBarPreviewData.LoremIpsum30wordsExpanded,
        DetailsScreenTopBarPreviewData.LoremIpsum30wordsCollapsed
    )
}
