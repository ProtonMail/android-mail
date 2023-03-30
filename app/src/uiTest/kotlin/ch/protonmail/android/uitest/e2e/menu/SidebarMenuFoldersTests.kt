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

import androidx.test.filters.SdkSuppress
import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.login.LoginStrategy
import ch.protonmail.android.uitest.helpers.login.MockedLoginTestUsers.defaultLoginUser
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.folders.SidebarFolderEntry
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import me.proton.core.auth.domain.usecase.ValidateServerProof
import me.proton.core.test.android.robots.auth.AddAccountRobot
import org.junit.Test

@HiltAndroidTest
@SdkSuppress(minSdkVersion = 28)
@UninstallModules(ServerProofModule::class)
internal class SidebarMenuFoldersTests : MockedNetworkTest(loginStrategy = LoginStrategy.LoggedOut) {

    private val addAccountRobot = AddAccountRobot()
    private val menuRobot = MenuRobot(composeTestRule)

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("68718")
    fun checkShortHexAndStandardColorFolderAreDisplayedInSidebarMenu() {
        mockWebServer.dispatcher = mockNetworkDispatcher(useDefaultCustomFolders = false) {
            addMockRequests(
                "/core/v4/labels?Type=3" respondWith "/core/v4/labels/labels-type3_68718.json" withStatusCode 200,
                "/mail/v4/messages" respondWith "/mail/v4/messages/messages_empty.json" withStatusCode 200 ignoreQueryParams true
            )
        }

        val expectedFolders = arrayOf(
            SidebarFolderEntry(index = 0, name = "Shorthand Hex Folder"),
            SidebarFolderEntry(index = 1, name = "Standard Folder")
        )

        addAccountRobot
            .signIn()
            .loginUser<Any>(defaultLoginUser)

        menuRobot
            .swipeOpenSidebarMenu()
            .verify { customFoldersAreDisplayed(*expectedFolders) }
    }
}
