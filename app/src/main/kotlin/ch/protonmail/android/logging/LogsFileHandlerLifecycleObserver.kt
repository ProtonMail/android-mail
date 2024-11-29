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

package ch.protonmail.android.logging

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ch.protonmail.android.initializer.LoggerInitializer
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import dagger.hilt.android.EntryPointAccessors

/**
 * A [LifecycleObserver] used to clean up the [LogsFileHandler] instance.
 */
internal class LogsFileHandlerLifecycleObserver(
    context: Context
) : DefaultLifecycleObserver {

    private val logsFileHandler: LogsFileHandler

    init {
        val entryPoint = EntryPointAccessors.fromApplication(
            context, LoggerInitializer.LoggerInitializerEntryPoint::class.java
        )
        logsFileHandler = entryPoint.logsFileHandlerProvider()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        logsFileHandler.close()
    }
}
