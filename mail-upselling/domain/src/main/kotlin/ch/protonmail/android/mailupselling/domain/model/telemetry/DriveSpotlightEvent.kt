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

sealed class DriveSpotlightEvent(
    val name: String,
    open val dimensions: DriveSpotlightEventDimensions
) {

    data class DriveSpotlightMailboxButtonTapped(
        override val dimensions: DriveSpotlightEventDimensions
    ) : DriveSpotlightEvent(name = "drive_spotlight_mailbox_button_tapped", dimensions)

    data class DriveSpotlightCTAButtonTapped(
        override val dimensions: DriveSpotlightEventDimensions
    ) : DriveSpotlightEvent(name = "drive_spotlight_cta_button_tapped", dimensions)

    fun toTelemetryEvent() = TelemetryEvent(
        group = EventGroup,
        name = name,
        dimensions = dimensions.asMap()
    )

    private companion object {

        const val EventGroup = "mail.any.upsell"
    }
}
