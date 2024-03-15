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

package ch.protonmail.android.uitest.helpers.core.navigation

import androidx.test.espresso.Espresso
import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.uitest.helpers.login.MockedLoginTestUsers
import ch.protonmail.android.uitest.robot.common.section.fullscreenLoaderSection
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.mailbox.mailboxRobot
import ch.protonmail.android.uitest.robot.mailbox.section.listSection
import ch.protonmail.android.uitest.robot.mailbox.section.topAppBarSection
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.util.ActivityScenarioHolder
import ch.protonmail.android.uitest.util.extensions.waitUntilSignInScreenIsGone
import me.proton.core.test.android.robots.auth.AddAccountRobot
import me.proton.core.test.android.robots.auth.login.LoginRobot

/**
 * An abstraction to help navigating the app in UI tests to reduce the overall verbosity.
 */
@AsDsl
internal class Navigator {

    private val addAccountRobot = AddAccountRobot()

    /**
     * Triggers the launch of the app and waits for an idle state (via Espresso).
     *
     * The Compose test rule here is not used as the entry point when launching the app
     * will never contain a Compose hierarchy for now (as it's the common Core sign in/up screen).
     */
    fun openApp() {
        ActivityScenarioHolder.initialize()
        Espresso.onIdle()
    }

    /**
     * Navigates to a given [Destination].
     *
     * The navigation shall always be performed at the beginning of the test, as it assumes that the initial state
     * will always either be the "Add account" screen (from the Core library) or the Inbox.
     *
     * @param destination the destination
     * @param launchApp whether the app shall be launched.
     * @param performLoginViaUI whether the login flow shall be performed via UI
     */
    fun navigateTo(
        destination: Destination,
        launchApp: Boolean = true,
        performLoginViaUI: Boolean = true
    ) {
        if (launchApp) openApp()

        if (performLoginViaUI) login()

        when (destination) {
            is Destination.Onboarding,
            is Destination.Inbox -> Unit // It's the default screen post-login, nothing to do.
            is Destination.Drafts -> menuRobot {
                openSidebarMenu()
                openDrafts()
            }

            is Destination.Archive -> menuRobot {
                openSidebarMenu()
                openArchive()
            }

            is Destination.Spam -> menuRobot {
                openSidebarMenu()
                openSpam()
            }

            is Destination.Trash -> menuRobot {
                openSidebarMenu()
                openTrash()
            }

            is Destination.Composer -> mailboxRobot {
                topAppBarSection { tapComposerIcon() }
            }

            is Destination.MailDetail -> mailboxRobot {
                listSection { clickMessageByPosition(destination.messagePosition) }
            }

            is Destination.EditDraft -> {
                navigateTo(Destination.Drafts, launchApp = false, performLoginViaUI = false)
                mailboxRobot { listSection { clickMessageByPosition(destination.draftPosition) } }
                composerRobot { fullscreenLoaderSection { waitUntilGone() } }
            }

            is Destination.SidebarMenu -> menuRobot { openSidebarMenu() }
        }
    }

    private fun login() {
        addAccountRobot
            .signIn()
            .loginUser<LoginRobot>(MockedLoginTestUsers.defaultLoginUser)
            .waitUntilSignInScreenIsGone()
    }
}
