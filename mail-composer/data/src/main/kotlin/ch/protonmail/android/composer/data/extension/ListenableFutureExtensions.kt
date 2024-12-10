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

package ch.protonmail.android.composer.data.extension

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * An alternative to [ListenableFuture]'s `await`, which usage is restricted when outside of [androidx.work] group.
 */
@Suppress("BlockingMethodInNonBlockingContext", "TooGenericExceptionCaught")
suspend fun <T> ListenableFuture<T>.awaitCompletion(): T = suspendCancellableCoroutine { continuation ->
    addListener({
        try {
            continuation.resume(get())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }, Dispatchers.Default.asExecutor())

    continuation.invokeOnCancellation {
        if (!isDone) {
            cancel(true)
        }
    }
}
