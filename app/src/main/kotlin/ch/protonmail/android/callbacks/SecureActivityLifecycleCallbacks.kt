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
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.view.WindowManager
import arrow.core.getOrElse
import ch.protonmail.android.LockScreenActivity
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObservePreventScreenshotsSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.auth.presentation.ui.AuthActivity
import me.proton.core.usersettings.presentation.ui.PasswordManagementActivity
import timber.log.Timber
import javax.inject.Inject

internal class SecureActivityLifecycleCallbacks @Inject constructor(
    private val observePreventScreenshotsSetting: ObservePreventScreenshotsSetting,
    @AppScope private val coroutineScope: CoroutineScope
) : ActivityLifecycleCallbacks {

    private var setSecureJob: Job? = null

    override fun onActivityResumed(activity: Activity) {
        // Regardless of the user-defined setting, a subset of activities will always be secure.
        if (activity.isSecureActivity()) {
            setSecureFlags(activity)
            return
        }

        setSecureJob = coroutineScope.launch {
            observePreventScreenshotsSetting().collectLatest {
                val preventTakingScreenshotsPreference = it.getOrElse {
                    Timber.e("Unable to get 'Prevent taking screenshots' setting.")
                    return@collectLatest
                }

                withContext(Dispatchers.Main) {
                    if (preventTakingScreenshotsPreference.isEnabled) {
                        setSecureFlags(activity)
                    } else {
                        clearSecureFlags(activity)
                    }
                }
            }
        }
    }

    override fun onActivityPaused(p0: Activity) {
        setSecureJob?.cancel()
        setSecureJob = null
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) = Unit
    override fun onActivityStarted(p0: Activity) = Unit
    override fun onActivityStopped(p0: Activity) = Unit
    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) = Unit
    override fun onActivityDestroyed(p0: Activity) = Unit

    private fun setSecureFlags(activity: Activity) {
        activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun clearSecureFlags(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun Activity.isSecureActivity(): Boolean = when (this) {
        is AuthActivity<*>,
        is PasswordManagementActivity,
        is LockScreenActivity -> true

        else -> false
    }
}
