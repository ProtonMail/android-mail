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

package ch.protonmail.android.maildetail.presentation.usecase

import android.content.Context
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

class ClearMessageBodyCache @Inject constructor(
    @ApplicationContext private val context: Context,
    @IODispatcher private val coroutineDispatcher: CoroutineDispatcher
) {

    suspend operator fun invoke() {
        runCatching {
            withContext(coroutineDispatcher) {
                val cacheDir = File(context.cacheDir, BODY_CACHE_DIRECTORY)
                Timber.d("message-body-cache: Clearing cache directory children")

                if (!cacheDir.exists()) return@withContext

                cacheDir.listFiles()?.forEach { child ->
                    if (!child.deleteRecursively()) {
                        Timber.w("message-body-cache: Failed to delete child ${child.name}")
                    }
                }
            }
        }.onFailure {
            Timber.e(it, "message-body-cache: Error clearing cache")
        }
    }


    companion object {

        internal const val BODY_CACHE_DIRECTORY = "body_cache"
    }
}
