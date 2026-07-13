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

package ch.protonmail.android.mailpinlock.data.mapper

import ch.protonmail.android.mailpinlock.model.AutoLock
import ch.protonmail.android.mailpinlock.model.AutoLockBiometricsState
import ch.protonmail.android.mailpinlock.model.AutoLockInterval
import ch.protonmail.android.mailpinlock.model.BiometricsSystemState
import ch.protonmail.android.mailpinlock.model.Protection
import ch.protonmail.android.mailsettings.domain.model.AllowMobileDataForContentSearchIndexing
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.MobileSignaturePreference
import ch.protonmail.android.mailsettings.domain.model.SwipeNextPreference
import ch.protonmail.android.mailsettings.domain.model.Theme
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoLockMapperTest {

    private val appSettings = AppSettings(
        theme = Theme.DARK,
        autolockInterval = AutoLockInterval.FifteenMinutes,
        autolockProtection = Protection.Biometrics,
        customAppLanguage = "en",
        hasCombinedContactsEnabled = true,
        hasAlternativeRouting = false,
        mobileSignaturePreference = MobileSignaturePreference.Empty,
        swipeNextPreference = SwipeNextPreference(true),
        useMobileDataForContentSearchIndexing = AllowMobileDataForContentSearchIndexing.Enabled
    )

    private val biometricsSystemState = BiometricsSystemState.BiometricEnrolled
    private val biometricsState = AutoLockBiometricsState.BiometricsAvailable.BiometricsEnrolled

    private val expected = AutoLock(
        autolockInterval = AutoLockInterval.FifteenMinutes,
        protectionType = Protection.Biometrics,
        biometricsState = biometricsState
    )

    @Test
    fun `map appSettings to autolock WHEN biometrics enrolled`() {
        val actual = appSettings.toAutoLock(biometricsSystemState)
        assertEquals(actual.autolockInterval, expected.autolockInterval)
        assertEquals(actual.protectionType, expected.protectionType)
        assertEquals(actual.biometricsState, expected.biometricsState)
    }

    @Test
    fun `map appSettings to autolock WHEN biometrics not enrolled`() {
        appSettings.copy(autolockProtection = Protection.Pin)
        expected.copy(biometricsState = AutoLockBiometricsState.BiometricsAvailable.BiometricsNotEnrolled)
        val actual = appSettings.toAutoLock(biometricsSystemState)
        assertEquals(actual.autolockInterval, expected.autolockInterval)
        assertEquals(actual.protectionType, expected.protectionType)
        assertEquals(actual.biometricsState, expected.biometricsState)
    }
}
