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

package ch.protonmail.android.mailsettings.presentation.settings.identity.usecase

import android.content.Context
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidMailUser
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class GetMobileFooterTest {

    private val context = mockk<Context>()
    private val mobileFooterRepository: MobileFooterRepository = mockk()
    private val isPaidMailUser = mockk<IsPaidMailUser> {
        coEvery { this@mockk(BaseUserId) } returns false.right()
    }
    private val getMobileFooter = GetMobileFooter(context, isPaidMailUser, mobileFooterRepository)

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should return the free mobile footer when the user is not a paid user`() = runTest {
        // Given
        expectFreeUser()

        // When
        val result = getMobileFooter(BaseUserId)

        // Then
        assertTrue(result.getOrNull() is MobileFooter.FreeUserMobileFooter)
    }

    @Test
    fun `should propagate the paid user mobile footer when the user is a paid mail user`() = runTest {
        // Given
        val expectedFooter = MobileFooter.PaidUserMobileFooter("footer", false).right()
        expectPaidMailUser()
        coEvery { mobileFooterRepository.getMobileFooter(BaseUserId) } returns expectedFooter

        // When
        val result = getMobileFooter(BaseUserId)

        // Then
        assertEquals(expectedFooter, result)
    }

    @Test
    fun `should propagate the error when cannot determine if the user is paid or not`() = runTest {
        // Given
        val expectedError = DataError.Local.Unknown.left()
        coEvery { isPaidMailUser(BaseUserId) } returns expectedError

        // When
        val result = getMobileFooter(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `should propagate the default footer when the mobile footer has never been set`() = runTest {
        // Given
        val expectedDefault = MobileFooter.PaidUserMobileFooter(BaseDefaultString, true)
        expectPaidMailUserWithNoFooter()
        coEvery {
            context.getString(R.string.mail_settings_identity_mobile_footer_default_free)
        } returns BaseDefaultString

        // When
        val result = getMobileFooter(BaseUserId).getOrNull()

        // Then
        assertNotNull(result)
        assertEquals(result.enabled, expectedDefault.enabled)
        assertEquals(result.value, expectedDefault.value)
        verify(exactly = 1) { context.getString(R.string.mail_settings_identity_mobile_footer_default_free) }
    }

    private fun expectPaidMailUserWithNoFooter() {
        val error = DataError.Local.NoDataCached.left()
        coEvery { isPaidMailUser(BaseUserId) } returns true.right()
        coEvery { mobileFooterRepository.getMobileFooter(BaseUserId) } returns error
    }

    private fun expectPaidMailUser() {
        coEvery { isPaidMailUser(BaseUserId) } returns true.right()
    }

    private fun expectFreeUser() {
        coEvery { isPaidMailUser(BaseUserId) } returns false.right()
        every { context.getString(any()) } returns "Any string"
    }

    private companion object {

        val BaseUserId = UserId("123")
        const val BaseDefaultString = "Default"
    }
}
