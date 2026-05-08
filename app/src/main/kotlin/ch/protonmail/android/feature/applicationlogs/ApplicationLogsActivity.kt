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

package ch.protonmail.android.feature.applicationlogs

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsPeekView
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsScreen
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.navigation.model.Destination.Screen
import dagger.hilt.android.AndroidEntryPoint

// This is a public variant of [DebugActivity] accessible from entry points
// where no nav controller can wire up the navigation (e.g. from the Login screen in account)
@AndroidEntryPoint
class ApplicationLogsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtonTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.ApplicationLogs.route
                ) {
                    composable(route = Screen.ApplicationLogs.route) {
                        ApplicationLogsScreen(
                            actions = ApplicationLogsScreen.Actions(
                                onBackClick = { finish() },
                                onViewItemClick = {},
                                onFeatureFlagsNavigation = {}
                            )
                        )
                    }
                    composable(route = Screen.ApplicationLogsView.route) {
                        ApplicationLogsPeekView(
                            onBack = { navController.navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
