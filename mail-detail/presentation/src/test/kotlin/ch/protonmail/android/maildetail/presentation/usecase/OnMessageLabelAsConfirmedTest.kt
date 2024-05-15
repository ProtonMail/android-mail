package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.right
import ch.protonmail.android.maildetail.domain.usecase.MoveMessage
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.RelabelMessage
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.sample.LabelUiModelWithSelectedStateSample
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class OnMessageLabelAsConfirmedTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.PlainTextMessage

    private val moveMessage = mockk<MoveMessage> {
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = messageId,
                labelId = SystemLabelId.Archive.labelId
            )
        } returns Unit.right()
    }
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(userId, messageId) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message,
                emptyList()
            ).right()
        )
    }
    private val relabelMessage = mockk<RelabelMessage> {
        coEvery {
            this@mockk.invoke(
                userId = userId,
                messageId = messageId,
                currentLabelIds = any(),
                updatedLabelIds = any()
            )
        } returns with(MessageSample) {
            Invoice.labelAs(
                listOf(
                    MailLabelTestData.customLabelOne.id.labelId,
                    MailLabelTestData.customLabelTwo.id.labelId
                )
            )
        }.right()
    }

    private val onMessageLabelAsConfirmed = OnMessageLabelAsConfirmed(
        moveMessage,
        observeMessageWithLabels,
        relabelMessage
    )

    @Test
    fun `should call relabel message when label as was confirmed`() = runTest {
        // Given
        val labelUiModelsWithSelectedState = LabelUiModelWithSelectedStateSample.customLabelListWithSelection

        // When
        onMessageLabelAsConfirmed(userId, messageId, labelUiModelsWithSelectedState, false)

        // Then
        coVerify {
            relabelMessage(
                userId,
                messageId,
                emptyList(),
                labelUiModelsWithSelectedState.filter {
                    it.selectedState == LabelSelectedState.Selected
                }.map { it.labelUiModel.id.labelId }
            )
        }
    }

    @Test
    fun `should call move message when label as was confirmed and archive was selected`() = runTest {
        // Given
        val labelUiModelsWithSelectedState = LabelUiModelWithSelectedStateSample.customLabelListWithSelection

        // When
        onMessageLabelAsConfirmed(userId, messageId, labelUiModelsWithSelectedState, true)

        // Then
        coVerify {
            moveMessage(userId, messageId, SystemLabelId.Archive.labelId)
        }
    }
}
