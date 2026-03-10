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

package ch.protonmail.android.uicomponents.fab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * A Fab host where we can lazily change the Fab composable allowing us to delegate the actual implementation of
 * a Fab to individual screens.  Screens wanting to show a fab should use @see LazyFab which will automatically cleanup
 * and remove the fab when the screen moves out of composition
 */
@Composable
fun FabHost(modifier: Modifier = Modifier, fabHostState: ProtonFabHostState) {
    val provider = remember { fabHostState.currentItemProvider }
    provider.value(modifier)
}

@Stable
class ProtonFabHostState {

    internal var currentItemProvider = mutableStateOf<@Composable (Modifier) -> Unit>(nopFabProvider)

    internal fun setFabProvider(fabProvider: @Composable (Modifier) -> Unit) {
        currentItemProvider.value = fabProvider
    }

    companion object {

        val nopFabProvider = @Composable { _: Modifier -> }
    }
}

/**
 * Define a Fab that can be used by a FabHost in the parent screen skeleton.
 * LazyFab will clean up after itself when the Screen leaves composition.
 */
@Composable
fun LazyFab(fabHostState: ProtonFabHostState, fabContent: @Composable (modifier: Modifier) -> Unit) {
    SideEffect {
        fabHostState.setFabProvider { modifier: Modifier ->
            fabContent(modifier)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            fabHostState.setFabProvider(ProtonFabHostState.nopFabProvider)
        }
    }
}



