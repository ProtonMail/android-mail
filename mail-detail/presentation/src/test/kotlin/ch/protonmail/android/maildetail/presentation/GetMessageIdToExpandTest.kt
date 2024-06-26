package ch.protonmail.android.maildetail.presentation

import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMessageIdToExpandTest {

    private val getMessageIdToExpand = GetMessageIdToExpand()

    @Test
    fun `return latest messageId when all messages are read`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.ReadMessageMayFirst),
            MessageWithLabelsSample.build(MessageSample.ReadMessageMaySecond)
        )

        // When
        val result = getMessageIdToExpand(messages)

        // Then
        assertEquals(MessageIdSample.ReadMessageMaySecond, result)
    }

    @Test
    fun `return oldest messageId when all messages are unread`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.UnreadMessageMayFirst),
            MessageWithLabelsSample.build(MessageSample.UnreadMessageMaySecond)
        )

        // When
        val result = getMessageIdToExpand(messages)

        // Then
        assertEquals(MessageIdSample.UnreadMessageMayFirst, result)
    }

    @Test
    fun `return latest read when it's newer than other unread messages`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.UnreadMessageMayFirst),
            MessageWithLabelsSample.build(MessageSample.ReadMessageMaySecond)
        )

        // When
        val result = getMessageIdToExpand(messages)

        // Then
        assertEquals(MessageIdSample.ReadMessageMaySecond, result)
    }

    @Test
    fun `return oldest unread when latest message is unread`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.ReadMessageMayFirst.copy(order = 1)),
            MessageWithLabelsSample.build(MessageSample.UnreadMessageMayFirst.copy(order = 2)),
            MessageWithLabelsSample.build(MessageSample.ReadMessageMaySecond),
            MessageWithLabelsSample.build(MessageSample.UnreadMessageMayThird)
        )

        // When
        val result = getMessageIdToExpand(messages)

        // Then
        assertEquals(MessageIdSample.UnreadMessageMayFirst, result)
    }

    @Test
    fun `return latest messageId that is not a drafts`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.ReadMessageMayFirst),
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMaySecond.copy(labelIds = listOf(SystemLabelId.AllDrafts.labelId))
            )
        )

        // When
        val result = getMessageIdToExpand(messages)

        // Then
        assertEquals(MessageIdSample.ReadMessageMayFirst, result)
    }
}
