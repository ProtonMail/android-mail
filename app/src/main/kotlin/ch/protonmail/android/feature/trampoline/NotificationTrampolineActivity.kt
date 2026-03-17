/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.feature.trampoline

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.feature.lockscreen.LockScreenActivity
import ch.protonmail.android.feature.lockscreen.LockScreenState
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailpinlock.domain.AutoLockCheckPendingState
import ch.protonmail.android.mailpinlock.domain.AutoLockRepository
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkData
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkHandler
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class NotificationTrampolineActivity : AppCompatActivity() {

    @Inject
    lateinit var autoLockCheckPendingState: AutoLockCheckPendingState

    @Inject
    lateinit var autoLockRepository: AutoLockRepository

    @Inject
    lateinit var createLaunchIntent: CreateLaunchIntent

    @Inject
    lateinit var deepLinkHandler: NotificationsDeepLinkHandler

    @Inject
    lateinit var lockScreenState: LockScreenState

    @Inject
    lateinit var appInBackgroundState: AppInBackgroundState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Skip the auto-lock check that AutoLockInitializer will trigger on ON_RESUME.
        // The Trampoline handles lock screen logic itself and the redundant check
        // would race with handleNotificationNavigation(), causing a double biometric prompt.
        autoLockCheckPendingState.skipNextAutoLockCheck()

        lifecycleScope.launch {
            handleNotificationNavigation()
            finish()
        }
    }

    private suspend fun handleNotificationNavigation() {
        val wasInBackground = appInBackgroundState.isAppInBackground()
        val lockScreenAlreadyActive = lockScreenState.isShowing.value
        val needsLockScreen = autoLockRepository.shouldAutoLock().getOrNull() ?: false
        val deepLinkData = NotificationsDeepLinkParser.parseUri(intent.data)

        when {
            lockScreenAlreadyActive -> handleLockScreenActive(deepLinkData)
            wasInBackground && needsLockScreen -> handleBackgroundWithLock(deepLinkData)
            wasInBackground -> handleBackgroundNoLock(deepLinkData)
            else -> handleForegroundNoLock(deepLinkData)
        }
    }

    private fun handleLockScreenActive(deepLinkData: NotificationsDeepLinkData?) {
        Timber.d("Trampoline: Lock screen active, storing deep link")
        deepLinkData?.let { deepLinkHandler.setPending(it) }
        launchLockScreen()
    }

    private fun handleBackgroundWithLock(deepLinkData: NotificationsDeepLinkData?) {
        Timber.d("Trampoline: Background + needs lock, launching lock screen")
        lockScreenState.setActive(true)
        deepLinkHandler.setLocked()
        deepLinkData?.let { deepLinkHandler.setPending(it) }
        launchLockScreen()
    }

    private fun handleBackgroundNoLock(deepLinkData: NotificationsDeepLinkData?) {
        Timber.d("Trampoline: Background + no lock, navigating directly")
        deepLinkHandler.setUnlocked()
        deepLinkData?.let { deepLinkHandler.setPending(it) }
        launchMainActivity()
    }

    private fun handleForegroundNoLock(deepLinkData: NotificationsDeepLinkData?) {
        Timber.d("Trampoline: Foreground + no lock, signaling in-app navigation")
        deepLinkHandler.setUnlocked()
        deepLinkData?.let { deepLinkHandler.setPending(it) }
    }

    private fun launchLockScreen() {
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(lockIntent)
    }

    private fun launchMainActivity() {
        createLaunchIntent()?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }?.let { startActivity(it) }
    }
}
