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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailsettings.data.repository.local.MobileFooterLocalDataSource
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.fail

internal class MobileFooterRepositoryImplTest {

    private val mobileFooterLocalDataSource = mockk<MobileFooterLocalDataSource>()
    private val mobileFooterRepository = MobileFooterRepositoryImpl(mobileFooterLocalDataSource)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return the correct mobile footer when it is stored`() = runTest {
        // Given
        val expectedFooter = MobileFooter.PaidUserMobileFooter("footer", enabled = true)
        coEvery { mobileFooterLocalDataSource.observeMobileFooterPreference(BaseUserId) } returns flowOf(
            BaseMobileFooter.right()
        )

        // When
        val result = mobileFooterRepository.getMobileFooter(BaseUserId)

        // Then
        (result.getOrNull() as? MobileFooter.PaidUserMobileFooter)?.run {
            assertEquals(expectedFooter.enabled, this.enabled)
            assertEquals(expectedFooter.value, this.value)
        } ?: fail("Invalid result")
    }

    @Test
    fun `should return an error when no local data can be fetched`() = runTest {
        // Given
        coEvery { mobileFooterLocalDataSource.observeMobileFooterPreference(BaseUserId) } returns flowOf(
            PreferencesError.left()
        )

        // When
        val result = mobileFooterRepository.getMobileFooter(BaseUserId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should not return an error when the mobile footer can be saved successfully`() = runTest {
        // Given
        coEvery {
            mobileFooterLocalDataSource.updateMobileFooter(BaseUserId, BaseMobileFooter)
        } returns Unit.right()

        // When
        val result = mobileFooterRepository.updateMobileFooter(BaseUserId, BaseMobileFooter)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return an error when the mobile footer cannot be saved`() = runTest {
        // Given
        coEvery {
            mobileFooterLocalDataSource.updateMobileFooter(BaseUserId, BaseMobileFooter)
        } returns PreferencesError.left()

        // When
        val result = mobileFooterRepository.updateMobileFooter(BaseUserId, BaseMobileFooter)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)

    }

    private companion object {

        val BaseUserId = UserId("123")
        val BaseMobileFooter = MobileFooterPreference("footer", enabled = true)
    }
}
