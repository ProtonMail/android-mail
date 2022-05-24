/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.testdata.mailsettings

import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
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
        swipeLeft: SwipeAction? = null,
        swipeRight: SwipeAction? = null,
        enableFolderColor: Boolean = true,
        inheritParentFolderColor: Boolean = true,
    ) = MailSettings(
        userId = UserIdTestData.userId,
        displayName = "displayName",
        signature = "Signature",
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(1, ShowMoved.Drafts),
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
        confirmLink = true
    )
}
