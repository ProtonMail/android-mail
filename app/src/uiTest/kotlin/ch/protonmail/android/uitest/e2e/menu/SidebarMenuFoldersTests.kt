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

package ch.protonmail.android.uitest.e2e.menu

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.folders.SidebarCustomItemEntry
import ch.protonmail.android.uitest.models.folders.Tint
import ch.protonmail.android.uitest.robot.menu.menuRobot
import ch.protonmail.android.uitest.robot.menu.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class SidebarMenuFoldersTests : MockedNetworkTest() {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("68718")
    fun checkShortHexAndStandardColorFolderAreDisplayedInSidebarMenu() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultCustomFolders = false,
            useDefaultMailSettings = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_68718.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_68718.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedFolders = arrayOf(
            SidebarCustomItemEntry(index = 0, name = "Shorthand Hex Folder", iconTint = Tint.WithColor.Bridge),
            SidebarCustomItemEntry(index = 1, name = "Standard Folder", iconTint = Tint.WithColor.PurpleBase)
        )

        navigator {
            navigateTo(Destination.Inbox)
        }

        menuRobot {
            openSidebarMenu()

            verify { customFoldersAreDisplayed(*expectedFolders) }
        }
    }

    @Test
    @TestId("79096")
    fun checkFoldersColorWhenSettingIsOffWithNoParentInheritingInSidebarMenu() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultCustomFolders = false,
            useDefaultMailSettings = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_79096.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_79096.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedFolders = arrayOf(
            SidebarCustomItemEntry(index = 0, name = "Shorthand Hex Folder", iconTint = Tint.NoColor),
            SidebarCustomItemEntry(index = 1, name = "Standard Folder", iconTint = Tint.NoColor)
        )

        navigator {
            navigateTo(Destination.Inbox)
        }

        menuRobot {
            openSidebarMenu()
            verify { customFoldersAreDisplayed(*expectedFolders) }
        }
    }

    @Test
    @TestId("79097")
    fun checkFoldersColorWhenSettingIsOffWithParentInheritingInSidebarMenu() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher(
            useDefaultCustomFolders = false,
            useDefaultMailSettings = false
        ) {
            addMockRequests(
                get("/mail/v4/settings")
                    respondWith "/mail/v4/settings/mail-v4-settings_79097.json"
                    withStatusCode 200,
                get("/core/v4/labels?Type=3")
                    respondWith "/core/v4/labels/labels-type3_79097.json"
                    withStatusCode 200,
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedFolders = arrayOf(
            SidebarCustomItemEntry(index = 0, name = "Shorthand Hex Folder", iconTint = Tint.NoColor),
            SidebarCustomItemEntry(index = 1, name = "Standard Folder", iconTint = Tint.NoColor)
        )

        navigator {
            navigateTo(Destination.Inbox)
        }

        menuRobot {
            openSidebarMenu()
            verify { customFoldersAreDisplayed(*expectedFolders) }
        }
    }
}
