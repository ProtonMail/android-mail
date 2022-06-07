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

package ch.protonmail.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ch.protonmail.android.navigation.Launcher
import ch.protonmail.android.navigation.LauncherViewModel
import dagger.hilt.android.AndroidEntryPoint
import me.proton.core.compose.theme.ProtonTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            launcherViewModel.state.value != LauncherViewModel.State.PrimaryExist
        }
        super.onCreate(savedInstanceState)

        // Register activities for result.
        launcherViewModel.register(this)

        setContent {
            ProtonTheme {
                Launcher(launcherViewModel)
            }
        }
    }
}
