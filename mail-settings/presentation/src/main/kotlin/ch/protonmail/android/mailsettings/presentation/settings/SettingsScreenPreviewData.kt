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

package ch.protonmail.android.mailsettings.presentation.settings

import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailsession.domain.model.Percent
import ch.protonmail.android.mailsession.presentation.model.AccountInformationUiModel
import ch.protonmail.android.mailsession.presentation.model.StorageQuotaUiModel
import ch.protonmail.android.design.compose.model.VisibilityUiModel
import me.proton.core.domain.entity.UserId

object SettingsScreenPreviewData {

    val Data = SettingsState.Data(
        userId = UserId(id = "123"),
        accountInfoUiModel = AccountInformationUiModel("ProtonUser", "user@proton.ch", null, UserId("123")),
        storageQuotaUiModel = VisibilityUiModel.Visible(
            StorageQuotaUiModel(
                usagePercent = Percent(80.0),
                maxStorage = "15 MB",
                isAboveAlertThreshold = true
            )
        ),
        appInformation = AppInformation(appVersionName = "6.0.0-alpha"),
        isContentSearchEnabled = true
    )

    val Actions = MainSettingsScreen.Actions(
        onAccountClick = {},
        onAppSettingsClick = {},
        onEmailSettingsClick = {},
        onFolderAndLabelSettingsClicked = {},
        onSpamFilterSettingsClicked = {},
        onPrivacyAndSecuritySettingsClicked = {},
        onSecurityKeysClicked = {},
        onPasswordManagementClicked = {},
        onAccountStorageClicked = {},
        onBackClick = {},
        onSignatureClicked = {},
        onContentSearchSettingsClick = {}
    )
}
