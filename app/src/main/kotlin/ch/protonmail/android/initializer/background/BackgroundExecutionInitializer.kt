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

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.startup.Initializer
import ch.protonmail.android.initializer.WorkManagerInitializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

internal class BackgroundExecutionInitializer : Initializer<Unit> {

    override fun create(context: Context) {

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BackgroundExecutionEntryPoint::class.java
        )

        val lifecycle = ProcessLifecycleOwner.Companion.get().lifecycle
        lifecycle.addObserver(entryPoint.lifecycleObserver())
        lifecycle.addObserver(entryPoint.contentIndexingLifecycleObserver())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(WorkManagerInitializer::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackgroundExecutionEntryPoint {

        fun lifecycleObserver(): RustWorkLifecycleObserver

        fun contentIndexingLifecycleObserver(): ContentIndexingLifecycleObserver
    }
}
