package ch.protonmail.android.maildetail.domain.usecase

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.Message
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Provider
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDetailBottomSheetActionsTest {

    private val provideIsCustomizeToolbarEnabled = mockk<Provider<Boolean>>()

    private val sut by lazy {
        GetDetailBottomSheetActions(
            provideIsCustomizeToolbarEnabled.get()
        )
    }

    private val normalLabels = listOf(
        SystemLabelId.AllMail.labelId,
        SystemLabelId.Sent.labelId,
        SystemLabelId.Inbox.labelId
    )

    private val spamLabels = listOf(SystemLabelId.Spam.labelId)
    private val trashLabels = listOf(SystemLabelId.Trash.labelId)

    private val normalMessage = mockk<Message> {
        every { this@mockk.labelIds } returns normalLabels
    }

    private val normalConversation = mockk<Conversation> {
        every { this@mockk.labels } returns normalLabels.toConversationLabels()
    }

    private val spamConversation = mockk<Conversation> {
        every { this@mockk.labels } returns spamLabels.toConversationLabels()
    }

    private val trashedConversation = mockk<Conversation> {
        every { this@mockk.labels } returns trashLabels.toConversationLabels()
    }

    private val spamMessage = mockk<Message> {
        every { this@mockk.labelIds } returns spamLabels
    }

    private val trashMessage = mockk<Message> {
        every { this@mockk.labelIds } returns trashLabels
    }

    @Test
    fun `returns correct actions for a normal message`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(false)

        // When
        val result = sut(null, normalMessage, affectingConversation = false)

        // Then
        assertEquals(
            listOf(
                Action.Reply,
                Action.ReplyAll,
                Action.Forward,
                Action.MarkUnread,
                Action.Label,
                Action.ViewInLightMode,
                Action.ViewInDarkMode,
                Action.Trash,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a normal message with toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(null, normalMessage, affectingConversation = false)

        // Then
        assertEquals(
            listOf(
                Action.Reply,
                Action.ReplyAll,
                Action.Forward,
                Action.MarkUnread,
                Action.Label,
                Action.ViewInLightMode,
                Action.ViewInDarkMode,
                Action.Trash,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a spam message with toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(null, spamMessage, affectingConversation = false)

        // Then
        assertEquals(
            listOf(
                Action.Reply,
                Action.ReplyAll,
                Action.Forward,
                Action.MarkUnread,
                Action.Label,
                Action.ViewInLightMode,
                Action.ViewInDarkMode,
                Action.Delete,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a trash message with toolbar FF disabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(false)

        // When
        val result = sut(null, trashMessage, affectingConversation = false)

        // Then
        assertEquals(
            listOf(
                Action.Reply,
                Action.ReplyAll,
                Action.Forward,
                Action.MarkUnread,
                Action.Label,
                Action.ViewInLightMode,
                Action.ViewInDarkMode,
                Action.Delete,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a normal conversation message with toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(normalConversation, trashMessage, affectingConversation = true)

        // Then
        assertEquals(
            listOf(
                Action.MarkUnread,
                Action.Label,
                Action.Trash,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a trashed conversation message with toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(trashedConversation, trashMessage, affectingConversation = true)

        // Then
        assertEquals(
            listOf(
                Action.MarkUnread,
                Action.Label,
                Action.Delete,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `returns correct actions for a spam conversation message with toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(spamConversation, normalMessage, affectingConversation = true)

        // Then
        assertEquals(
            listOf(
                Action.MarkUnread,
                Action.Label,
                Action.Delete,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `does not return delete action if the entire conversation is not trashed or spam`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(normalConversation, trashMessage, affectingConversation = true)

        // Then
        assertEquals(
            listOf(
                Action.MarkUnread,
                Action.Label,
                Action.Trash,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    @Test
    fun `does not return delete action if affecting a message inside a conversation`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
        val result = sut(normalConversation, trashMessage, affectingConversation = false)

        // Then
        assertEquals(
            listOf(
                Action.Reply,
                Action.ReplyAll,
                Action.Forward,
                Action.MarkUnread,
                Action.Label,
                Action.ViewInLightMode,
                Action.ViewInDarkMode,
                Action.Trash,
                Action.Archive,
                Action.Spam,
                Action.Move,
                Action.Print,
                Action.OpenCustomizeToolbar,
                Action.ReportPhishing
            ),
            result
        )
    }

    private fun customizeToolbarFeatureEnabled(value: Boolean) {
        every {
            provideIsCustomizeToolbarEnabled.get()
        } returns value
    }

    private fun List<LabelId>.toConversationLabels() = map { labelId ->
        mockk<ConversationLabel> {
            every { this@mockk.labelId } returns labelId
        }
    }
}
