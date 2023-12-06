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

package ch.protonmail.android.mailsettings.domain.usecase.privacy

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.domain.model.BackgroundSyncPreference
import ch.protonmail.android.mailsettings.domain.model.PreventScreenshotsPreference
import ch.protonmail.android.mailsettings.domain.model.PrivacySettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import ch.protonmail.android.testdata.mailsettings.MailSettingsTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.type.IntEnum
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.ShowImage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ObservePrivacySettingsTest {

    private val observeMailSettings = mockk<ObserveMailSettings>()
    private val observePreventScreenshotsSetting = mockk<ObservePreventScreenshotsSetting>()
    private val observeBackgroundSyncSetting = mockk<ObserveBackgroundSyncSetting>()
    private val observePrivacySettings = ObservePrivacySettings(
        observeMailSettings,
        observePreventScreenshotsSetting,
        observeBackgroundSyncSetting
    )

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when fetched mail settings are null, an error is returned`() = runTest {
        // Given
        val expectedResult = DataError.Local.NoDataCached.left()
        expectValidPreventScreenshotsPreference()
        expectValidBackgroundSyncPreference()
        coEvery { observeMailSettings(userId) } returns flowOf(null)

        // When
        val result = observePrivacySettings(userId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when prevent screenshots settings cannot be fetched, an error is returned`() = runTest {
        // Given
        val expectedResult = DataError.Local.NoDataCached.left()
        expectValidMailSettingsPreference()
        coEvery { observePreventScreenshotsSetting() } returns flowOf(PreferencesError.left())
        expectValidBackgroundSyncPreference()

        // When
        val result = observePrivacySettings(userId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when background sync settings cannot be fetched, an error is returned`() = runTest {
        // Given
        val expectedResult = DataError.Local.NoDataCached.left()
        expectValidMailSettingsPreference()
        expectValidPreventScreenshotsPreference()
        coEvery { observeBackgroundSyncSetting() } returns flowOf(PreferencesError.left())

        // When
        val result = observePrivacySettings(userId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when data is fetched correctly, privacy settings are created accordingly`() = runTest {
        // Given
        val expectedResult = basePrivacySettings.right()

        expectValidMailSettingsPreference()
        expectValidPreventScreenshotsPreference()
        expectValidBackgroundSyncPreference()

        // When
        val result = observePrivacySettings(userId).first()

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when data is fetched and auto show remote content is not enabled, privacy settings are created accordingly`() =
        runTest {
            // Given
            val expectedMailSettings = mailSettings.copy(showImages = IntEnum(2, ShowImage.Embedded))
            val expectedResult = basePrivacySettings.copy(autoShowRemoteContent = false).right()

            expectValidMailSettingsPreference(expectedMailSettings)
            expectValidPreventScreenshotsPreference()
            expectValidBackgroundSyncPreference()

            // When
            val result = observePrivacySettings(userId).first()

            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    fun `when data is fetched and auto show inline images is not enabled, privacy settings are created accordingly`() =
        runTest {
            // Given
            val expectedMailSettings = mailSettings.copy(showImages = IntEnum(1, ShowImage.Remote))
            val expectedResult = basePrivacySettings.copy(autoShowEmbeddedImages = false).right()


            expectValidMailSettingsPreference(expectedMailSettings)
            expectValidPreventScreenshotsPreference()
            expectValidBackgroundSyncPreference()

            // When
            val result = observePrivacySettings(userId).first()

            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    fun `when data is fetched and no remote content loading is enabled, privacy settings are created accordingly`() =
        runTest {
            // Given
            val expectedMailSettings = mailSettings.copy(showImages = IntEnum(0, ShowImage.None))
            val expectedResult = basePrivacySettings.copy(
                autoShowRemoteContent = false,
                autoShowEmbeddedImages = false
            ).right()

            expectValidMailSettingsPreference(expectedMailSettings)
            expectValidPreventScreenshotsPreference()
            expectValidBackgroundSyncPreference()

            // When
            val result = observePrivacySettings(userId).first()

            // Then
            assertEquals(expectedResult, result)
        }

    private fun expectValidMailSettingsPreference(settings: MailSettings = mailSettings) {
        coEvery { observeMailSettings(userId) } returns flowOf(settings)
    }

    private fun expectValidPreventScreenshotsPreference() {
        coEvery {
            observePreventScreenshotsSetting()
        } returns flowOf(PreventScreenshotsPreference(true).right())
    }

    private fun expectValidBackgroundSyncPreference() {
        coEvery {
            observeBackgroundSyncSetting()
        } returns flowOf(BackgroundSyncPreference(true).right())
    }

    private companion object {

        val userId = UserIdTestData.userId
        val mailSettings = MailSettingsTestData.buildMailSettings(
            showImages = IntEnum(3, ShowImage.Both),
            confirmLink = true
        )
        val basePrivacySettings = PrivacySettings(
            autoShowRemoteContent = true,
            autoShowEmbeddedImages = true,
            preventTakingScreenshots = true,
            requestLinkConfirmation = true,
            allowBackgroundSync = true
        )
    }
}
