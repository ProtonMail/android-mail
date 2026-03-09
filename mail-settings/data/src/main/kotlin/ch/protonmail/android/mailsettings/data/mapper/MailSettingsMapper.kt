/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.data.mapper

import ch.protonmail.android.mailcommon.data.mapper.LocalAlmostAllMail
import ch.protonmail.android.mailcommon.data.mapper.LocalComposerDirection
import ch.protonmail.android.mailcommon.data.mapper.LocalComposerMode
import ch.protonmail.android.mailcommon.data.mapper.LocalMailSettings
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageButtons
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeType
import ch.protonmail.android.mailcommon.data.mapper.LocalMobileSettings
import ch.protonmail.android.mailcommon.data.mapper.LocalPgpScheme
import ch.protonmail.android.mailcommon.data.mapper.LocalPmSignature
import ch.protonmail.android.mailcommon.data.mapper.LocalShowMoved
import ch.protonmail.android.mailcommon.data.mapper.LocalSwipeAction
import ch.protonmail.android.mailcommon.data.mapper.LocalViewLayout
import ch.protonmail.android.mailcommon.data.mapper.LocalViewMode
import ch.protonmail.android.mailcommon.domain.model.DeprecatedId
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ActionsToolbarSetting
import me.proton.core.mailsettings.domain.entity.AlmostAllMail
import me.proton.core.mailsettings.domain.entity.ComposerMode
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MessageButtons
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.MobileSettings
import me.proton.core.mailsettings.domain.entity.PMSignature
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.entity.ToolbarAction
import me.proton.core.mailsettings.domain.entity.ViewLayout
import me.proton.core.mailsettings.domain.entity.ViewMode
import timber.log.Timber
import uniffi.mail_uniffi.MobileSetting

object MailSettingsMapper {

    fun LocalMailSettings.toMailSettings(): MailSettings {
        return MailSettings(
            userId = DeprecatedId.UserId,
            displayName = displayName,
            signature = signature,
            autoSaveContacts = autoSaveContacts,
            composerMode = composerMode.toComposerMode(),
            messageButtons = messageButtons.toMessageButtons(),
            showImages = this.toShowImage(),
            showMoved = showMoved.toShowMoved(),
            viewMode = viewMode.toViewMode(),
            viewLayout = viewLayout.toViewLayout(),
            swipeLeft = swipeLeft.toSwipeAction(),
            swipeRight = swipeRight.toSwipeAction(),
            shortcuts = shortcuts,
            pmSignature = pmSignature.toPMSignature(),
            numMessagePerPage = numMessagePerPage.toInt(),
            draftMimeType = draftMimeType.toMimeType(),
            receiveMimeType = receiveMimeType.toMimeType(),
            showMimeType = showMimeType.toMimeType(),
            enableFolderColor = enableFolderColor,
            inheritParentFolderColor = inheritParentFolderColor,
            rightToLeft = rightToLeft.toAndroidComposerDirection(),
            attachPublicKey = attachPublicKey,
            sign = sign,
            pgpScheme = pgpScheme.toPackageType(),
            promptPin = promptPin,
            stickyLabels = stickyLabels,
            confirmLink = confirmLink,
            autoDeleteSpamAndTrashDays = autoDeleteSpamAndTrashDays?.toInt(),
            almostAllMail = almostAllMail.toAlmostAllMail(),
            mobileSettings = mobileSettings?.toMobileSettings()
        )
    }

    private fun LocalComposerMode.toComposerMode(): IntEnum<ComposerMode>? = ComposerMode.enumOf(this.value.toInt())

    private fun LocalMessageButtons.toMessageButtons(): IntEnum<MessageButtons>? =
        MessageButtons.enumOf(this.value.toInt())

    private fun LocalMailSettings.toShowImage(): IntEnum<ShowImage>? {
        val show = when {
            hideRemoteImages && hideEmbeddedImages -> ShowImage.None
            !hideRemoteImages && hideEmbeddedImages -> ShowImage.Remote
            hideRemoteImages && !hideEmbeddedImages -> ShowImage.Embedded
            else -> ShowImage.Both
        }
        return ShowImage.enumOf(show.value)
    }

    private fun LocalShowMoved.toShowMoved(): IntEnum<ShowMoved>? = ShowMoved.enumOf(this.value.toInt())

    private fun LocalViewMode.toViewMode(): IntEnum<ViewMode>? = ViewMode.enumOf(this.value.toInt())

    private fun LocalViewLayout.toViewLayout(): IntEnum<ViewLayout>? = ViewLayout.enumOf(this.value.toInt())

    private fun LocalSwipeAction.toSwipeAction(): IntEnum<SwipeAction>? = when (this) {
        uniffi.mail_uniffi.SwipeAction.TRASH -> IntEnum(SwipeAction.Trash.value, SwipeAction.Trash)
        uniffi.mail_uniffi.SwipeAction.SPAM -> IntEnum(SwipeAction.Spam.value, SwipeAction.Spam)
        uniffi.mail_uniffi.SwipeAction.STAR -> IntEnum(SwipeAction.Star.value, SwipeAction.Star)
        uniffi.mail_uniffi.SwipeAction.ARCHIVE -> IntEnum(SwipeAction.Archive.value, SwipeAction.Archive)
        uniffi.mail_uniffi.SwipeAction.MARK_AS_READ -> IntEnum(SwipeAction.MarkRead.value, SwipeAction.MarkRead)
        uniffi.mail_uniffi.SwipeAction.LABEL_AS -> IntEnum(SwipeAction.LabelAs.value, SwipeAction.LabelAs)
        uniffi.mail_uniffi.SwipeAction.MOVE_TO -> IntEnum(SwipeAction.MoveTo.value, SwipeAction.MoveTo)
        uniffi.mail_uniffi.SwipeAction.NO_ACTION -> IntEnum(SwipeAction.None.value, SwipeAction.None)
    }

    private fun LocalPgpScheme.toPackageType(): IntEnum<PackageType>? = PackageType.enumOf(this.value.toInt())

    private fun LocalComposerDirection.toAndroidComposerDirection(): Boolean? {
        return when (this) {
            LocalComposerDirection.LEFT_TO_RIGHT -> false
            LocalComposerDirection.RIGHT_TO_LEFT -> true
        }
    }

    private fun LocalPmSignature.toPMSignature(): IntEnum<PMSignature>? {
        val intValue = this.value.toInt()
        return PMSignature.enumOf(intValue)
    }

    private fun LocalMimeType.toMimeType(): StringEnum<MimeType>? {
        val mimeType = when (this) {
            uniffi.mail_uniffi.MimeType.TEXT_HTML -> MimeType.Html
            uniffi.mail_uniffi.MimeType.TEXT_PLAIN -> MimeType.PlainText
            uniffi.mail_uniffi.MimeType.MULTIPART_MIXED -> MimeType.Mixed
            uniffi.mail_uniffi.MimeType.APPLICATION_JSON,
            uniffi.mail_uniffi.MimeType.APPLICATION_PDF,
            uniffi.mail_uniffi.MimeType.MESSAGE_RFC822,
            uniffi.mail_uniffi.MimeType.MULTIPART_RELATED -> {
                Timber.w("rust-mail-settings: Mapping from unsupported Mime type $this. Fallback to text/plain")
                MimeType.PlainText
            }
        }
        return MimeType.enumOf(mimeType.value)
    }

    private fun LocalAlmostAllMail.toAlmostAllMail(): IntEnum<AlmostAllMail>? {
        val intValue = this.value.toInt()
        return AlmostAllMail.enumOf(intValue)
    }

    private fun LocalMobileSettings.toMobileSettings(): MobileSettings {
        fun MobileSetting.toMobileSetting(): ActionsToolbarSetting {
            val actions = this.actions.map { ToolbarAction.enumOf(it.toString()) } // ET-4237
            return ActionsToolbarSetting(this.isCustom, actions)
        }

        return MobileSettings(
            listToolbar = this.listToolbar.toMobileSetting(),
            conversationToolbar = this.conversationToolbar.toMobileSetting(),
            messageToolbar = this.messageToolbar.toMobileSetting()
        )
    }
}
