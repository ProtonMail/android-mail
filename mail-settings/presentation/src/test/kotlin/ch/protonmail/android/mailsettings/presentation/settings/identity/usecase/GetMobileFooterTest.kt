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
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GetMobileFooterTest {

    private val context = mockk<Context>()
    private val isPaidUser: IsPaidUser = mockk()
    private val mobileFooterRepository: MobileFooterRepository = mockk()
    private val getMobileFooter = GetMobileFooter(context, isPaidUser, mobileFooterRepository)

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
    fun `should propagate the paid user mobile footer when the user is a paid user`() = runTest {
        // Given
        val expectedFooter = MobileFooter.PaidUserMobileFooter("footer", false).right()
        expectPaidUser()
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
        coEvery { isPaidUser(BaseUserId) } returns expectedError

        // When
        val result = getMobileFooter(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    @Test
    fun `should propagate the error when the mobile footer cannot be retrieved`() = runTest {
        // Given
        val expectedError = DataError.Local.NoDataCached.left()
        expectPaidUser()
        coEvery { mobileFooterRepository.getMobileFooter(BaseUserId) } returns expectedError

        // When
        val result = getMobileFooter(BaseUserId)

        // Then
        assertEquals(expectedError, result)
    }

    private fun expectPaidUser() {
        coEvery { isPaidUser(BaseUserId) } returns true.right()
    }

    private fun expectFreeUser() {
        coEvery { isPaidUser(BaseUserId) } returns false.right()
        every { context.getString(any()) } returns "Any string"
    }

    private companion object {

        val BaseUserId = UserId("123")
    }
}
