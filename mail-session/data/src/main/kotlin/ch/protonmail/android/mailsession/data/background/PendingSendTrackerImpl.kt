/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.background

import ch.protonmail.android.mailsession.domain.background.PendingSendTracker
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingSendTrackerImpl @Inject constructor() : PendingSendTracker {

    private val pending = ConcurrentHashMap.newKeySet<String>()

    override fun onSendStarted(messageId: String) {
        pending.add(messageId)
        Timber.d("Send Tracker: send started messageId=$messageId, pending=${pending.size}")
    }

    override fun onSendCompleted(messageId: String) {
        val removed = pending.remove(messageId)
        Timber.d("Send Tracker: send completed messageId=$messageId removed=$removed, pending=${pending.size}")
    }

    override fun hasPendingSends(): Boolean = pending.isNotEmpty()

    override fun reset() {
        if (pending.isNotEmpty()) Timber.d("Send Tracker: reset, clearing pending=$pending")
        pending.clear()
    }
}
