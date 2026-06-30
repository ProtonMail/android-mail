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
import androidx.work.WorkManager
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingWorker
import ch.protonmail.android.mailcontentsearch.domain.usecase.DisableContentSearch
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ContentIndexingCancelReceiver : BroadcastReceiver() {

    @Inject
    lateinit var disableContentSearch: DisableContentSearch

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActionCancel) return
        val userIdValue = intent.getStringExtra(ExtraUserId)?.takeIf { it.isNotBlank() }
        if (userIdValue == null) {
            Timber.d("ContentIndexingCancelReceiver: userIdValue == null, canceling unique work")
            WorkManager.getInstance(context).cancelUniqueWork(ContentIndexingWorker.UniqueName)
            return
        }
        val pending = goAsync()
        @Suppress("TooGenericExceptionCaught")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(WorkTimeoutMillis.milliseconds) {
                    disableContentSearch(UserId(userIdValue))
                }
            } catch (t: Throwable) {
                Timber.w(t, "ContentIndexingCancelReceiver: failed to disable content search")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ActionCancel = "ch.protonmail.android.mailcontentsearch.action.CANCEL_INDEXING"
        const val ExtraUserId = "ch.protonmail.android.mailcontentsearch.extra.USER_ID"

        private const val WorkTimeoutMillis = 8_000L
    }
}
