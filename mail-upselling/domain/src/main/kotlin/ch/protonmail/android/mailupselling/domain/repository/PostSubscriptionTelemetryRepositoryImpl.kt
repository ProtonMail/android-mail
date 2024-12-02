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

import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEvent
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventDimensions
import ch.protonmail.android.mailupselling.domain.model.telemetry.postsubscription.PostSubscriptionTelemetryEventType
import ch.protonmail.android.mailupselling.domain.usecase.featureflags.IsPostSubscriptionTelemetryEnabled
import kotlinx.coroutines.launch
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryEvent
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject

class PostSubscriptionTelemetryRepositoryImpl @Inject constructor(
    private val getPrimaryUser: GetPrimaryUser,
    private val isPostSubscriptionTelemetryEnabled: IsPostSubscriptionTelemetryEnabled,
    private val telemetryManager: TelemetryManager,
    private val scopeProvider: CoroutineScopeProvider
) : PostSubscriptionTelemetryRepository {

    override fun trackEvent(eventType: PostSubscriptionTelemetryEventType) {
        scopeProvider.GlobalDefaultSupervisedScope.launch {

            val user = getPrimaryUser() ?: return@launch

            if (!isPostSubscriptionTelemetryEnabled(user.userId)) return@launch

            val event = when (eventType) {
                is PostSubscriptionTelemetryEventType.LastStepDisplayed -> getLastStepDisplayedEvent()
                is PostSubscriptionTelemetryEventType.DownloadApp -> getDownloadAppEvent(eventType.packageName)
            }

            telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate)
        }
    }

    private fun getLastStepDisplayedEvent() = PostSubscriptionTelemetryEvent.LastStepDisplayed(
        PostSubscriptionTelemetryEventDimensions()
    ).toTelemetryEvent()

    private fun getDownloadAppEvent(packageName: String): TelemetryEvent {
        val dimensions = PostSubscriptionTelemetryEventDimensions()
        val appName = when (packageName) {
            PACKAGE_NAME_CALENDAR -> "android_calendar"
            PACKAGE_NAME_DRIVE -> "android_drive"
            PACKAGE_NAME_VPN -> "android_vpn"
            PACKAGE_NAME_PASS -> "android_pass"
            else -> null
        }
        dimensions.addSelectedApp(appName!!)
        return PostSubscriptionTelemetryEvent.DownloadApp(dimensions).toTelemetryEvent()
    }
}

private const val PACKAGE_NAME_CALENDAR = "me.proton.android.calendar"
private const val PACKAGE_NAME_DRIVE = "me.proton.android.drive"
private const val PACKAGE_NAME_VPN = "ch.protonvpn.android"
private const val PACKAGE_NAME_PASS = "proton.android.pass"
