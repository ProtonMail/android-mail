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
import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.testdata.conversation.ConversationIdTestData
import ch.protonmail.android.testdata.label.LabelEntityTestData
import ch.protonmail.android.testdata.label.LabelIdTestData
import ch.protonmail.android.testdata.label.MessageLabelEntityTestData
import ch.protonmail.android.testdata.message.MessageEntityTestData
import ch.protonmail.android.testdata.message.MessageWithLabelIdsTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class MessageDaoTest : BaseDatabaseTest() {

    private val allMessages = listOf(
        MessageWithLabelIdsTestData.AugWeatherForecast,
        MessageWithLabelIdsTestData.Invoice,
        MessageWithLabelIdsTestData.SepWeatherForecast
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
            userId = UserIdTestData.Primary
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
            userId = UserIdTestData.Primary
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByLabelIdByDesc() = runBlocking {
        // given
        val labelId = LabelIdTestData.Document
        val expected = allMessages
            .filter { labelId in it.labelIds }
            .sortedByDescending { it.message.time }

        // when
        messageDao.observeAllOrderByTimeDesc(
            userId = UserIdTestData.Primary,
            labelId = labelId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByLabelIdByAsc() = runBlocking {
        // given
        val labelId = LabelIdTestData.Document
        val expected = allMessages
            .filter { labelId in it.labelIds }
            .sortedBy { it.message.time }

        // when
        messageDao.observeAllOrderByTimeAsc(
            userId = UserIdTestData.Primary,
            labelId = labelId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByConversationIdByDesc() = runBlocking {
        // given
        val conversationId = ConversationIdTestData.WeatherForecast
        val expected = allMessages
            .filter { it.message.conversationId == conversationId }
            .sortedByDescending { it.message.time }

        // when
        messageDao.observeAllOrderByTimeDesc(
            userId = UserIdTestData.Primary,
            conversationId = conversationId
        ).test {

            // then
            assertMessagesEquals(expected)
        }
    }

    @Test
    fun findAllByConversationIdByAsc() = runBlocking {
        // given
        val conversationId = ConversationIdTestData.WeatherForecast
        val expected = allMessages
            .filter { it.message.conversationId == conversationId }
            .sortedBy { it.message.time }

        // when
        messageDao.observeAllOrderByTimeAsc(
            userId = UserIdTestData.Primary,
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
                insertOrIgnore(LabelEntityTestData.Archive)
                insertOrIgnore(LabelEntityTestData.Document)
            }
            with(messageDao) {
                insertOrIgnore(MessageEntityTestData.AugWeatherForecast)
                insertOrIgnore(MessageEntityTestData.Invoice)
                insertOrIgnore(MessageEntityTestData.SepWeatherForecast)
            }
            with(messageLabelDao) {
                insertOrIgnore(MessageLabelEntityTestData.AugWeatherForecastArchive)
                insertOrIgnore(MessageLabelEntityTestData.InvoiceArchive)
                insertOrIgnore(MessageLabelEntityTestData.InvoiceDocument)
                insertOrIgnore(MessageLabelEntityTestData.SepWeatherForecastArchive)
            }
        }
    }

    private suspend fun FlowTurbine<List<MessageWithLabelIds>>.assertMessagesEquals(
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
