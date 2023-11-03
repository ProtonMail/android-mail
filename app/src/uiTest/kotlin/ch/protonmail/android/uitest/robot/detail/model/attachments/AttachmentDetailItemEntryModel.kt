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

package ch.protonmail.android.uitest.robot.detail.model.attachments

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentFooterTestTags
import ch.protonmail.android.mailmessage.presentation.ui.AttachmentItemTestTags
import ch.protonmail.android.uitest.util.awaitDisplayed
import ch.protonmail.android.uitest.util.awaitHidden
import ch.protonmail.android.uitest.util.child

internal class AttachmentDetailItemEntryModel(index: Int, parent: SemanticsNodeInteraction) {

    private val item = parent.child {
        hasTestTag("${AttachmentFooterTestTags.Item}$index")
    }

    private val icon = item.child {
        hasTestTag(AttachmentItemTestTags.Icon)
    }

    private val loader = item.child {
        hasTestTag(AttachmentItemTestTags.Loader)
    }

    private val name = item.child {
        hasTestTag(AttachmentItemTestTags.Name)
    }

    private val extension = item.child {
        hasTestTag(AttachmentItemTestTags.Extension)
    }

    private val deleteIcon = item.child {
        hasTestTag(AttachmentItemTestTags.Delete)
    }

    private val size = item.child {
        hasTestTag(AttachmentItemTestTags.Size)
    }

    // region actions
    fun tapItem() {
        name.performClick()
    }
    // endregion

    // region verification
    fun hasIcon(): AttachmentDetailItemEntryModel = apply {
        icon.assertIsDisplayed()
        loader.assertDoesNotExist()
    }

    fun hasLoaderIcon(): AttachmentDetailItemEntryModel = apply {
        icon.awaitHidden().assertDoesNotExist()
        loader.awaitDisplayed().assertIsDisplayed()
    }

    fun hasNoLoaderIcon(): AttachmentDetailItemEntryModel = apply {
        loader.awaitHidden().assertDoesNotExist()
    }

    fun hasName(value: String): AttachmentDetailItemEntryModel = apply {
        val (fileName, fileExtension) = value.split(".")
        name.assertTextEquals(fileName)
        extension.assertTextEquals(".$fileExtension")
    }

    fun hasDeleteIcon(): AttachmentDetailItemEntryModel = apply {
        deleteIcon.assertIsDisplayed()
    }

    fun hasNoDeleteIcon(): AttachmentDetailItemEntryModel = apply {
        deleteIcon.assertDoesNotExist()
    }

    fun hasSize(value: String): AttachmentDetailItemEntryModel = apply {
        size.assertTextEquals(value)
    }
    // endregion

    // region utility
    fun waitUntilShown() = apply {
        item.awaitDisplayed()
    }
    // endregion
}
