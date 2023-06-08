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

package ch.protonmail.android.db

import androidx.room.withTransaction
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.data.sample.LabelEntitySample
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.data.sample.MessageEntitySample
import ch.protonmail.android.mailmessage.data.sample.MessageLabelEntitySample
import ch.protonmail.android.mailmessage.data.sample.MessageWithLabelIdsSample
import ch.protonmail.android.test.annotations.suite.SmokeTest
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

@SmokeTest
internal class MessageDaoTest : BaseDatabaseTest() {

    private val allMessages = listOf(
        MessageWithLabelIdsSample.AugWeatherForecast,
        MessageWithLabelIdsSample.Invoice,
        MessageWithLabelIdsSample.SepWeatherForecast
    )

    @BeforeTest
    fun setup() {
        runBlocking { setupDatabaseWithMessages() }
    }

    @Test
    fun findAllByAsc() = runBlocking {
        // given
        val expected = allMessages.sortedBy { it.message.time }

        // when
        messageDao.observeAllOrderByTimeAsc(
            userId = UserIdSample.Primary
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByDesc() = runBlocking {
        // given
        val expected = allMessages.sortedByDescending { it.message.time }

        // when
        messageDao.observeAllOrderByTimeDesc(
            userId = UserIdSample.Primary
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByLabelIdByDesc() = runBlocking {
        // given
        val labelId = LabelIdSample.Document
        val expected = allMessages
            .filter { labelId in it.labelIds }
            .sortedByDescending { it.message.time }

        // when
        messageDao.observeAllOrderByTimeDesc(
            userId = UserIdSample.Primary,
            labelId = labelId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByLabelIdByAsc() = runBlocking {
        // given
        val labelId = LabelIdSample.Document
        val expected = allMessages
            .filter { labelId in it.labelIds }
            .sortedBy { it.message.time }

        // when
        messageDao.observeAllOrderByTimeAsc(
            userId = UserIdSample.Primary,
            labelId = labelId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByConversationIdByDesc() = runBlocking {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val expected = allMessages
            .filter { it.message.conversationId == conversationId }
            .sortedByDescending { it.message.time }

        // when
        messageDao.observeAllOrderByTimeDesc(
            userId = UserIdSample.Primary,
            conversationId = conversationId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByConversationIdByAsc() = runBlocking {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val expected = allMessages
            .filter { it.message.conversationId == conversationId }
            .sortedBy { it.message.time }

        // when
        messageDao.observeAllOrderByTimeAsc(
            userId = UserIdSample.Primary,
            conversationId = conversationId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    private suspend fun setupDatabaseWithMessages() {
        database.withTransaction {
            insertPrimaryUser()
            with(labelDao) {
                insertOrIgnore(LabelEntitySample.Archive)
                insertOrIgnore(LabelEntitySample.Document)
            }
            with(messageDao) {
                insertOrIgnore(MessageEntitySample.AugWeatherForecast)
                insertOrIgnore(MessageEntitySample.Invoice)
                insertOrIgnore(MessageEntitySample.SepWeatherForecast)
            }
            with(messageLabelDao) {
                insertOrIgnore(MessageLabelEntitySample.AugWeatherForecastArchive)
                insertOrIgnore(MessageLabelEntitySample.InvoiceArchive)
                insertOrIgnore(MessageLabelEntitySample.InvoiceDocument)
                insertOrIgnore(MessageLabelEntitySample.SepWeatherForecastArchive)
            }
        }
    }

    private suspend fun ReceiveTurbine<List<MessageWithLabelIds>>.assertMessagesEquals(
        expected: List<MessageWithLabelIds>
    ) {
        val actual = awaitItem()
        assertContentEquals(expected, actual, buildMessage(expected, actual))
    }

    private fun buildMessage(expected: List<MessageWithLabelIds>, actual: List<MessageWithLabelIds>) =
        """Expected ${expected.size} messages, but got ${actual.size} messages
            |Expected: ${expected.map { it.message.messageId.id }}
            |Actual: ${actual.map { it.message.messageId.id }}
            |
        """.trimMargin()
}
