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

package ch.protonmail.android.mailmessage.domain.sample

import ch.protonmail.android.mailmessage.domain.entity.MessageId

object MessageIdSample {

    val AugWeatherForecast = MessageId("aug_weather_forecast")
    val EmptyDraft = MessageId("empty_draft")
    val Invoice = MessageId("invoice")
    val OctWeatherForecast = MessageId("oct_weather_forecast")
    val SepWeatherForecast = MessageId("sep_weather_forecast")
    val AlphaAppQAReport = MessageId("QA_testing_report")
    val AlphaAppInfoRequest = MessageId("alpha_app_info_request")

    fun build() = MessageId("message")
}
