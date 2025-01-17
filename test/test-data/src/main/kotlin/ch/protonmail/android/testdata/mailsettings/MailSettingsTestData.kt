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

package ch.protonmail.android.testdata.mailsettings

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.AlmostAllMail
import me.proton.core.mailsettings.domain.entity.ComposerMode
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MessageButtons
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PMSignature
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.entity.ViewLayout
import me.proton.core.mailsettings.domain.entity.ViewMode

object MailSettingsTestData {

    val mailSettings = buildMailSettings()

    fun buildMailSettings(
        showImages: IntEnum<ShowImage>? = null,
        showMoved: IntEnum<ShowMoved>? = null,
        almostAllMail: IntEnum<AlmostAllMail>? = null,
        swipeLeft: SwipeAction? = null,
        swipeRight: SwipeAction? = null,
        enableFolderColor: Boolean = true,
        inheritParentFolderColor: Boolean = true,
        confirmLink: Boolean = true,
        autoDeleteSpamAndTrashDays: Int? = null
    ) = MailSettings(
        userId = UserIdTestData.userId,
        displayName = "displayName",
        signature = "Signature",
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = showImages,
        showMoved = showMoved,
        viewMode = IntEnum(1, ViewMode.NoConversationGrouping),
        viewLayout = IntEnum(1, ViewLayout.Row),
        swipeLeft = swipeLeft?.let { IntEnum(it.value, it) },
        swipeRight = swipeRight?.let { IntEnum(it.value, it) },
        shortcuts = true,
        pmSignature = IntEnum(1, PMSignature.Disabled),
        numMessagePerPage = 1,
        draftMimeType = StringEnum("text/plain", MimeType.PlainText),
        receiveMimeType = StringEnum("text/plain", MimeType.PlainText),
        showMimeType = StringEnum("text/plain", MimeType.PlainText),
        enableFolderColor = enableFolderColor,
        inheritParentFolderColor = inheritParentFolderColor,
        rightToLeft = true,
        attachPublicKey = true,
        sign = true,
        pgpScheme = IntEnum(1, PackageType.ProtonMail),
        promptPin = true,
        stickyLabels = true,
        confirmLink = confirmLink,
        autoDeleteSpamAndTrashDays = autoDeleteSpamAndTrashDays,
        almostAllMail = almostAllMail,
        mobileSettings = null
    )
}
