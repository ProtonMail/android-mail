/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.repository.ByoeGuidanceRepository
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Reports BYOE telemetry for [UserId], or for the primary user when none is given.
 *
 * Fire and forget: it runs in the app scope, so a report survives the screen that made it (a connection succeeds
 * while its screen closes). Telemetry is best effort, so a failure is only logged.
 */
class ReportByoeTelemetryEvent @Inject constructor(
    private val byoeGuidanceRepository: ByoeGuidanceRepository,
    private val observePrimaryUserId: ObservePrimaryUserId,
    @AppScope private val appScope: CoroutineScope
) {

    operator fun invoke(event: ByoeTelemetryEvent, userId: UserId? = null) {
        appScope.launch {
            val reportUserId = userId ?: observePrimaryUserId().first()
            if (reportUserId == null) {
                Timber.w("byoe: no user to report $event for")
                return@launch
            }
            byoeGuidanceRepository.report(reportUserId, event)
                .onLeft { Timber.e("byoe: unable to report $event: $it") }
                .onRight { Timber.d("byoe: reported $event") }
        }
    }
}
