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

package ch.protonmail.android.mailnotifications.data.repository

import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEvent
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventDimensions
import ch.protonmail.android.mailnotifications.domain.model.telemetry.NotificationPermissionTelemetryEventType
import ch.protonmail.android.mailnotifications.presentation.model.NotificationPermissionDialogType
import kotlinx.coroutines.launch
import me.proton.core.auth.domain.usecase.GetPrimaryUser
import me.proton.core.telemetry.domain.TelemetryManager
import me.proton.core.telemetry.domain.entity.TelemetryEvent
import me.proton.core.telemetry.domain.entity.TelemetryPriority
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject

class NotificationPermissionTelemetryRepositoryImpl @Inject constructor(
    private val getPrimaryUser: GetPrimaryUser,
    private val telemetryManager: TelemetryManager,
    private val scopeProvider: CoroutineScopeProvider
) : NotificationPermissionTelemetryRepository {

    override fun trackEvent(eventType: NotificationPermissionTelemetryEventType) {
        scopeProvider.GlobalDefaultSupervisedScope.launch {

            val user = getPrimaryUser() ?: return@launch

            val event = when (eventType) {
                is NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed ->
                    getDisplayedEvent(eventType)
                is NotificationPermissionTelemetryEventType.NotificationPermissionDialogEnable ->
                    getEnableEvent(eventType)
                is NotificationPermissionTelemetryEventType.NotificationPermissionDialogDismiss ->
                    getDismissEvent(eventType)
            }

            telemetryManager.enqueue(user.userId, event, TelemetryPriority.Immediate)
        }
    }

    private fun getDisplayedEvent(
        eventType: NotificationPermissionTelemetryEventType.NotificationPermissionDialogDisplayed
    ): TelemetryEvent {
        val dimensions = getDimensions(eventType.notificationPermissionDialogType)
        return NotificationPermissionTelemetryEvent.NotificationPermissionDialogDisplayed(dimensions).toTelemetryEvent()
    }

    private fun getEnableEvent(
        eventType: NotificationPermissionTelemetryEventType.NotificationPermissionDialogEnable
    ): TelemetryEvent {
        val dimensions = getDimensions(eventType.notificationPermissionDialogType)
        return NotificationPermissionTelemetryEvent.NotificationPermissionDialogEnable(dimensions).toTelemetryEvent()
    }

    private fun getDismissEvent(
        eventType: NotificationPermissionTelemetryEventType.NotificationPermissionDialogDismiss
    ): TelemetryEvent {
        val dimensions = getDimensions(eventType.notificationPermissionDialogType)
        return NotificationPermissionTelemetryEvent.NotificationPermissionDialogDismiss(dimensions).toTelemetryEvent()
    }

    private fun getDimensions(
        notificationPermissionDialogType: NotificationPermissionDialogType
    ): NotificationPermissionTelemetryEventDimensions {
        val dimensions = NotificationPermissionTelemetryEventDimensions()
        val dialogType = when (notificationPermissionDialogType) {
            NotificationPermissionDialogType.PostOnboarding -> "post_onboarding"
            NotificationPermissionDialogType.PostSending -> "post_sending"
        }
        dimensions.addNotificationPermissionDialogType(dialogType)
        return dimensions
    }
}
