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

package ch.protonmail.android.mailupselling.domain.model.telemetry

import me.proton.core.telemetry.domain.entity.TelemetryEvent

internal sealed class NPSFeedbackTelemetryEvent(
    val name: String,
    open val dimensions: NPSFeedbackEventDimensions
) {

    data class SubmitButtonTapped(
        override val dimensions: NPSFeedbackEventDimensions
    ) : NPSFeedbackTelemetryEvent(name = "nps_feedback_submit_tap", dimensions)

    data class Skipped(
        override val dimensions: NPSFeedbackEventDimensions
    ) : NPSFeedbackTelemetryEvent(name = "nps_feedback_skipped", dimensions)

    fun toTelemetryEvent() = TelemetryEvent(
        group = EventGroup,
        name = name,
        dimensions = dimensions.asMap()
    )

    private companion object {

        const val EventGroup = "mail.any.nps_feedback"
    }
}
