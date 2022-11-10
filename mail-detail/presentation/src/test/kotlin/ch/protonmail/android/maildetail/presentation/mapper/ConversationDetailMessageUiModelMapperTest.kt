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

import ch.protonmail.android.maildetail.presentation.sample.ConversationDetailMessageUiModelSample
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
        every { this@mockk(message = MessageSample.AugWeatherForecast, senderResolvedName = any()) } returns
            ConversationDetailMessageUiModelSample.AugWeatherForecast.avatar
    }
    private val resolveParticipantName: ResolveParticipantName = mockk {
        every { this@mockk(contacts = any(), participant = RecipientSample.Doe) } returns ContactSample.Doe.name
        every { this@mockk(contacts = any(), participant = RecipientSample.John) } returns ContactSample.John.name
    }
    private val mapper = ConversationDetailMessageUiModelMapper(
        avatarUiModelMapper = avatarUiModelMapper,
        resolveParticipantName = resolveParticipantName
    )

    @Test
    fun `map to ui model returns collapsed model`() {
        // given
        val message = MessageSample.AugWeatherForecast
        val expected = ConversationDetailMessageUiModelSample.AugWeatherForecast

        // when
        val result = mapper.toUiModel(message, contacts = emptyList())

        // then
        assertEquals(expected, result)
    }
}
