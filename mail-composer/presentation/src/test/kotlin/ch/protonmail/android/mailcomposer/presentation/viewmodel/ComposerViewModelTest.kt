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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import org.junit.Test

class ComposerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>(relaxUnitFun = true)
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val isValidEmailAddressMock = mockk<IsValidEmailAddress>()
    private val userAddressManagerMock = mockk<UserAddressManager> {
        every { observeAddresses(any()) } returns emptyFlow()
    }
    private val provideNewDraftIdMock = mockk<ProvideNewDraftId>()
    private val viewModel
        get() = ComposerViewModel(
            storeDraftWithBodyMock,
            observePrimaryUserIdMock,
            userAddressManagerMock,
            ComposerReducer(),
            isValidEmailAddressMock,
            provideNewDraftIdMock
        )

    @Test
    fun `should store the draft body when the body changes`() {
        // Given
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedUserAddress = expectedSenderAddress(UserIdSample.Primary) { UserAddressSample.build() }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedUserAddress,
                expectedUserId
            )
        }
    }

    private fun expectedMessageId(messageId: () -> MessageId): MessageId = messageId().also {
        every { provideNewDraftIdMock() } returns it
    }

    private fun expectedUserId(userId: () -> UserId): UserId = userId().also {
        coEvery { observePrimaryUserIdMock() } returns flowOf(it)
    }

    private fun expectedSenderAddress(userId: UserId, senderAddress: () -> UserAddress): UserAddress =
        senderAddress().also {
            coEvery { userAddressManagerMock.observeAddresses(userId) } returns flowOf(listOf(it))
        }

    companion object TestData {

        const val RawDraftBody = "I'm a message body"
    }
}
