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

package ch.protonmail.android.mailsettings.presentation.appsettings

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailpinlock.model.Protection
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.SwipeNextPreference
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.testdata.AppSettingsTestData
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class AppSettingsUiModelMapperTest(
    @Suppress("unused") private val testName: String,
    private val args: Arguments,
    private val expectedUiModel: AppSettingsUiModel
) {

    @Test
    fun `should map to ui model`() {
        val uiModel = AppSettingsUiModelMapper.toUiModel(
            appSettings = args.appSettings,
            notificationsEnabled = args.notificationsEnabled,
            appIconDescription = args.appName,
            isEmailCategoriesEnabled = args.isEmailCategoriesEnabled
        )
        assertEquals(expectedUiModel, uiModel)
    }

    companion object {

        val baseAppSettings = AppSettingsTestData.appSettings
        val appIconName = TextUiModel("Proton Mail")

        val baseUiModel = AppSettingsUiModel(
            autoLockEnabled = false,
            alternativeRoutingEnabled = true,
            isEmailCategoriesEnabled = true,
            customLanguage = null,
            deviceContactsEnabled = true,
            theme = TextUiModel.TextRes(R.string.mail_settings_system_default),
            notificationsEnabledStatus = TextUiModel(R.string.notifications_on),
            appIconName = appIconName,
            swipeNextEnabled = false
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                "to app settings with default values",
                Arguments(
                    appSettings = baseAppSettings,
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel
            ),
            arrayOf(
                "to app settings with light theme",
                Arguments(
                    appSettings = baseAppSettings.copy(theme = Theme.LIGHT),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(theme = TextUiModel.TextRes(R.string.mail_settings_theme_light))
            ),
            arrayOf(
                "to app settings with dark theme",
                Arguments(
                    appSettings = baseAppSettings.copy(theme = Theme.DARK),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(theme = TextUiModel.TextRes(R.string.mail_settings_theme_dark))
            ),
            arrayOf(
                "to app settings with custom language",
                Arguments(
                    appSettings = baseAppSettings.copy(customAppLanguage = "Custom language"),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(customLanguage = "Custom language")
            ),
            arrayOf(
                "to app settings with pin lock enabled",
                Arguments(
                    appSettings = baseAppSettings.copy(autolockProtection = Protection.Pin),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(autoLockEnabled = true)
            ),
            arrayOf(
                "to app settings with biometrics lock enabled",
                Arguments(
                    appSettings = baseAppSettings.copy(autolockProtection = Protection.Biometrics),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(autoLockEnabled = true)
            ),
            arrayOf(
                "to app settings with no lock enabled",
                Arguments(
                    appSettings = baseAppSettings,
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(autoLockEnabled = false)
            ),
            arrayOf(
                "to app settings with alternative routing enabled",
                Arguments(
                    appSettings = baseAppSettings.copy(hasAlternativeRouting = true),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(alternativeRoutingEnabled = true)
            ),
            arrayOf(
                "to app settings with device contacts enabled",
                Arguments(
                    appSettings = baseAppSettings.copy(hasCombinedContactsEnabled = true),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(deviceContactsEnabled = true)
            ),
            arrayOf(
                "to app settings with notifications off",
                Arguments(
                    appSettings = baseAppSettings,
                    notificationsEnabled = false,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(notificationsEnabledStatus = TextUiModel(R.string.notifications_off))
            ),

            arrayOf(
                "to app settings with swipe pref on",
                Arguments(
                    appSettings = baseAppSettings.copy(swipeNextPreference = SwipeNextPreference.Enabled),
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = true
                ),
                baseUiModel.copy(swipeNextEnabled = true)
            ),
            arrayOf(
                "to app settings with email categories disabled",
                Arguments(
                    appSettings = baseAppSettings,
                    notificationsEnabled = true,
                    isEmailCategoriesEnabled = false
                ),
                baseUiModel.copy(isEmailCategoriesEnabled = false)
            )
        )

        data class Arguments(
            val appSettings: AppSettings,
            val notificationsEnabled: Boolean,
            val isEmailCategoriesEnabled: Boolean,
            val appName: TextUiModel = appIconName
        )
    }
}
