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

import android.util.Log
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.usecase.GetPrimaryAddress
import ch.protonmail.android.mailcomposer.domain.usecase.IsValidEmailAddress
import ch.protonmail.android.mailcomposer.domain.usecase.ProvideNewDraftId
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBody
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithSender
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.ComposerAction
import ch.protonmail.android.mailcomposer.presentation.model.SenderUiModel
import ch.protonmail.android.mailcomposer.presentation.reducer.ComposerReducer
import ch.protonmail.android.mailcomposer.domain.usecase.GetComposerSenderAddresses
import ch.protonmail.android.mailcomposer.domain.usecase.StoreDraftWithBodyError
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.test.utils.TestTree
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.UserAddress
import org.junit.Rule
import timber.log.Timber
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val storeDraftWithBodyMock = mockk<StoreDraftWithBody>()
    private val storeDraftWithSenderMock = mockk<StoreDraftWithSender>()
    private val observePrimaryUserIdMock = mockk<ObservePrimaryUserId>()
    private val isValidEmailAddressMock = mockk<IsValidEmailAddress>()
    private val getPrimaryAddressMock = mockk<GetPrimaryAddress>()
    private val provideNewDraftIdMock = mockk<ProvideNewDraftId>()
    private val resolveUserAddressMock = mockk<ResolveUserAddress>()
    private val getComposerSenderAddresses = mockk<GetComposerSenderAddresses> {
        coEvery { this@mockk.invoke() } returns GetComposerSenderAddresses.Error.UpgradeToChangeSender.left()
    }
    private val reducer = ComposerReducer()
    private val testTree = TestTree()

    private val viewModel by lazy {
        ComposerViewModel(
            storeDraftWithBodyMock,
            storeDraftWithSenderMock,
            reducer,
            isValidEmailAddressMock,
            getPrimaryAddressMock,
            resolveUserAddressMock,
            getComposerSenderAddresses,
            observePrimaryUserIdMock,
            provideNewDraftIdMock
        )
    }

    @BeforeTest
    fun setUp() {
        Timber.plant(testTree)
    }

    @Test
    fun `should store the draft body when the body changes`() {
        // Given
        val primaryAddress = UserAddressSample.PrimaryAddress
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedDraftBody = DraftBody(RawDraftBody)
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSenderAddress = expectedSenderAddress(expectedUserId, primaryAddress.email) {
            primaryAddress
        }
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)
        expectedPrimaryAddress(expectedUserId) { primaryAddress }
        expectStoreDraftBodySucceeds(
            expectedMessageId,
            expectedDraftBody,
            expectedSenderAddress,
            expectedUserId
        )

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedSenderAddress,
                expectedUserId
            )
        }
    }

    @Test
    fun `should store draft sender when sender address changes`() = runTest {
        // Given
        val expectedSenderEmail = UserAddressSample.AliasAddress.email
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSenderAddress = expectedSenderAddress(expectedUserId, expectedSenderEmail) {
            UserAddressSample.AliasAddress
        }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftSenderSucceeds(expectedMessageId, expectedSenderAddress, expectedUserId)

        // When
        viewModel.submit(action)

        // Then
        coVerify {
            storeDraftWithSenderMock(
                expectedMessageId,
                expectedSenderAddress,
                expectedUserId
            )
        }
    }

    @Test
    fun `emits state with primary sender address when available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedMessageId { MessageIdSample.EmptyDraft }
        val primaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(SenderUiModel(primaryAddress.email), actual.fields.sender)
    }

    @Test
    fun `emits state with sender address error when not available`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddressError(expectedUserId) { DataError.Local.NoDataCached }

        // When
        val actual = viewModel.state.value

        // Then
        assertEquals(TextUiModel(R.string.composer_error_invalid_sender), actual.error.consume())
    }

    @Test
    fun `emits state with user addresses when sender can be changed`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val addresses = listOf(UserAddressSample.PrimaryAddress, UserAddressSample.AliasAddress)
        val expectedPrimaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedSenderAddress(expectedUserId, expectedPrimaryAddress.email) {
            UserAddressSample.AliasAddress
        }
        expectedGetComposerSenderAddresses { addresses }

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = addresses.map { SenderUiModel(it.email) }
        assertEquals(expected, currentState.senderAddresses)
    }

    @Test
    fun `emits state with upgrade plan to change sender when user cannot change sender`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedPrimaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedSenderAddress(expectedUserId, expectedPrimaryAddress.email) {
            UserAddressSample.AliasAddress
        }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.UpgradeToChangeSender }

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = TextUiModel(R.string.composer_change_sender_paid_feature)
        assertEquals(expected, currentState.premiumFeatureMessage.consume())
    }

    @Test
    fun `emits state with error when cannot determine if user can change sender`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedPrimaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedSenderAddress(expectedUserId, expectedPrimaryAddress.email) {
            UserAddressSample.AliasAddress
        }
        expectedGetComposerSenderAddressesError { GetComposerSenderAddresses.Error.FailedDeterminingUserSubscription }

        // When
        viewModel.submit(ComposerAction.ChangeSenderRequested)

        // Then
        val currentState = viewModel.state.value
        val expected = TextUiModel(R.string.composer_error_change_sender_failed_getting_subscription)
        assertEquals(expected, currentState.error.consume())
    }

    @Test
    fun `emits state with new sender address when sender changed`() = runTest {
        // Given
        val expectedSenderEmail = UserAddressSample.AliasAddress.email
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedSenderAddress = expectedSenderAddress(expectedUserId, expectedSenderEmail) {
            UserAddressSample.AliasAddress
        }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail))
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectStoreDraftSenderSucceeds(expectedMessageId, expectedSenderAddress, expectedUserId)

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(SenderUiModel(expectedSenderEmail), currentState.fields.sender)
    }

    @Test
    fun `emits state with saving draft with new sender error when save draft with sender returns error`() = runTest {
        // Given
        val expectedUserAddress = UserAddressSample.AliasAddress
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedUserAddress.email))
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedSenderAddress(expectedUserId, expectedUserAddress.email) { expectedUserAddress }
        expectStoreDraftSenderFails(expectedMessageId, expectedUserAddress, expectedUserId) {
            StoreDraftWithSender.Error.DraftSaveError
        }

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_save_draft_with_new_sender), currentState.error.consume())
        assertErrorLogged("Store draft $expectedMessageId with new sender ${expectedUserAddress.addressId} failed")
    }

    @Test
    fun `emits state with change sender error when resolve user address returns error`() = runTest {
        // Given
        val expectedSenderEmail = UserAddressSample.AliasAddress.email
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.SenderChanged(SenderUiModel(expectedSenderEmail))
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedSenderAddressError(expectedUserId, expectedSenderEmail) {
            ResolveUserAddress.Error.UserAddressNotFound
        }

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_resolving_sender_address), currentState.error.consume())
    }

    @Test
    fun `emits state with save draft failed resolving sender when resolve user address returns error`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val action = ComposerAction.DraftBodyChanged(DraftBody("updated-draft"))
        val expectedPrimaryAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedMessageId { MessageIdSample.EmptyDraft }
        expectedSenderAddressError(expectedUserId, expectedPrimaryAddress.email) {
            ResolveUserAddress.Error.UserAddressNotFound
        }

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        val expected = TextUiModel(R.string.composer_error_save_draft_could_not_resolve_sender)
        assertEquals(expected, currentState.error.consume())
    }

    @Test
    fun `emits state with saving draft body error when save draft body returns error`() = runTest {
        // Given
        val expectedUserId = expectedUserId { UserIdSample.Primary }
        val expectedDraftBody = DraftBody("updated-draft")
        val action = ComposerAction.DraftBodyChanged(expectedDraftBody)
        val expectedMessageId = expectedMessageId { MessageIdSample.EmptyDraft }
        val expectedUserAddress = expectedPrimaryAddress(expectedUserId) { UserAddressSample.PrimaryAddress }
        expectedSenderAddress(expectedUserId, expectedUserAddress.email) { expectedUserAddress }
        expectStoreDraftBodyFails(expectedMessageId, expectedDraftBody, expectedUserAddress, expectedUserId) {
            StoreDraftWithBodyError.DraftSaveError
        }

        // When
        viewModel.submit(action)

        // Then
        val currentState = viewModel.state.value
        assertEquals(TextUiModel(R.string.composer_error_store_draft_body_DB_failure), currentState.error.consume())
    }

    private fun expectedMessageId(messageId: () -> MessageId): MessageId = messageId().also {
        every { provideNewDraftIdMock() } returns it
    }

    private fun expectedUserId(userId: () -> UserId): UserId = userId().also {
        coEvery { observePrimaryUserIdMock() } returns flowOf(it)
    }

    private fun expectedPrimaryAddress(userId: UserId, userAddress: () -> UserAddress) = userAddress().also {
        coEvery { getPrimaryAddressMock(userId) } returns it.right()
    }

    private fun expectedPrimaryAddressError(userId: UserId, dataError: () -> DataError) = dataError().also {
        coEvery { getPrimaryAddressMock(userId) } returns it.left()
    }

    private fun expectedSenderAddress(
        userId: UserId,
        email: String,
        senderAddress: () -> UserAddress
    ): UserAddress = senderAddress().also {
        coEvery { resolveUserAddressMock(userId, email) } returns it.right()
    }

    private fun expectedSenderAddressError(
        userId: UserId,
        email: String,
        error: () -> ResolveUserAddress.Error
    ): ResolveUserAddress.Error = error().also {
        coEvery { resolveUserAddressMock(userId, email) } returns it.left()
    }

    private fun expectedGetComposerSenderAddresses(addresses: () -> List<UserAddress>): List<UserAddress> =
        addresses().also { coEvery { getComposerSenderAddresses() } returns it.right() }

    private fun expectedGetComposerSenderAddressesError(
        error: () -> GetComposerSenderAddresses.Error
    ): GetComposerSenderAddresses.Error = error().also { coEvery { getComposerSenderAddresses() } returns it.left() }

    private fun expectStoreDraftBodySucceeds(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedPrimaryAddress: UserAddress,
        expectedUserId: UserId
    ) {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedPrimaryAddress,
                expectedUserId
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftBodyFails(
        expectedMessageId: MessageId,
        expectedDraftBody: DraftBody,
        expectedPrimaryAddress: UserAddress,
        expectedUserId: UserId,
        error: () -> StoreDraftWithBodyError
    ) = error().also {
        coEvery {
            storeDraftWithBodyMock(
                expectedMessageId,
                expectedDraftBody,
                expectedPrimaryAddress,
                expectedUserId
            )
        } returns it.left()
    }

    private fun expectStoreDraftSenderSucceeds(
        expectedMessageId: MessageId,
        expectedSenderAddress: UserAddress,
        expectedUserId: UserId
    ) {
        coEvery {
            storeDraftWithSenderMock(
                expectedMessageId,
                expectedSenderAddress,
                expectedUserId
            )
        } returns Unit.right()
    }

    private fun expectStoreDraftSenderFails(
        expectedMessageId: MessageId,
        expectedSenderAddress: UserAddress,
        expectedUserId: UserId,
        error: () -> StoreDraftWithSender.Error
    ) = error().also {
        coEvery {
            storeDraftWithSenderMock(
                expectedMessageId,
                expectedSenderAddress,
                expectedUserId
            )
        } returns it.left()
    }

    private fun assertErrorLogged(message: String) {
        val expectedLog = TestTree.Log(Log.ERROR, null, message, null)
        assertEquals(expectedLog, testTree.logs.lastOrNull())
    }

    companion object TestData {

        const val RawDraftBody = "I'm a message body"
    }
}
