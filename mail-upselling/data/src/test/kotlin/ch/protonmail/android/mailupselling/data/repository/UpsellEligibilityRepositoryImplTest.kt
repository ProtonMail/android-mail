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

package ch.protonmail.android.mailupselling.data.repository

import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import ch.protonmail.android.mailupselling.domain.usecase.UpsellVariantPlan
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.MailUserSessionUpsellEligibilityResult
import uniffi.mail_uniffi.UpsellEligibility
import uniffi.mail_uniffi.UpsellExperimentFlag
import uniffi.mail_uniffi.UpsellType
import uniffi.mail_uniffi.upsellExperimentFlagForAndroid
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class UpsellEligibilityRepositoryImplTest {

    private val userId = UserId("user-1")
    private val dispatcher = UnconfinedTestDispatcher()
    private val rustSession = mockk<MailUserSession>()
    private val wrapper = mockk<MailUserSessionWrapper> {
        every { getRustUserSession() } returns rustSession
    }
    private val userSessionRepository = mockk<UserSessionRepository> {
        coEvery { getUserSession(userId) } returns wrapper
    }
    private val executeWithUserSession = ExecuteWithUserSession(userSessionRepository, dispatcher)

    private val repository = UpsellEligibilityRepositoryImpl(executeWithUserSession)

    @BeforeTest
    fun setup() {
        mockkStatic("uniffi.mail_uniffi.Mail_uniffiKt")
        every { upsellExperimentFlagForAndroid() } returns UpsellExperimentFlag("MailAndroidV7UpsellPlanExperiment")
    }

    @AfterTest
    fun teardown() = unmockkAll()

    @Test
    fun `maps Eligible Unlimited to the Unlimited plan`() = runTest {
        coEvery { rustSession.upsellEligibility(any()) } returns
            MailUserSessionUpsellEligibilityResult.Ok(UpsellEligibility.Eligible(UpsellType.UNLIMITED, null))

        assertEquals(UpsellVariantPlan.UNLIMITED, repository.getEligibleUpsellPlan(userId))
    }

    @Test
    fun `maps Eligible MailPlus to the Mail Plus plan`() = runTest {
        coEvery { rustSession.upsellEligibility(any()) } returns
            MailUserSessionUpsellEligibilityResult.Ok(UpsellEligibility.Eligible(UpsellType.MAIL_PLUS, null))

        assertEquals(UpsellVariantPlan.MAIL_PLUS, repository.getEligibleUpsellPlan(userId))
    }

    @Test
    fun `maps NotEligible to null`() = runTest {
        coEvery { rustSession.upsellEligibility(any()) } returns
            MailUserSessionUpsellEligibilityResult.Ok(UpsellEligibility.NotEligible)

        assertNull(repository.getEligibleUpsellPlan(userId))
    }

    @Test
    fun `returns null when the session reports an error`() = runTest {
        coEvery { rustSession.upsellEligibility(any()) } returns
            mockk<MailUserSessionUpsellEligibilityResult.Error>()

        assertNull(repository.getEligibleUpsellPlan(userId))
    }

    @Test
    fun `returns null when there is no user session`() = runTest {
        coEvery { userSessionRepository.getUserSession(userId) } returns null

        assertNull(repository.getEligibleUpsellPlan(userId))
    }
}
