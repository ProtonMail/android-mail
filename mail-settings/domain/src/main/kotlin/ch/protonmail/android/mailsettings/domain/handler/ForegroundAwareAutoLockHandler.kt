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

package ch.protonmail.android.mailsettings.domain.handler

import java.time.Instant
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailsettings.domain.usecase.autolock.HasAutoLockPendingAttempt
import ch.protonmail.android.mailsettings.domain.usecase.autolock.IsAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateLastForegroundMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ForegroundAwareAutoLockHandler @Inject constructor(
    private val appInBackgroundState: AppInBackgroundState,
    private val updateAutoLockLastForegroundMillis: UpdateLastForegroundMillis,
    private val hasAutoLockPendingAttempt: HasAutoLockPendingAttempt,
    private val isAutoLockEnabled: IsAutoLockEnabled,
    @AppScope private val coroutineScope: CoroutineScope
) {

    fun handle() {
        coroutineScope.launch {
            appInBackgroundState.observe().collectLatest { isInBackground ->
                if (!isInBackground) return@collectLatest
                if (!isAutoLockEnabled()) return@collectLatest

                if (hasAutoLockPendingAttempt()) {
                    Timber.d("Auto Lock last foreground millis NOT updated, pending attempt still present.")
                    return@collectLatest
                }

                updateAutoLockLastForegroundMillis(Instant.now().toEpochMilli()).mapLeft {
                    Timber.e("Unable to update Auto Lock last foreground millis - $it.")
                }

                Timber.d("Auto Lock last foreground millis updated.")
            }
        }
    }
}
