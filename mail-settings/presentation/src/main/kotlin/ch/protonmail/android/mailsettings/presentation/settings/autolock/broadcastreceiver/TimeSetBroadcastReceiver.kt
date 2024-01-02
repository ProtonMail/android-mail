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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.broadcastreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailsettings.domain.usecase.autolock.IsAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockAttemptPendingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class TimeSetBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject
    lateinit var isAutoLockEnabled: IsAutoLockEnabled

    @Inject
    lateinit var toggleAutoLockPendingAttempt: ToggleAutoLockAttemptPendingStatus

    @Inject
    @AppScope
    lateinit var coroutineScope: CoroutineScope

    override fun onReceive(context: Context?, intent: Intent?) {
        coroutineScope.launch {
            if (!isAutoLockEnabled()) return@launch
            toggleAutoLockPendingAttempt(value = true)
        }
    }
}
