package ch.protonmail.android.mailmessage.data.mapper

import android.graphics.Color
import ch.protonmail.android.maillabel.data.mapper.toLabelId
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import uniffi.mail_uniffi.CategoryDestination
import uniffi.mail_uniffi.Id
import uniffi.mail_uniffi.InboxDestination
import uniffi.mail_uniffi.InboxFolderAction
import uniffi.mail_uniffi.LabelColor
import uniffi.mail_uniffi.MovableCategoryFolder
import uniffi.mail_uniffi.MovableSystemFolder
import uniffi.mail_uniffi.MovableSystemFolderAction
import uniffi.mail_uniffi.MoveAction
import uniffi.mail_uniffi.MoveDestination
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActionsMapperTest {

    private val moveDestinationMapper = MoveDestinationMapper()

    @Before
    fun setUp() {
        mockkStatic(Color::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun `maps move to destinations to mail labels`() {
        // Given
        val archiveLocalId = Id(1uL)
        val spamLocalId = Id(2uL)
        val archive = MovableSystemFolderAction(archiveLocalId, MovableSystemFolder.ARCHIVE)
        val spam = MovableSystemFolderAction(spamLocalId, MovableSystemFolder.SPAM)
        val systemFolderActions = listOf(MoveAction.SystemFolder(archive), MoveAction.SystemFolder(spam))
        val expected = listOf(
            MailLabel.System(MailLabelId.System(archiveLocalId.toLabelId()), SystemLabelId.Archive, 0),
            MailLabel.System(MailLabelId.System(spamLocalId.toLabelId()), SystemLabelId.Spam, 0)
        )

        // When
        val actual = moveDestinationMapper(systemFolderActions)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `maps custom folder move actions to custom mail labels hierarchy`() {
        // Given
        every { Color.parseColor("#FFFFFF") } returns 0xFFFFFFFF.toInt()
        every { Color.parseColor("#EEEEEE") } returns 0xFFEEEEEE.toInt()
        val rootId = Id(11uL)
        val childId = Id(12uL)
        val childFolder = CustomFolderAction(childId, "Child", LabelColor("#EEE"), emptyList())
        val actions = listOf(
            MoveAction.CustomFolder(
                CustomFolderAction(rootId, "Root", LabelColor("#FFF"), listOf(childFolder))
            )
        )

        // When
        val actual = moveDestinationMapper(actions)

        // Then
        val root = actual[0] as MailLabel.Custom
        val child = actual[1] as MailLabel.Custom
        assertEquals(MailLabelId.Custom.Folder(rootId.toLabelId()), root.id)
        assertEquals(listOf(MailLabelId.Custom.Folder(childId.toLabelId())), root.children)
        assertEquals(0xFFFFFFFF.toInt(), root.color)
        assertEquals(0xFFEEEEEE.toInt(), child.color)
        assertEquals(0, root.level)
        assertEquals(root, child.parent)
        assertEquals(1, child.level)
    }

    @Test
    fun `maps inbox move action to inbox with categories`() {
        // Given
        val inbox = InboxFolderAction(
            localId = Id(20uL),
            name = MovableSystemFolder.INBOX,
            categories = listOf(
                CategoryDestination(Id(21uL), MovableCategoryFolder.CATEGORY_SOCIAL)
            )
        )

        // When
        val actual = moveDestinationMapper(listOf(MoveAction.Inbox(inbox)))

        // Then
        val expected = listOf(
            MailLabel.System(
                id = MailLabelId.System(Id(20uL).toLabelId()),
                systemLabelId = SystemLabelId.Inbox,
                order = 0,
                categories = listOf(
                    MailLabel.Category(
                        id = MailLabelId.Category(Id(21uL).toLabelId()),
                        categorySystemLabelId = CategorySystemLabelId.Social,
                        order = 0
                    )
                )
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `maps all category destinations to their category system labels`() {
        // Given
        val inbox = InboxDestination(
            localId = Id(20uL),
            name = MovableSystemFolder.INBOX,
            categories = listOf(
                CategoryDestination(Id(1uL), MovableCategoryFolder.CATEGORY_DEFAULT),
                CategoryDestination(Id(2uL), MovableCategoryFolder.CATEGORY_SOCIAL),
                CategoryDestination(Id(3uL), MovableCategoryFolder.CATEGORY_PROMOTIONS),
                CategoryDestination(Id(4uL), MovableCategoryFolder.CATEGORY_UPDATES),
                CategoryDestination(Id(5uL), MovableCategoryFolder.CATEGORY_NEWSLETTER),
                CategoryDestination(Id(6uL), MovableCategoryFolder.CATEGORY_TRANSACTIONS)
            )
        )

        // When
        val actual = moveDestinationMapper(listOf(MoveDestination.Inbox(inbox)))

        // Then
        val expectedCategories = listOf(
            MailLabel.Category(MailLabelId.Category(Id(1uL).toLabelId()), CategorySystemLabelId.Primary, 0),
            MailLabel.Category(MailLabelId.Category(Id(2uL).toLabelId()), CategorySystemLabelId.Social, 0),
            MailLabel.Category(MailLabelId.Category(Id(3uL).toLabelId()), CategorySystemLabelId.Promotions, 0),
            MailLabel.Category(MailLabelId.Category(Id(4uL).toLabelId()), CategorySystemLabelId.Updates, 0),
            MailLabel.Category(MailLabelId.Category(Id(5uL).toLabelId()), CategorySystemLabelId.Newsletter, 0),
            MailLabel.Category(MailLabelId.Category(Id(6uL).toLabelId()), CategorySystemLabelId.Transactions, 0)
        )
        val inboxLabel = actual.single() as MailLabel.System
        assertEquals(SystemLabelId.Inbox, inboxLabel.systemLabelId)
        assertEquals(expectedCategories, inboxLabel.categories)
    }

    @Test
    fun `maps inbox without categories to null categories`() {
        // Given
        val inbox = InboxDestination(
            localId = Id(20uL),
            name = MovableSystemFolder.INBOX,
            categories = emptyList()
        )

        // When
        val actual = moveDestinationMapper(listOf(MoveDestination.Inbox(inbox)))

        // Then
        val inboxLabel = actual.single() as MailLabel.System
        assertNull(inboxLabel.categories)
    }
}
