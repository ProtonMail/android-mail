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
        val result = getMessageIdToExpand(messages, null, false)

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
        val result = getMessageIdToExpand(messages, null, false)

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
        val result = getMessageIdToExpand(messages, null, false)

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
        val result = getMessageIdToExpand(messages, null, false)

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
        val result = getMessageIdToExpand(messages, null, false)

        // Then
        assertEquals(MessageIdSample.ReadMessageMayFirst, result)
    }

    @Test
    fun `return latest non-draft message id that is located where the conversation was opened from`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(MessageSample.ReadMessageMayFirst),
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMaySecond.copy(
                    labelIds = listOf(SystemLabelId.Archive.labelId)
                )
            ),
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMayThird.copy(
                    labelIds = listOf(SystemLabelId.Archive.labelId, SystemLabelId.AllDrafts.labelId)
                )
            )
        )

        // When
        val result = getMessageIdToExpand(messages, SystemLabelId.Archive.labelId, false)

        // Then
        assertEquals(MessageIdSample.ReadMessageMaySecond, result)
    }

    @Test
    fun `return latest non-draft non-trash message id when messages are hidden based on the trash filter`() {
        // Given
        val messages = listOf(
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMayFirst.copy(
                    labelIds = listOf(SystemLabelId.Archive.labelId)
                )
            ),
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMaySecond.copy(
                    labelIds = listOf(SystemLabelId.Trash.labelId)
                )
            ),
            MessageWithLabelsSample.build(
                MessageSample.ReadMessageMayThird.copy(
                    labelIds = listOf(SystemLabelId.Archive.labelId, SystemLabelId.AllDrafts.labelId)
                )
            )
        )

        // When
        val result = getMessageIdToExpand(messages, SystemLabelId.Archive.labelId, true)

        // Then
        assertEquals(MessageIdSample.ReadMessageMayFirst, result)
    }
}
