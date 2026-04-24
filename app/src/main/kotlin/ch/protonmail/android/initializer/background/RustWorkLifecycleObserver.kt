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

package ch.protonmail.android.initializer.background

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.mailsession.data.background.BackgroundExecutionWorkScheduler
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class RustWorkLifecycleObserver @Inject constructor(
    private val mailSessionRepository: MailSessionRepository,
    private val backgroundExecutionWorkScheduler: BackgroundExecutionWorkScheduler
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        owner.lifecycleScope.launch {
            backgroundExecutionWorkScheduler.cancelPendingWork()
            onRustEnterForeground()
            Timber.d("onStart finished - pending work canceled + onEnterForeground")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        onRustExitForeground()
        backgroundExecutionWorkScheduler.scheduleWork()
        Timber.d("onStop finished - onExitForeground + schedule work called")
    }

    private fun onRustExitForeground() {
        mailSessionRepository.getMailSession().onExitForeground()
    }

    private fun onRustEnterForeground() {
        mailSessionRepository.getMailSession().onEnterForeground()
    }
}
