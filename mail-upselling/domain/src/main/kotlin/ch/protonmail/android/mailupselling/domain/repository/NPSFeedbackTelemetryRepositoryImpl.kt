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

import java.util.TimeZone
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEvent.Skipped
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEvent.SubmitButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.NPSFeedbackTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.data.toUpsellingTelemetryDimensionValue
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.domain.usecase.GetSubscriptionName
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryEvent
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.user.domain.entity.User
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject

@ViewModelScoped
class NPSFeedbackTelemetryRepositoryImpl @Inject constructor(
    private val getAccountAgeInDays: GetAccountAgeInDays,
    private val getPrimaryUser: GetPrimaryUser,
    private val getSubscriptionName: GetSubscriptionName,
    private val telemetryManager: TelemetryManager,
    private val scopeProvider: CoroutineScopeProvider,
    private val getAppLocale: GetAppLocale,
    private val getInstalledProtonApps: GetInstalledProtonApps
) : NPSFeedbackTelemetryRepository {

    override fun trackEvent(eventType: NPSFeedbackTelemetryEventType) = onSupervisedScope {
        val user = getPrimaryUser() ?: return@onSupervisedScope

        val event = createBaseEvent(eventType, user)

        telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate)
    }

    private suspend fun createBaseEvent(event: NPSFeedbackTelemetryEventType, user: User): TelemetryEvent {

        val accountAgeInDays = getAccountAgeInDays(user).toUpsellingTelemetryDimensionValue()
        val subscriptionName = getSubscriptionName(user.userId).getOrNull()

        val dimensions = NPSFeedbackEventDimensions().apply {
            addPlanName(subscriptionName?.value)
            addDaysSinceAccountCreation(accountAgeInDays)
            addTerritory(getAppLocale.invoke(), TimeZone.getDefault())
            addInstalledApps(getInstalledProtonApps())
        }

        return when (event) {
            is NPSFeedbackTelemetryEventType.SubmitTap -> SubmitButtonTapped(
                dimensions.apply {
                    if (event.comment != null) {
                        addComment(event.comment)
                    }
                    addRatingValue(event.ratingValue)
                }
            ).toTelemetryEvent()

            NPSFeedbackTelemetryEventType.Skipped -> Skipped(
                dimensions.apply {
                    addNoRatingValue()
                }
            ).toTelemetryEvent()
        }
    }

    private fun onSupervisedScope(block: suspend () -> Unit) {
        scopeProvider.GlobalDefaultSupervisedScope.launch { block() }
    }
}
