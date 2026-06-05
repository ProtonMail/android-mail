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

package ch.protonmail.android.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsPeekView
import ch.protonmail.android.mailbugreport.presentation.ui.ApplicationLogsScreen
import ch.protonmail.android.mailbugreport.presentation.ui.LocalAppLogsEntryPointIsStandalone
import ch.protonmail.android.mailcommon.presentation.extension.navigateBack
import ch.protonmail.android.mailfeatureflags.presentation.ui.FeatureFlagOverridesScreen
import ch.protonmail.android.navigation.model.Destination.Screen
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.compose.withSentryObservableEffect

@AndroidEntryPoint
internal class DebugActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtonTheme {
                val navController = rememberNavController().withSentryObservableEffect()

                NavHost(
                    navController = navController,
                    startDestination = Screen.ApplicationDebug.route
                ) {

                    composable(route = Screen.ApplicationDebug.route) {
                        ApplicationDebugScreen(
                            actions = ApplicationDebugScreen.Actions(
                                onBackClick = { finish() },
                                onLogsNavigation = { navController.navigate(Screen.ApplicationLogs.route) },
                                onDangerZoneNavigation = {
                                    navController.navigate(Screen.ApplicationDebugDangerZone.route)
                                },
                                onDatabaseExportNavigation = {
                                    navController.navigate(Screen.ApplicationDebugDatabaseExport.route)
                                }
                            )
                        )
                    }

                    composable(route = Screen.ApplicationDebugDangerZone.route) {
                        DangerZoneScreen(
                            onBackClick = { navController.navigateBack() }
                        )
                    }

                    composable(route = Screen.ApplicationDebugDatabaseExport.route) {
                        DatabaseExportScreen(
                            onBackClick = { navController.navigateBack() }
                        )
                    }

                    composable(route = Screen.ApplicationLogs.route) {
                        CompositionLocalProvider(LocalAppLogsEntryPointIsStandalone provides true) {
                            ApplicationLogsScreen(
                                actions = ApplicationLogsScreen.Actions(
                                    onBackClick = { navController.navigateBack() },
                                    onViewItemClick = { navController.navigate(Screen.ApplicationLogsView(it)) },
                                    onFeatureFlagsNavigation = {
                                        navController.navigate(Screen.FeatureFlagsOverrides.route)
                                    }
                                )
                            )
                        }
                    }
                    composable(route = Screen.ApplicationLogsView.route) {
                        ApplicationLogsPeekView(
                            onBack = { navController.navigateBack() }
                        )
                    }
                    composable(route = Screen.FeatureFlagsOverrides.route) {
                        FeatureFlagOverridesScreen(
                            onBack = { navController.navigateBack() }
                        )
                    }
                }
            }
        }
    }
}
