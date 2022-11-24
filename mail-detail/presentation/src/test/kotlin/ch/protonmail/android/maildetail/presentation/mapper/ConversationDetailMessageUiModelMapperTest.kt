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

package ch.protonmail.android.maildetail.presentation.mapper

import ch.protonmail.android.mailcommon.presentation.mapper.ExpirationTimeMapper
import ch.protonmail.android.mailcommon.presentation.usecase.FormatShortTime
import ch.protonmail.android.maildetail.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
import ch.protonmail.android.maildetail.presentation.sample.MessageLocationUiModelSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.mailmessage.domain.usecase.ResolveParticipantName
import ch.protonmail.android.testdata.contact.ContactSample
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConversationDetailMessageUiModelMapperTest {

    private val avatarUiModelMapper: DetailAvatarUiModelMapper = mockk {
        every { this@mockk(message = any(), senderResolvedName = any()) } returns
            ConversationDetailMessageUiModelSample.AugWeatherForecast.avatar
        every { this@mockk(message = MessageSample.ExpiringInvitation, senderResolvedName = any()) } returns
            ConversationDetailMessageUiModelSample.ExpiringInvitation.avatar
    }
    private val expirationTimeMapper: ExpirationTimeMapper = mockk {
        every { toUiModel(epochTime = any()) } returns
            requireNotNull(ConversationDetailMessageUiModelSample.ExpiringInvitation.expiration)
    }
    private val formatShortTime: FormatShortTime = mockk {
        every { this@mockk(itemTime = any()) } returns
            requireNotNull(ConversationDetailMessageUiModelSample.AugWeatherForecast.shortTime)
    }
    private val messageLocationUiModelMapper: MessageLocationUiModelMapper = mockk {
        every { this@mockk(labelIds = any(), labels = any()) } returns MessageLocationUiModelSample.AllMail
    }
    private val resolveParticipantName: ResolveParticipantName = mockk {
        every { this@mockk(contacts = any(), participant = RecipientSample.Doe) } returns ContactSample.Doe.name
        every { this@mockk(contacts = any(), participant = RecipientSample.John) } returns ContactSample.John.name
        every { this@mockk(contacts = any(), participant = RecipientSample.PreciWeather) } returns
            RecipientSample.PreciWeather.name
    }
    private val mapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = avatarUiModelMapper,
        expirationTimeMapper = expirationTimeMapper,
        formatShortTime = formatShortTime,
        messageLocationUiModelMapper = messageLocationUiModelMapper,
        resolveParticipantName = resolveParticipantName
    )

    @Test
    fun `map to ui model returns collapsed model`() {
        // given
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast
        val expected = ConversationDetailMessageUiModelSample.AugWeatherForecast

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is forwarded, ui model contains forwarded icon`() {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isForwarded = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.collapse().copy(
                forwardedIcon = ConversationDetailMessageUiModel.ForwardedIcon.Forwarded
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied, ui model contains replied icon`() {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isReplied = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.collapse().copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.Replied
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied all, ui model contains replied all icon`() {
        // given
        val message = MessageSample.AugWeatherForecast.copy(isRepliedAll = true)
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.collapse().copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message is replied and replied all, ui model contains replied all icon`() {
        // given
        val message = MessageSample.AugWeatherForecast.copy(
            isReplied = true,
            isRepliedAll = true
        )
        val messageWithLabels = MessageWithLabelsSample.AugWeatherForecast.copy(message = message)
        val expected = with(ConversationDetailMessageUiModelSample) {
            AugWeatherForecast.collapse().copy(
                repliedIcon = ConversationDetailMessageUiModel.RepliedIcon.RepliedAll
            )
        }

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has expiration, ui model contains formatted time`() {
        // given
        val message = MessageSample.ExpiringInvitation
        val messageWithLabels = MessageWithLabelsSample.ExpiringInvitation.copy(message = message)
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `when message has only calendar attachments, ui model has not attachments`() {
        // given
        val message = MessageSample.ExpiringInvitation
        val messageWithLabels = MessageWithLabelsSample.ExpiringInvitation.copy(message = message)
        val expected = ConversationDetailMessageUiModelSample.ExpiringInvitation

        // when
        val result = mapper.toUiModel(messageWithLabels, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }
}
