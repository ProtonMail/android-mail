package ch.protonmail.android.mailconversation.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationActionRepository
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.sample.LabelIdSample
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetConversationMoveToLocationsTest {

    private val actionRepository = mockk<ConversationActionRepository>()

    private val getConversationMoveToLocations = GetConversationMoveToLocations(
        actionRepository
    )

    @Test
    fun `returns available actions when repo succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = LabelIdSample.Trash
        val conversationIds = listOf(ConversationIdSample.Newsletter)
        val expected = listOf(
            MailLabel.System(MailLabelId.System(LabelId("2")), SystemLabelId.Archive, 0),
            MailLabel.System(MailLabelId.System(LabelId("3")), SystemLabelId.Trash, 0)
        )
        coEvery {
            actionRepository.getMoveToLocations(userId, labelId, conversationIds)
        } returns expected.right()

        // When
        val actual = getConversationMoveToLocations(userId, labelId, conversationIds)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns error when repository fails to get available actions`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = LabelIdSample.Trash
        val conversationIds = listOf(ConversationIdSample.Newsletter)
        val expected = DataError.Local.CryptoError.left()
        coEvery { actionRepository.getMoveToLocations(userId, labelId, conversationIds) } returns expected

        // When
        val actual = getConversationMoveToLocations(userId, labelId, conversationIds)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `returns custom folders included by the repository`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = LabelIdSample.Trash
        val conversationIds = listOf(ConversationIdSample.Newsletter)
        val customF0 = buildCustomFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1"))
        val customF01 = buildCustomFolder("0.1", level = 1, order = 0, parent = customF0)
        val moveToLocations = listOf(
            MailLabel.System(MailLabelId.System(LabelId("2")), SystemLabelId.Archive, 0),
            MailLabel.System(MailLabelId.System(LabelId("3")), SystemLabelId.Trash, 0)
        )
        val customFolders = listOf(customF0, customF01)
        val expected = moveToLocations + customFolders
        coEvery {
            actionRepository.getMoveToLocations(userId, labelId, conversationIds)
        } returns expected.right()

        // When
        val actual = getConversationMoveToLocations(userId, labelId, conversationIds)

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `returns only system locations when repository has no custom ones`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = LabelIdSample.Trash
        val conversationIds = listOf(ConversationIdSample.Newsletter)
        val expectedSystemActions = listOf(
            MailLabel.System(MailLabelId.System(LabelId("2")), SystemLabelId.Archive, 0)
        )
        coEvery {
            actionRepository.getMoveToLocations(userId, labelId, conversationIds)
        } returns expectedSystemActions.right()

        // When
        val actual = getConversationMoveToLocations(userId, labelId, conversationIds)

        // Then
        assertEquals(expectedSystemActions.right(), actual)
    }
}
