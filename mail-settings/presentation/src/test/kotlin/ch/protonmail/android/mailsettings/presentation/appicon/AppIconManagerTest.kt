/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.appicon

import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailsettings.presentation.settings.appicon.ActivityAliasProxy
import ch.protonmail.android.mailsettings.presentation.settings.appicon.AppIconManager
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.AppIconData
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.activityAliasName
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AppIconManagerTest {

    private val mockAppInformation: AppInformation = mockk()
    private val mockCreateLaunchIntent: CreateLaunchIntent = mockk()
    private val mockNotificationManager: NotificationManagerCompatProxy = mockk()

    // Stands in for PackageManager's component state.
    private val enabledAliases = mutableSetOf<String>()
    private val activityAliasProxy: ActivityAliasProxy = mockk {
        every { isEnabled(any()) } answers { firstArg<String>() in enabledAliases }
        every { setEnabled(any(), any()) } answers {
            if (secondArg()) enabledAliases += firstArg<String>() else enabledAliases -= firstArg<String>()
        }
    }

    private lateinit var appIconManager: AppIconManager

    @BeforeTest
    fun setup() {
        every { mockAppInformation.appBuildFlavor } returns MOCK_FLAVOR
        every { mockNotificationManager.dismissAllNotifications() } just runs
        every { mockCreateLaunchIntent.invalidateCache() } just runs

        appIconManager = AppIconManager(
            activityAliasProxy = activityAliasProxy,
            appInformation = mockAppInformation,
            createLaunchIntent = mockCreateLaunchIntent,
            notificationManagerCompatProxy = mockNotificationManager
        )
    }

    @AfterTest
    fun teardown() {
        clearAllMocks()
    }

    @Test
    fun `getAvailableIcons returns all icons`() {
        val availableIcons = appIconManager.getAvailableIcons()
        assert(availableIcons == AppIconData.ALL_ICONS)
    }

    @Test
    fun `getCurrentIconData returns enabled icon`() {
        // Given
        enabledAliases += NEW_ICON.activityAliasName(ALIAS_PREFIX)

        // When
        val currentIcon = appIconManager.getCurrentIconData()

        // Then
        assertEquals(NEW_ICON, currentIcon)
    }

    @Test
    fun `getCurrentIconData falls back to the default icon when no alias is enabled`() {
        // When
        val currentIcon = appIconManager.getCurrentIconData()

        // Then
        assertEquals(DEFAULT_ICON, currentIcon)
    }

    @Test
    fun `setNewAppIcon updates icon state and executes side effects`() = runTest {
        // Given
        enabledAliases += DEFAULT_ICON.activityAliasName(ALIAS_PREFIX)

        // When
        appIconManager.setNewAppIcon(NEW_ICON)

        // Then
        assertEquals(NEW_ICON, appIconManager.getCurrentIconData())
        assertEquals(setOf(NEW_ICON.activityAliasName(ALIAS_PREFIX)), enabledAliases)
        verify { mockNotificationManager.dismissAllNotifications() }
        verify { mockCreateLaunchIntent.invalidateCache() }
    }

    private companion object {

        const val MOCK_FLAVOR = "dev"
        const val ALIAS_PREFIX = "ch.protonmail.android.dev"
        val DEFAULT_ICON = AppIconData.DEFAULT
        val NEW_ICON = AppIconData.CALCULATOR
    }
}
