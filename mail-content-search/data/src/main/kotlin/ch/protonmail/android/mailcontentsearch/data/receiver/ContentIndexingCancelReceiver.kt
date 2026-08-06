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

package ch.protonmail.android.mailcontentsearch.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.domain.usecase.StopContentIndexing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles the notification's Pause action.
 *
 * Stopping the orchestrator is an actor round-trip that has to happen inside a Rust background
 * scope, which is far more than [onReceive] is allowed to block for - so the work is handed to the
 * app scope and `onReceive` returns immediately.
 */
@AndroidEntryPoint
class ContentIndexingCancelReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stopContentIndexing: StopContentIndexing

    @Inject
    lateinit var workScheduler: ContentIndexingWorkScheduler

    @Inject
    @AppScope
    lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActionCancel) return

        Timber.d("content-search: pausing indexing from the notification")

        appScope.launch {
            stopContentIndexing().onLeft { Timber.w("content-search: could not pause indexing: $it") }
            // The worker exits on the resulting Stopped event, but cancel it too in case the
            // orchestrator never publishes one.
            workScheduler.cancel()
        }
    }

    companion object {
        const val ActionCancel = "ch.protonmail.android.mailcontentsearch.action.CANCEL_INDEXING"
    }
}
