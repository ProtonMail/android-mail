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

package ch.protonmail.android.navigation.transitions

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import ch.protonmail.android.navigation.model.Destination

data class RouteTransitionSpec(
    val enter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    val exit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    val popEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    val popExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null
) {

    companion object {

        val None = RouteTransitionSpec()

        /**
         * M3 "Top level" / utility focused transition:
         * Cross-fade (no spatial relationship).
         */
        val Default = RouteTransitionSpec(
            enter = {
                RouteTransitions.defaultEnterTransition()
            },
            exit = {
                RouteTransitions.defaultExitTransition()
            },
            popEnter = {
                RouteTransitions.defaultPopEnterTransition()
            },
            popExit = {
                RouteTransitions.defaultPopExitTransition()
            }
        )

        /**
         * M3: Navigation drawer destination change is also a "Top level" transition.
         * Do NOT slide the whole screen (that implies hierarchy/relationship).
         */
        val Drawer = Default

        /**
         * Composer opened from FAB behaves like a bottom sheet:
         * Enter/exit from bottom (beyond bounds).
         */
        val ComposerFromFab = RouteTransitionSpec(
            enter = { RouteTransitions.enterFromBottom() },
            exit = { RouteTransitions.defaultExitTransition() },
            popEnter = { RouteTransitions.defaultPopEnterTransition() },
            popExit = { RouteTransitions.exitToBottom() }
        )

        val ForwardBack = RouteTransitionSpec(
            enter = {
                RouteTransitions.pushEnterFromRight()
            },
            exit = {
                RouteTransitions.pushExitToLeft()
            },
            popEnter = {
                RouteTransitions.popEnterFromLeft()
            },
            popExit = {
                RouteTransitions.popExitToRight()
            }
        )

        val ComposerFromDrafts = ForwardBack

        val Mailbox = Default.copy(
            exit = {
                whenTargetRoute { route ->
                    when (route) {
                        Destination.Screen.Conversation.route -> {
                            RouteTransitions.pushExitToLeft()
                        }

                        else -> {
                            RouteTransitions.defaultExitTransition()
                        }
                    }
                }
            },
            popEnter = {
                whenInitialRoute { route ->
                    when (route) {
                        Destination.Screen.Conversation.route -> {
                            RouteTransitions.popEnterFromRight()
                        }

                        else -> {
                            RouteTransitions.defaultPopEnterTransition()
                        }
                    }
                }
            }
        )

        val Conversation = Default.copy(
            enter = {

                if (initialState.destination.route == Destination.Screen.Mailbox.route) {
                    RouteTransitions.pushEnterFromRight()
                } else {
                    RouteTransitions.defaultEnterTransition()
                }
            },
            popExit = {

                if (targetState.destination.route == Destination.Screen.Mailbox.route) {
                    RouteTransitions.popExitToRight()
                } else {
                    RouteTransitions.defaultPopExitTransition()
                }
            }
        )

        val Contacts = Default.copy(
            exit = {
                whenTargetRoute { route ->
                    when (route) {
                        Destination.Screen.ContactDetails.route -> {
                            RouteTransitions.pushExitToLeft()
                        }

                        else -> {
                            RouteTransitions.defaultExitTransition()
                        }
                    }
                }
            },
            popEnter = {
                whenInitialRoute { route ->
                    when (route) {
                        Destination.Screen.ContactDetails.route -> {
                            RouteTransitions.popEnterFromRight()
                        }

                        else -> {
                            RouteTransitions.defaultPopEnterTransition()
                        }
                    }
                }
            }
        )

        val ContactSearch = Default.copy(
            popEnter = {
                whenInitialRoute { route ->
                    when (route) {
                        Destination.Screen.ContactDetails.route -> {
                            RouteTransitions.popEnterFromRight()
                        }

                        else -> {
                            RouteTransitions.defaultPopEnterTransition()
                        }
                    }
                }
            }
        )

        val ContactDetails = Default.copy(
            enter = {
                if (initialState.destination.route == Destination.Screen.Contacts.route ||
                    initialState.destination.route == Destination.Screen.ContactSearch.route
                ) {
                    RouteTransitions.pushEnterFromRight()
                } else {
                    RouteTransitions.defaultEnterTransition()
                }
            },
            popExit = {

                if (targetState.destination.route == Destination.Screen.Contacts.route ||
                    targetState.destination.route == Destination.Screen.ContactSearch.route
                ) {
                    RouteTransitions.popExitToRight()
                } else {
                    RouteTransitions.defaultPopExitTransition()
                }
            }
        )

        val Settings = Drawer.copy(
            popEnter = {
                whenInitialRoute { route ->
                    when (route) {
                        Destination.Screen.AccountSettings.route,
                        Destination.Screen.AppSettings.route,
                        Destination.Screen.EmailSettings.route,
                        Destination.Screen.FolderAndLabelSettings.route,
                        Destination.Screen.SpamFilterSettings.route,
                        Destination.Screen.PrivacyAndSecuritySettings.route,
                        Destination.Screen.SignatureSettingsMenu.route -> {
                            RouteTransitions.popEnterFromRight()
                        }

                        else -> {
                            RouteTransitions.defaultPopEnterTransition()
                        }
                    }
                }
            }
        )

        val SettingsSubScreen = Default.copy(
            enter = {
                if (initialState.destination.route == Destination.Screen.Settings.route) {
                    RouteTransitions.pushEnterFromRight()
                } else {
                    RouteTransitions.defaultEnterTransition()
                }
            },
            popExit = {
                if (targetState.destination.route == Destination.Screen.Settings.route) {
                    RouteTransitions.popExitToRight()
                } else {
                    RouteTransitions.defaultPopExitTransition()
                }
            }
        )

        val SignatureSettingsMenu = SettingsSubScreen.copy(
            popEnter = {
                whenInitialRoute { route ->
                    when (route) {
                        Destination.Screen.MobileSignatureSettings.route,
                        Destination.Screen.EmailSignatureSettings.route -> {
                            RouteTransitions.popEnterFromRight()
                        }

                        else -> {
                            RouteTransitions.defaultPopEnterTransition()
                        }
                    }
                }
            }
        )
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.targetRoute(): String? = targetState.destination.route

private fun AnimatedContentTransitionScope<NavBackStackEntry>.initialRoute(): String? = initialState.destination.route

private inline fun AnimatedContentTransitionScope<NavBackStackEntry>.whenTargetRoute(
    block: (String?) -> ExitTransition?
): ExitTransition? = block(targetRoute())

private inline fun AnimatedContentTransitionScope<NavBackStackEntry>.whenInitialRoute(
    block: (String?) -> EnterTransition?
): EnterTransition? = block(initialRoute())
