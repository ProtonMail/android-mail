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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.isReservedSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetRootLabel
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemLocationUiModel
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

/**
 * Defines the list of locations for which a mailbox item should show a
 * location icon, based on the currently selected location (mailbox user is looking at)
 */
class GetMailboxItemLocationIcons @Inject constructor(
    private val selectedMailLabelId: SelectedMailLabelId,
    private val colorMapper: ColorMapper,
    private val getRootLabel: GetRootLabel
) {

    suspend operator fun invoke(
        mailboxItem: MailboxItem,
        folderColorSettings: FolderColorSettings,
        isShowingSearchResults: Boolean
    ): Result {
        if (!currentLocationShouldShowIcons() && !isShowingSearchResults) {
            return Result.None
        }

        val icons = getLocationIcons(mailboxItem, folderColorSettings)
        if (icons.isEmpty()) {
            // Having no icons can happen when an item was in a custom folder which got
            // deleted. Such item is now only in all mail and no other location.
            // This is handled by showing no location icons, product discussion on alternatives ongoing
            return Result.None
        }
        return Result.Icons(icons.first(), icons.getOrNull(1), icons.getOrNull(2))
    }

    private suspend fun getLocationIcons(
        mailboxItem: MailboxItem,
        folderColorSettings: FolderColorSettings
    ): MutableList<MailboxItemLocationUiModel> {
        val icons = mutableListOf<MailboxItemLocationUiModel>()
        SystemLabelId.exclusiveList.forEach { systemLabelId ->
            if (mailboxItem.labelIds.contains(systemLabelId.labelId)) {
                val iconDrawable = SystemLabelId.enumOf(systemLabelId.labelId.id).iconRes()
                icons.add(MailboxItemLocationUiModel(iconDrawable))
            }

            if (systemLabelId == SystemLabelId.Spam) {
                mailboxItem.labels.firstOrNull {
                    it.type == LabelType.MessageFolder && !it.labelId.isReservedSystemLabelId()
                }?.let {
                    when (folderColorSettings.useFolderColor) {
                        true -> icons.add(
                            MailboxItemLocationUiModel(
                                icon = R.drawable.ic_proton_folder_filled,
                                color = getLocationIconColor(it.userId, it, folderColorSettings)
                            )
                        )

                        false -> icons.add(MailboxItemLocationUiModel(R.drawable.ic_proton_folder))
                    }
                }
            }
        }

        return icons
    }

    private suspend fun getLocationIconColor(
        userId: UserId,
        label: Label,
        folderColorSettings: FolderColorSettings
    ): Color {
        val colorToMap = when {
            folderColorSettings.inheritParentFolderColor -> getRootLabel(userId, label).color
            else -> label.color
        }
        return colorMapper.toColor(colorToMap).getOrElse { Color.Unspecified }
    }

    private fun currentLocationShouldShowIcons(): Boolean {
        val currentLocation = selectedMailLabelId.flow.value

        return currentLocation is MailLabelId.System.Starred ||
            currentLocation is MailLabelId.System.AllMail ||
            currentLocation is MailLabelId.System.AlmostAllMail ||
            currentLocation is MailLabelId.Custom.Label
    }

    sealed interface Result {
        data object None : Result
        data class Icons(
            val first: MailboxItemLocationUiModel,
            val second: MailboxItemLocationUiModel? = null,
            val third: MailboxItemLocationUiModel? = null
        ) : Result
    }
}
