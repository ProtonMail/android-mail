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

package ch.protonmail.android.callbacks

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import ch.protonmail.android.LockScreenActivity
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ShouldPresentPinInsertionScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AutoLockLifecycleCallbacks @Inject constructor(
    private val shouldPresentPinInsertionScreen: ShouldPresentPinInsertionScreen,
    @AppScope private val coroutineScope: CoroutineScope
) : Application.ActivityLifecycleCallbacks {

    private var job: Job? = null

    override fun onActivityResumed(activity: Activity) {
        if (activity is LockScreenActivity) return

        job = coroutineScope.launch {
            shouldPresentPinInsertionScreen().collectLatest { forcePinInsertion ->
                if (!forcePinInsertion) return@collectLatest
                val intent = Intent(activity, LockScreenActivity::class.java)
                activity.startActivity(intent)
            }
        }
    }

    override fun onActivityPaused(p0: Activity) {
        job?.cancel()
        job = null
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) = Unit
    override fun onActivityStarted(p0: Activity) = Unit
    override fun onActivityStopped(p0: Activity) = Unit
    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit
    override fun onActivityDestroyed(p0: Activity) = Unit
}
