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

package ch.protonmail.android.navigation.deeplinks

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsDeepLinkHandler @Inject constructor() {

    private val pendingChannel = Channel<NotificationsDeepLinkData>(capacity = Channel.UNLIMITED)
    private val isUnlocked = MutableStateFlow(false)
    private val hasPending = AtomicBoolean(false)

    val pending: Flow<NotificationsDeepLinkData> = pendingChannel
        .receiveAsFlow()
        .transform { data ->
            isUnlocked.first { it }
            Timber.d("DeepLinkHandler: Emitting pending deep link - $data")
            emit(data)
        }

    fun hasPending(): Boolean = hasPending.get()

    fun setPending(data: NotificationsDeepLinkData) {
        Timber.d("DeepLinkHandler: setPending called with $data, isUnlocked=${isUnlocked.value}")
        hasPending.set(true)
        pendingChannel.trySend(data)
    }

    fun setLocked() {
        isUnlocked.value = false
    }

    fun setUnlocked() {
        isUnlocked.value = true
    }

    fun consume() {
        hasPending.set(false)
    }
}
