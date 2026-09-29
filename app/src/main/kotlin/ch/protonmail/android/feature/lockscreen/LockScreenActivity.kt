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

package ch.protonmail.android.feature.lockscreen

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.AutoLockUnlockSignal
import ch.protonmail.android.mailpinlock.domain.AutoLockCheckPendingState
import ch.protonmail.android.mailpinlock.domain.AutoLockRepository
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class LockScreenActivity : AppCompatActivity() {

    @Inject
    lateinit var autoLockUnlockSignal: AutoLockUnlockSignal

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Timber.d("auto-lock: Lock screen created")

        initializeLockState()
        setupBackPressBlocking()
        observeUnlockSignal()

        setContent {
            LockScreenContent(onClose = { closeWithoutUnlockSignal() })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lockScreenState.setActive(false)
    }

    private fun initializeLockState() {
        deepLinkHandler.setLocked()
        lockScreenState.setActive(true)
    }

    private fun observeUnlockSignal() {
        lifecycleScope.launch {
            autoLockUnlockSignal.unlockSignal.collect {
                deepLinkHandler.setUnlocked()

                if (shouldLaunchMainActivity()) {
                    autoLockCheckPendingState.skipNextAutoLockCheck()
                    launchMainActivity()
                }

                finish()
            }
        }
    }

    /**
     * The lock screen can be closed without an unlock (e.g. app settings failing to load, or back pressed while
     * the PIN screen is loading). Only reveal what is underneath when the session is actually unlocked,
     * otherwise close the app so the next launch goes through the lock screen again.
     */
    private fun closeWithoutUnlockSignal() {
        if (isFinishing) return

        lifecycleScope.launch {
            val isStillLocked = autoLockRepository.shouldAutoLock().getOrElse { true }

            if (isStillLocked) {
                Timber.d("auto-lock: Lock screen closed while still locked, closing the app")
                finishAffinity()
            } else {
                finish()
            }
        }
    }

    private fun shouldLaunchMainActivity() = isTaskRoot || deepLinkHandler.hasPending()

    private fun launchMainActivity() {
        createLaunchIntent()?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }?.let { startActivity(it) }
    }

    private fun setupBackPressBlocking() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            }
        )
    }
}
