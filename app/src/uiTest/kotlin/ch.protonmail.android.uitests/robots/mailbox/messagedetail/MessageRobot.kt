/*
 * Copyright (c) 2020 Proton Technologies AG
 * 
 * This file is part of ProtonMail.
 * 
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.uitests.robots.mailbox.messagedetail

import ch.protonmail.android.uitests.robots.mailbox.ApplyLabelRobotInterface
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.spam.SpamRobot
import ch.protonmail.android.uitests.robots.mailbox.trash.TrashRobot

/**
 * [MessageRobot] class contains actions and verifications for Message detail view functionality.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class MessageRobot {

    fun selectFolder(folderName: String): MessageRobot {
        return this
    }

    fun expandAttachments(): MessageRobot {
        return this
    }

    fun clickAttachment(attachmentFileName: String): MessageRobot {
        return this
    }

    fun clickLink(linkText: String): LinkNavigationDialogRobot {
        return LinkNavigationDialogRobot()
    }

    fun moveFromSpamToFolder(folderName: String): SpamRobot {
        return SpamRobot()
    }

    fun moveToTrash(): InboxRobot {
        return InboxRobot()
    }

    fun openActionSheet(): MessageActionSheet {
        return MessageActionSheet()
    }

    fun navigateUpToSearch(): SearchRobot {
        return SearchRobot()
    }

    fun navigateUpToSent(): SentRobot {
        return SentRobot()
    }

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot()
    }

    fun clickSendButtonFromDrafts(): DraftsRobot {
        return DraftsRobot()
    }

    fun clickLoadEmbeddedImagesButton(): MessageRobot {
        return this
    }

    class LabelsDialogRobot : ApplyLabelRobotInterface {

        override fun addLabel(name: String): LabelsDialogRobot {
            super.addLabel(name)
            return this
        }

        override fun selectLabelByName(name: String): LabelsDialogRobot {
            super.selectLabelByName(name)
            return this
        }

        override fun checkAlsoArchiveCheckBox(): LabelsDialogRobot {
            super.checkAlsoArchiveCheckBox()
            return this
        }

        override fun apply(): MessageRobot {
            super.apply()
            return MessageRobot()
        }

        override fun applyAndArchive(): MessageRobot {
            super.apply()
            return MessageRobot()
        }

        override fun closeLabelModal(): MessageRobot {
            super.closeLabelModal()
            return MessageRobot()
        }
    }

    class FoldersDialogRobot {

        fun clickCreateFolder(): AddFolderRobot {
            return AddFolderRobot()
        }

        fun moveMessageFromSpamToFolder(folderName: String): SpamRobot {
            selectFolder(folderName)
            return SpamRobot()
        }

        fun moveMessageFromTrashToFolder(folderName: String): TrashRobot {
            selectFolder(folderName)
            return TrashRobot()
        }

        fun moveMessageFromSentToFolder(folderName: String): SentRobot {
            selectFolder(folderName)
            return SentRobot()
        }

        fun moveMessageFromInboxToFolder(folderName: String): InboxRobot {
            selectFolder(folderName)
            return InboxRobot()
        }

        fun moveMessageFromMessageToFolder(folderName: String): MessageRobot {
            selectFolder(folderName)
            return MessageRobot()
        }

        private fun selectFolder(folderName: String) {}

        class Verify {

            fun folderExistsInFoldersList(folderName: String) {}
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class AddFolderRobot {

        fun addFolderWithName(name: String): FoldersDialogRobot = typeName(name).saveNewFolder()

        private fun saveNewFolder(): FoldersDialogRobot {
            return FoldersDialogRobot()
        }

        private fun typeName(folderName: String): AddFolderRobot {
            return this
        }
    }

    class MessageActionSheet {

        fun reply(): ComposerRobot {
            return ComposerRobot()
        }

        fun replyAll(): ComposerRobot {
            return ComposerRobot()
        }

        fun forward(): ComposerRobot {
            return ComposerRobot()
        }

        fun openFoldersModal(): FoldersDialogRobot {
            return FoldersDialogRobot()
        }

        fun openLabelsModal(): LabelsDialogRobot {
            return LabelsDialogRobot()
        }

        fun viewHeaders(): ViewHeadersRobot {
            return ViewHeadersRobot()
        }
    }

    class LinkNavigationDialogRobot {

        class Verify {

            fun linkIsPresentInDialogMessage(link: String) {}
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class Verify {

        fun labelAdded(labelName: String) {}

        fun publicKeyIsAttached(publicKey: String) {}

        fun messageContainsAttachment() {}

        fun messageContainsOneAttachment() {}

        fun messageContainsTwoAttachments() {}

        fun quotedHeaderShown() {}

        fun attachmentsNotAdded() {}

        fun attachmentsAdded() {}

        fun pgpIconShown() {}

        fun pgpEncryptedMessageDecrypted() {}

        fun pgpSignedMessageDecrypted() {}

        fun messageWebViewContainerShown() {}

        fun loadEmbeddedImagesButtonIsGone() {}

        fun showRemoteContentButtonIsGone() {}

        fun intentWithActionFileNameAndMimeTypeSent(mimeType: String) {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
