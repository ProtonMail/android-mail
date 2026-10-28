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

package ch.protonmail.android.mailupselling.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailfeatureflags.domain.model.PlusToUnlimitedUpsellOptOut
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailupselling.domain.repository.UpsellOptOutRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

internal class RecordPlusToUnlimitedOptOutTest {

    private val userId = UserId("user-id")
    private val userSessionRepository = mockk<UserSessionRepository>()
    private val upsellOptOutRepository = mockk<UpsellOptOutRepository>(relaxUnitFun = true)
    private val recordPlusToUnlimitedOptOut = RecordPlusToUnlimitedOptOut(userSessionRepository, upsellOptOutRepository)

    @Test
    fun `writes the opt-out flag override for the user`() = runTest {
        coEvery {
            userSessionRepository.overrideFeatureFlag(userId, PlusToUnlimitedUpsellOptOut.key, true)
        } returns Unit.right()

        recordPlusToUnlimitedOptOut(userId)

        coVerify { userSessionRepository.overrideFeatureFlag(userId, PlusToUnlimitedUpsellOptOut.key, true) }
    }

    @Test
    fun `signals the opt-out change after writing the flag`() = runTest {
        coEvery {
            userSessionRepository.overrideFeatureFlag(userId, PlusToUnlimitedUpsellOptOut.key, true)
        } returns Unit.right()

        recordPlusToUnlimitedOptOut(userId)

        coVerifyOrder {
            userSessionRepository.overrideFeatureFlag(userId, PlusToUnlimitedUpsellOptOut.key, true)
            upsellOptOutRepository.emitOptOutChanged()
        }
    }
}
