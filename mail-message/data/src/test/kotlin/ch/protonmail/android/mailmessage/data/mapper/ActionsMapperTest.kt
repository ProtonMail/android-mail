package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import org.junit.Test
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MovableSystemFolderAction
import uniffi.mail_uniffi.MoveAction
import kotlin.test.assertEquals

class ActionsMapperTest {

    @Test
    fun `maps move to system folder actions to mail label`() {
        // Given
        val archiveLocalId = Id(1uL)
        val spamLocalId = Id(1uL)
        val archive = MovableSystemFolderAction(archiveLocalId, MovableSystemFolder.ARCHIVE)
        val spam = MovableSystemFolderAction(spamLocalId, MovableSystemFolder.SPAM)
        val systemFolderActions = listOf(MoveAction.SystemFolder(archive), MoveAction.SystemFolder(spam))
        val expected = listOf(
            MailLabel.System(MailLabelId.System(archiveLocalId.toLabelId()), SystemLabelId.Archive, 0),
            MailLabel.System(MailLabelId.System(spamLocalId.toLabelId()), SystemLabelId.Spam, 0)
        )

        // When
        val actual = systemFolderActions.toMailLabels()

        // Then
        assertEquals(expected, actual)
    }
}
