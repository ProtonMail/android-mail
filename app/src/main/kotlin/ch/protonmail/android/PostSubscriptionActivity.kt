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
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.navigation.listener.withDestinationChangedObservableEffect
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.route.addPostSubscription
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.compose.withSentryObservableEffect
import me.proton.core.compose.theme.ProtonTheme

@AndroidEntryPoint
class PostSubscriptionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProtonTheme {
                val navController = rememberNavController()
                    .withSentryObservableEffect()
                    .withDestinationChangedObservableEffect()

                NavHost(
                    modifier = Modifier.fillMaxSize(),
                    navController = navController,
                    startDestination = Destination.Screen.PostSubscription.route
                ) {
                    addPostSubscription(
                        onClose = { this@PostSubscriptionActivity.finish() }
                    )
                }

                navController.navigate(
                    Destination.Screen.PostSubscription.route
                )
            }
        }
    }
}
