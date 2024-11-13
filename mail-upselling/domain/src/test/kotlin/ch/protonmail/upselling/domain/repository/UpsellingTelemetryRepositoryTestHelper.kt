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

package ch.protonmail.upselling.domain.repository

import java.time.Instant
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventDimensions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic

internal object UpsellingTelemetryRepositoryTestHelper {

    val BaseDimensions: UpsellingTelemetryEventDimensions
        get() = UpsellingTelemetryEventDimensions().apply {
            addPlanBeforeUpgrade("free")
            addDaysSinceAccountCreation("01-03")
            addUpsellModalVersion("A.1")
            addUpsellEntryPoint("contact_groups")
        }

    val UpgradeDimensions: UpsellingTelemetryEventDimensions
        get() = UpsellingTelemetryEventDimensions().apply {
            addPlanBeforeUpgrade("free")
            addDaysSinceAccountCreation("01-03")
            addUpsellModalVersion("A.1")
            addSelectedPlan("mail2022")
            addSelectedPlanCycle(1)
            addUpsellEntryPoint("contact_groups")
        }

    fun mockInstant() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns mockk { every { epochSecond } returns 0 }
    }
}
