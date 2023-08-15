package ch.protonmail.android.mailcomposer.domain.usecase

import java.time.Instant
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.domain.model.AttachmentCount
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CreateEmptyDraftTest {

    @Before
    fun setUp() {
        mockkStatic(Instant::class)
    }

    @Test
    fun `should create an empty draft with the current timestamp and the given sender`() {
        // Given
        val expectedCurrentTimestamp = expectedCurrentTimestamp { 42L }
        val expectedMessageId = MessageIdSample.EmptyDraft
        val expectedUserId = UserIdSample.Primary
        val expectedUserAddress = UserAddressSample.build()
        val expectedEmptyDraft = MessageWithBody(
            message = Message(
                userId = expectedUserId,
                messageId = expectedMessageId,
                conversationId = ConversationId(EMPTY_STRING),
                order = 0,
                subject = EMPTY_STRING,
                unread = false,
                sender = Sender(expectedUserAddress.email, expectedUserAddress.displayName!!),
                toList = emptyList(),
                ccList = emptyList(),
                bccList = emptyList(),
                time = expectedCurrentTimestamp,
                size = 0L,
                expirationTime = 0L,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                addressId = expectedUserAddress.addressId,
                externalId = null,
                numAttachments = 0,
                flags = 0L,
                attachmentCount = AttachmentCount(0),
                labelIds = listOf(
                    SystemLabelId.Drafts.labelId,
                    SystemLabelId.AllDrafts.labelId,
                    SystemLabelId.AllMail.labelId
                )
            ),
            messageBody = MessageBody(
                userId = expectedUserId,
                messageId = expectedMessageId,
                body = EMPTY_STRING,
                header = EMPTY_STRING,
                attachments = emptyList(),
                mimeType = MimeType.PlainText,
                spamScore = EMPTY_STRING,
                replyTo = Recipient(
                    address = expectedUserAddress.email,
                    name = expectedUserAddress.displayName!!,
                    group = null
                ),
                replyTos = emptyList(),
                unsubscribeMethods = null
            )
        )

        // When
        val actualEmptyDraft = CreateEmptyDraft()(expectedMessageId, expectedUserId, expectedUserAddress)

        // Then
        assertEquals(expectedEmptyDraft, actualEmptyDraft)
    }

    @After
    fun tearDown() {
        unmockkStatic(Instant::class)
    }

    private fun expectedCurrentTimestamp(expectedCurrentTimestamp: () -> Long): Long = expectedCurrentTimestamp().also {
        every { Instant.now() } returns mockk {
            every { epochSecond } returns it
        }
    }
}
