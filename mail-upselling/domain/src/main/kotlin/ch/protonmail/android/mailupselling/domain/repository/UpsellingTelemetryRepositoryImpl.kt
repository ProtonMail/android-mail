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
import ch.protonmail.android.mailupselling.domain.annotations.OneClickUpsellingTelemetryEnabled
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.getDimensionValue
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEvent.UpsellButtonTapped
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType.Base
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType.Upgrade
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryTargetPlanPayload
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
class UpsellingTelemetryRepositoryImpl @Inject constructor(
    private val getAccountAgeInDays: GetAccountAgeInDays,
    private val getPrimaryUser: GetPrimaryUser,
    private val getSubscriptionName: GetSubscriptionName,
    private val telemetryManager: TelemetryManager,
    @OneClickUpsellingTelemetryEnabled private val isOneClickTelemetryEnabled: Boolean,
    private val scopeProvider: CoroutineScopeProvider
) : UpsellingTelemetryRepository {

    // Note that the user preference check is delegated to the TelemetryManager from core.
    // If the user has opted out, we will discard the event and not send it.
    override fun trackEvent(eventType: UpsellingTelemetryEventType, upsellingEntryPoint: UpsellingEntryPoint) =
        onSupervisedScope {
            if (!isOneClickTelemetryEnabled) return@onSupervisedScope
            val user = getPrimaryUser() ?: return@onSupervisedScope

            val event = when (eventType) {
                is Base -> createBaseEvent(eventType, user, upsellingEntryPoint)
                is Upgrade -> createUpgradeEvent(eventType, user, upsellingEntryPoint)
            }.getOrElse { return@onSupervisedScope }

            telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate)
        }

    private suspend fun createBaseEvent(
        event: Base,
        user: User,
        upsellingEntryPoint: UpsellingEntryPoint
    ) = either {
        val dimensions = buildTelemetryDimensions(user, upsellingEntryPoint).getOrElse {
            raise(CreateTelemetryEventError)
        }

        when (event) {
            Base.MailboxButtonTap -> UpsellButtonTapped(dimensions).toTelemetryEvent()
            Base.NavbarButtonTap -> UpsellButtonTapped(dimensions).toTelemetryEvent()
        }
    }

    private suspend fun createUpgradeEvent(
        event: Upgrade,
        user: User,
        upsellingEntryPoint: UpsellingEntryPoint
    ) = either {
        val dimensions = buildTelemetryDimensions(user, event.payload, upsellingEntryPoint).getOrElse {
            raise(CreateTelemetryEventError)
        }

        when (event) {
            is Upgrade.UpgradeAttempt -> UpsellingTelemetryEvent.UpgradeAttempt(dimensions)
            is Upgrade.UpgradeCancelled -> UpsellingTelemetryEvent.UpgradeCancelled(dimensions)
            is Upgrade.UpgradeErrored -> UpsellingTelemetryEvent.UpgradeErrored(dimensions)
            is Upgrade.PurchaseCompleted -> UpsellingTelemetryEvent.PurchaseCompleted(dimensions)
        }.toTelemetryEvent()
    }

    private suspend fun buildTelemetryDimensions(user: User, upsellingEntryPoint: UpsellingEntryPoint) = either {
        val accountAgeInDays = getAccountAgeInDays(user).toUpsellingTelemetryDimensionValue()
        val subscriptionName = getSubscriptionName(user.userId).bind()

        UpsellingTelemetryEventDimensions().apply {
            addPlanBeforeUpgrade(subscriptionName.value)
            addDaysSinceAccountCreation(accountAgeInDays)
            addUpsellEntryPoint(upsellingEntryPoint.getDimensionValue())
            addUpsellModalVersion()
        }
    }

    private suspend fun buildTelemetryDimensions(
        user: User,
        payload: UpsellingTelemetryTargetPlanPayload,
        upsellingEntryPoint: UpsellingEntryPoint
    ) = either {
        buildTelemetryDimensions(user, upsellingEntryPoint).bind().apply {
            addSelectedPlan(payload.planName)
            addSelectedPlanCycle(payload.planCycle)
            addUpsellPromoVariant(isVariantB = payload.isVariantB, isSocialProofVariant = payload.isSocialProofVariant)
            addUpsellIsPromo(isPromo = payload.isPromotional)
            addUpsellModalVersion()
        }
    }

    private fun onSupervisedScope(block: suspend () -> Unit) {
        scopeProvider.GlobalDefaultSupervisedScope.launch { block() }
    }
}

data object CreateTelemetryEventError
