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

package ch.protonmail.android.feature.forceupdate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.presentation.app.AppLifecycleObserver
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.presentation.ui.alert.ForceUpdateActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForceUpdateHandler @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appLifecycleObserver: AppLifecycleObserver
) {
    fun onForceUpdate(errorMessage: String) {
        if (appLifecycleObserver.state.value == AppLifecycleProvider.State.Foreground) {
            startForceUpdateActivity(errorMessage)
        }
    }

    private fun startForceUpdateActivity(errorMessage: String) {
        context.startActivity(ForceUpdateActivity(context, errorMessage))
    }
}
