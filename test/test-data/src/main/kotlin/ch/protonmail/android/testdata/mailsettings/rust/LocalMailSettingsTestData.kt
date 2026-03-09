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

package ch.protonmail.android.testdata.mailsettings.rust

import ch.protonmail.android.mailcommon.data.mapper.LocalComposerDirection
import ch.protonmail.android.mailcommon.data.mapper.LocalComposerMode
import ch.protonmail.android.mailcommon.data.mapper.LocalMailSettings
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageButtons
import ch.protonmail.android.mailcommon.data.mapper.LocalMimeType
import ch.protonmail.android.mailcommon.data.mapper.LocalPgpScheme
import ch.protonmail.android.mailcommon.data.mapper.LocalPmSignature
import ch.protonmail.android.mailcommon.data.mapper.LocalShowImages
import ch.protonmail.android.mailcommon.data.mapper.LocalShowMoved
import ch.protonmail.android.mailcommon.data.mapper.LocalSwipeAction
import ch.protonmail.android.mailcommon.data.mapper.LocalViewLayout
import ch.protonmail.android.mailcommon.data.mapper.LocalViewMode
import uniffi.mail_uniffi.AlmostAllMail
import uniffi.mail_uniffi.NextMessageOnMove

object LocalMailSettingsTestData {

    val mailSettings = LocalMailSettings(
        displayName = "displayName",
        signature = "Signature",
        theme = "theme",
        autoSaveContacts = true,
        composerMode = LocalComposerMode.MAXIMIZED,
        messageButtons = LocalMessageButtons.UNREAD_FIRST,
        showImages = LocalShowImages.DO_NOT_AUTO_LOAD,
        showMoved = LocalShowMoved.KEEP_BOTH,
        autoDeleteSpamAndTrashDays = null,
        almostAllMail = AlmostAllMail.ALL_MAIL,
        nextMessageOnMove = NextMessageOnMove.DISABLED_EXPLICIT,
        viewMode = LocalViewMode.MESSAGES,
        viewLayout = LocalViewLayout.ROW,
        swipeLeft = LocalSwipeAction.TRASH,
        swipeRight = LocalSwipeAction.ARCHIVE,
        shortcuts = false,
        pmSignature = LocalPmSignature(1.toUByte()),
        pmSignatureReferralLink = false,
        imageProxy = 0.toUInt(),
        numMessagePerPage = 1.toUInt(),
        draftMimeType = LocalMimeType.TEXT_PLAIN,
        receiveMimeType = LocalMimeType.TEXT_PLAIN,
        showMimeType = LocalMimeType.TEXT_PLAIN,
        enableFolderColor = true,
        inheritParentFolderColor = true,
        submissionAccess = false,
        rightToLeft = LocalComposerDirection.RIGHT_TO_LEFT,
        attachPublicKey = true,
        sign = true,
        pgpScheme = LocalPgpScheme.MIME,
        promptPin = true,
        stickyLabels = true,
        confirmLink = true,
        delaySendSeconds = 0.toUInt(),
        fontFace = "fontFace",
        spamAction = null,
        blockSenderConfirmation = false,
        mobileSettings = null,
        hideRemoteImages = false,
        hideSenderImages = false,
        hideEmbeddedImages = false
    )

}
