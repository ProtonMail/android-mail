package ch.protonmail.android.maildetail.presentation.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.toUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadDataForMessageLabelAsBottomSheetTest {

    private val userId = UserIdTestData.userId
    private val messageId = MessageIdSample.PlainTextMessage

    private val observeCustomMailLabels = mockk<ObserveCustomMailLabels> {
        coEvery { this@mockk.invoke(userId) } returns flowOf(
            listOf(
                MailLabelTestData.customLabelOne,
                MailLabelTestData.customLabelTwo
            ).right()
        )
    }
    private val observeFolderColorSettings = mockk<ObserveFolderColorSettings> {
        every { this@mockk.invoke(userId) } returns flowOf(FolderColorSettings())
    }
    private val observeMessageWithLabels = mockk<ObserveMessageWithLabels> {
        every { this@mockk.invoke(userId, messageId) } returns flowOf(
            MessageWithLabels(
                MessageTestData.message,
                listOf(LabelTestData.buildLabel(MailLabelTestData.customLabelOne.id.labelId.id))
            ).right()
        )
    }

    private val loadDataForMessageLabelAsBottomSheet = LoadDataForMessageLabelAsBottomSheet(
        observeCustomMailLabels,
        observeFolderColorSettings,
        observeMessageWithLabels
    )

    @Test
    fun `should return bottom sheet action data when all operations succeeded`() = runTest {
        // When
        val actual = loadDataForMessageLabelAsBottomSheet(userId, messageId)

        // Then
        val expected = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = listOf(
                MailLabelTestData.customLabelOne, MailLabelTestData.customLabelTwo
            ).map {
                it.toUiModel(
                    FolderColorSettings(), emptyMap(), MailLabelId.System.Inbox
                ) as MailLabelUiModel.Custom
            }.toImmutableList(),
            selectedLabels = listOf(MailLabelTestData.customLabelOne.id.labelId).toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(messageId)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `should return empty bottom sheet action data when observing labels failed`() = runTest {
        // Given
        coEvery { observeCustomMailLabels(userId) } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val actual = loadDataForMessageLabelAsBottomSheet(userId, messageId)

        // Then
        val expected = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = emptyList<MailLabelUiModel.Custom>().toImmutableList(),
            selectedLabels = listOf(MailLabelTestData.customLabelOne.id.labelId).toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(messageId)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `should return empty selected labels list when observing the message failed`() = runTest {
        // Given
        every { observeMessageWithLabels(userId, messageId) } returns flowOf(DataError.Local.NoDataCached.left())

        // When
        val actual = loadDataForMessageLabelAsBottomSheet(userId, messageId)

        // Then
        val expected = LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
            customLabelList = listOf(
                MailLabelTestData.customLabelOne, MailLabelTestData.customLabelTwo
            ).map {
                it.toUiModel(
                    FolderColorSettings(), emptyMap(), MailLabelId.System.Inbox
                ) as MailLabelUiModel.Custom
            }.toImmutableList(),
            selectedLabels = emptyList<LabelId>().toImmutableList(),
            entryPoint = LabelAsBottomSheetEntryPoint.Message(messageId)
        )
        assertEquals(expected, actual)
    }
}
