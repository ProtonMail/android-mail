/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.feature.forceupdate

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.presentation.app.AppLifecycleObserver
import me.proton.core.presentation.app.AppLifecycleProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForceUpdateHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val appState = MutableStateFlow(AppLifecycleProvider.State.Background)

    private val appLifecycleObserver = mockk<AppLifecycleObserver>(relaxUnitFun = true) {
        every { state } returns appState
    }

    lateinit var forceUpdateHandler: ForceUpdateHandler

    @Before
    fun setUp() {
        forceUpdateHandler = ForceUpdateHandler(context, appLifecycleObserver)
    }

    @Test
    fun whenForegroundOnForceUpdate() {
        // GIVEN
        appState.tryEmit(AppLifecycleProvider.State.Foreground)
        // WHEN
        forceUpdateHandler.onForceUpdate("Update")
        // THEN
        verify(atLeast = 1) { context.startActivity(any()) }
    }

    @Test
    fun whenBackgroundOnForceUpdate() {
        // GIVEN
        appState.tryEmit(AppLifecycleProvider.State.Background)
        // WHEN
        forceUpdateHandler.onForceUpdate("Update")
        // THEN
        verify(exactly = 0) { context.startActivity(any()) }
    }
}
