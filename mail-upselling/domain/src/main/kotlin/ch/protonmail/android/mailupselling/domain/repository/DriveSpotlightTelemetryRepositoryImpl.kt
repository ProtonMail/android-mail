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

package ch.protonmail.android.mailupselling.domain.repository

import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEvent.DriveSpotlightCTAButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEvent.DriveSpotlightMailboxButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.DriveSpotlightTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.toUpsellingTelemetryDimensionValue
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.user.domain.entity.User
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject

@ViewModelScoped
class DriveSpotlightTelemetryRepositoryImpl @Inject constructor(
    private val getAccountAgeInDays: GetAccountAgeInDays,
    private val getPrimaryUser: GetPrimaryUser,
    private val getSubscriptionName: GetSubscriptionName,
    private val telemetryManager: TelemetryManager,
    private val scopeProvider: CoroutineScopeProvider
) : DriveSpotlightTelemetryRepository {

    override fun trackEvent(eventType: DriveSpotlightTelemetryEventType) = onSupervisedScope {
        val user = getPrimaryUser() ?: return@onSupervisedScope

        val event = createBaseEvent(eventType, user).getOrElse { return@onSupervisedScope }

        telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate)
    }

    private suspend fun createBaseEvent(event: DriveSpotlightTelemetryEventType, user: User) = either {
        val dimensions = buildTelemetryDimensions(user).getOrElse {
            raise(CreateTelemetryEventError)
        }

        when (event) {
            DriveSpotlightTelemetryEventType.DriveSpotlightCTATap ->
                DriveSpotlightCTAButtonTapped(dimensions).toTelemetryEvent()
            DriveSpotlightTelemetryEventType.MailboxDriveSpotlightButtonTap ->
                DriveSpotlightMailboxButtonTapped(dimensions).toTelemetryEvent()
        }
    }

    private suspend fun buildTelemetryDimensions(user: User) = either {
        val accountAgeInDays = getAccountAgeInDays(user).toUpsellingTelemetryDimensionValue()
        val subscriptionName = getSubscriptionName(user.userId).bind()

        DriveSpotlightEventDimensions().apply {
            addPlanBeforeUpgrade(subscriptionName.value)
            addDaysSinceAccountCreation(accountAgeInDays)
        }
    }

    private fun onSupervisedScope(block: suspend () -> Unit) {
        scopeProvider.GlobalDefaultSupervisedScope.launch { block() }
    }
}
