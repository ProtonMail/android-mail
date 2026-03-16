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
import ch.protonmail.android.mailcommon.presentation.AutoLockUnlockSignal
import ch.protonmail.android.mailpinlock.domain.AutoLockCheckPendingState
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class LockScreenActivity : AppCompatActivity() {

    @Inject
    lateinit var autoLockUnlockSignal: AutoLockUnlockSignal

    @Inject
    lateinit var autoLockCheckPendingState: AutoLockCheckPendingState

    @Inject
    lateinit var createLaunchIntent: CreateLaunchIntent

    @Inject
    lateinit var deepLinkHandler: NotificationsDeepLinkHandler

    @Inject
    lateinit var lockScreenState: LockScreenState

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initializeLockState()
        setupBackPressBlocking()
        observeUnlockSignal()

        setContent {
            LockScreenContent(onClose = { this@LockScreenActivity.finish() })
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
