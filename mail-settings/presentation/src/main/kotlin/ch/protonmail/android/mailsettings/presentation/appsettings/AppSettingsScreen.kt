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

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonAppSettingsItemInvert
import ch.protonmail.android.design.compose.component.ProtonAppSettingsItemNorm
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonMainSettingsIcon
import ch.protonmail.android.design.compose.component.ProtonSettingsDetailsAppBar
import ch.protonmail.android.design.compose.component.ProtonSettingsToggleItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsHeader
import ch.protonmail.android.mailsettings.presentation.settings.SettingsItemDivider
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility

const val TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM = "AlternativeRoutingToggleItem"

@Composable
fun AppSettingsScreen(modifier: Modifier = Modifier, actions: AppSettingsScreen.Actions) {
    AppSettingsScreenContent(modifier, actions)
}

@Composable
private fun AppSettingsScreenContent(
    modifier: Modifier = Modifier,
    actions: AppSettingsScreen.Actions,
    viewModel: AppSettingsViewModel = hiltViewModel()
) {
    when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
        is AppSettingsState.Data -> {
            AppSettingsScreenContent(
                modifier = modifier,
                actions = actions,
                state = state,
                onIntent = { viewModel.submit(it) }
            )
        }

        is AppSettingsState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun AppSettingsScreenContent(
    modifier: Modifier = Modifier,
    actions: AppSettingsScreen.Actions,
    state: AppSettingsState.Data,
    onIntent: (AppSettingsAction) -> Unit
) {
    val context = LocalContext.current

    var advancedHeaderTaps by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsDetailsAppBar(
                title = stringResource(id = R.string.mail_settings_app_customization_title),
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = ProtonDimens.Spacing.Large)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Standard))

                NotificationSettingsItem(
                    notificationStatus = state.settings.notificationsEnabledStatus.string(),
                    onNotificationClick = { launchNotificationSettingsIntent(context) }
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                LanguageSettingsItem(
                    language = state.settings.customLanguage
                        ?: stringResource(R.string.mail_settings_app_language_sys_default),
                    onLanguageClick = actions.onAppLanguageClick
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                AppearanceSettingsItem(
                    appearance = state.settings.theme.string(),
                    onClick = actions.onThemeClick
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                ProtectionSettingsItem(
                    autoLockStatus = if (state.settings.autoLockEnabled) {
                        stringResource(id = R.string.mail_settings_app_customization_protection_enabled_description)
                    } else {
                        stringResource(id = R.string.mail_settings_app_customization_protection_disabled_description)
                    },
                    onClick = actions.onAutoLockClick
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                AppIconSettingsItem(appIconName = state.settings.appIconName, onClick = actions.onAppIconSettingsClick)
                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.ExtraLarge))

                UseCombinedContactsSettingsItem(
                    useCombinedContacts = state.settings.deviceContactsEnabled,
                    onIntent = onIntent
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

                MainSettingsHeader(titleRes = R.string.mail_settings_app_customization_mail_experience_header)

                MailExperienceSettingsItem(
                    swipeToNextEmail = state.settings.swipeNextEnabled,
                    isEmailCategoriesEnabled = state.settings.isEmailCategoriesEnabled,
                    onIntent = onIntent,
                    actions = actions
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Medium))

                MainSettingsHeader(
                    titleRes = R.string.mail_settings_app_customization_advanced_header,
                    modifier = Modifier.clickable(null, null) { advancedHeaderTaps++ }
                )
                AdvancedSettingsItem(
                    alternativeRouting = state.settings.alternativeRoutingEnabled,
                    actions = actions,
                    showApplicationLogsEntry = advancedHeaderTaps >= APP_LOGS_ENTRY_TAP_THRESHOLD,
                    onIntent = onIntent
                )

                Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Jumbo))
            }
        }
    )
}

private fun launchNotificationSettingsIntent(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    context.startActivity(intent, null)
}

private const val APP_LOGS_ENTRY_TAP_THRESHOLD = 5

@Composable
private fun NotificationSettingsItem(
    modifier: Modifier = Modifier,
    notificationStatus: String,
    onNotificationClick: () -> Unit = {}
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column {
            ProtonAppSettingsItemInvert(
                name = stringResource(id = R.string.mail_settings_app_customization_notification),
                hint = notificationStatus,
                onClick = onNotificationClick,
                icon = {
                    ProtonMainSettingsIcon(
                        iconRes = R.drawable.ic_proton_arrow_out_over_square,
                        contentDescription = "",
                        tint = ProtonTheme.colors.iconHint
                    )
                }
            )
        }
    }
}

@Composable
private fun LanguageSettingsItem(
    modifier: Modifier = Modifier,
    language: String,
    onLanguageClick: () -> Unit = {}
) {

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        ProtonAppSettingsItemInvert(
            name = stringResource(id = R.string.mail_settings_app_customization_language),
            hint = language,
            onClick = onLanguageClick,
            icon = {
                ProtonMainSettingsIcon(
                    iconRes = R.drawable.ic_proton_arrow_out_over_square,
                    contentDescription = "",
                    tint = ProtonTheme.colors.iconHint
                )
            }
        )
    }
}

@Composable
private fun AppearanceSettingsItem(
    modifier: Modifier = Modifier,
    appearance: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        ProtonAppSettingsItemInvert(
            name = stringResource(id = R.string.mail_settings_app_customization_appearance),
            hint = appearance,
            onClick = onClick,
            icon = {
                ProtonMainSettingsIcon(
                    iconRes = R.drawable.ic_proton_chevron_up_down,
                    contentDescription = "",
                    tint = ProtonTheme.colors.iconHint
                )
            }
        )
    }
}

@Composable
private fun ProtectionSettingsItem(
    modifier: Modifier = Modifier,
    autoLockStatus: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        ProtonAppSettingsItemInvert(
            name = stringResource(id = R.string.mail_settings_app_customization_protection),
            hint = autoLockStatus,
            onClick = onClick,
            icon = {
                ProtonMainSettingsIcon(
                    iconRes = R.drawable.ic_proton_chevron_right,
                    contentDescription = "",
                    tint = ProtonTheme.colors.iconHint
                )
            }
        )
    }
}

@Composable
@Suppress("UnusedPrivateMember")
private fun AppIconSettingsItem(
    modifier: Modifier = Modifier,
    appIconName: TextUiModel,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        ProtonAppSettingsItemInvert(
            name = stringResource(R.string.mail_settings_app_icon),
            hint = appIconName.string(),
            onClick = onClick,
            icon = {
                ProtonMainSettingsIcon(
                    iconRes = R.drawable.ic_proton_chevron_right,
                    contentDescription = stringResource(id = R.string.mail_settings_app_customization_protection),
                    tint = ProtonTheme.colors.iconHint
                )
            }
        )
    }
}

@Composable
private fun UseCombinedContactsSettingsItem(
    modifier: Modifier = Modifier,
    useCombinedContacts: Boolean,
    onIntent: (AppSettingsAction) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        ProtonSettingsToggleItem(
            modifier = Modifier.padding(ProtonDimens.Spacing.Large),
            name = stringResource(id = R.string.mail_settings_combined_contacts),
            hint = stringResource(id = R.string.mail_settings_combined_contacts_hint),
            value = useCombinedContacts,
            onToggle = { onIntent(ToggleUseCombinedContacts(it)) }
        )
    }
}

@Composable
private fun MailExperienceSettingsItem(
    modifier: Modifier = Modifier,
    swipeToNextEmail: Boolean,
    isEmailCategoriesEnabled: Boolean,
    actions: AppSettingsScreen.Actions,
    onIntent: (AppSettingsAction) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column {
            ProtonSettingsToggleItem(
                modifier = Modifier.padding(ProtonDimens.Spacing.Large),
                name = stringResource(id = R.string.mail_settings_app_customization_swipe_to_next_email),
                hint = stringResource(id = R.string.mail_settings_app_customization_swipe_to_next_email_hint),
                value = swipeToNextEmail,
                onToggle = { onIntent(ToggleSwipeToNextEmail(it)) }
            )

            SettingsItemDivider()

            ProtonAppSettingsItemNorm(
                name = stringResource(id = R.string.mail_settings_app_customization_customize_toolbar),
                onClick = { actions.onCustomizeToolbarClick() },
                icon = {
                    ProtonMainSettingsIcon(
                        iconRes = R.drawable.ic_proton_chevron_right,
                        contentDescription = "",
                        tint = ProtonTheme.colors.iconHint
                    )
                }
            )

            if (isEmailCategoriesEnabled) {
                SettingsItemDivider()

                ProtonAppSettingsItemNorm(
                    name = stringResource(id = R.string.mail_settings_app_customization_email_categories),
                    onClick = { actions.onEmailCategoriesClick() },
                    icon = {
                        ProtonMainSettingsIcon(
                            iconRes = R.drawable.ic_proton_chevron_right,
                            contentDescription = "",
                            tint = ProtonTheme.colors.iconHint
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsItem(
    modifier: Modifier = Modifier,
    alternativeRouting: Boolean,
    actions: AppSettingsScreen.Actions,
    showApplicationLogsEntry: Boolean,
    onIntent: (AppSettingsAction) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column {

            ProtonSettingsToggleItem(
                modifier = Modifier
                    .testTag(TEST_TAG_ALTERNATIVE_ROUTING_TOGGLE_ITEM)
                    .padding(ProtonDimens.Spacing.Large),
                name = stringResource(id = R.string.mail_settings_app_customization_alternative_routing),
                hint = stringResource(id = R.string.mail_settings_app_customization_alternative_routing_hint),
                value = alternativeRouting,
                onToggle = { onIntent(ToggleAlternativeRouting(it)) }
            )


            if (showApplicationLogsEntry) {
                SettingsItemDivider()

                ProtonAppSettingsItemNorm(
                    name = stringResource(id = R.string.mail_settings_app_customization_view_application_logs),
                    onClick = { actions.onViewApplicationLogsClick() },
                    icon = {
                        ProtonMainSettingsIcon(
                            iconRes = R.drawable.ic_proton_chevron_right,
                            contentDescription = stringResource(
                                id = R.string.mail_settings_app_customization_view_application_logs
                            ),
                            tint = ProtonTheme.colors.iconHint
                        )
                    }
                )
            }
        }
    }
}

object AppSettingsScreen {

    data class Actions(
        val onThemeClick: () -> Unit,
        val onPushNotificationsClick: () -> Unit,
        val onAutoLockClick: () -> Unit,
        val onAppIconSettingsClick: () -> Unit,
        val onAppLanguageClick: () -> Unit,
        val onSwipeToNextEmailClick: () -> Unit,
        val onSwipeActionsClick: () -> Unit,
        val onNavigateToSignatureSettings: () -> Unit,
        val onNavigateToUpselling: (UpsellingEntryPoint.Feature, UpsellingVisibility) -> Unit,
        val onCustomizeToolbarClick: () -> Unit,
        val onEmailCategoriesClick: () -> Unit,
        val onViewApplicationLogsClick: () -> Unit,
        val onBackClick: () -> Unit
    )
}

@Preview(
    name = "App customization settings screen light mode",
    showBackground = true
)
@Composable
fun PreviewAppCustomizationScreenLight() {
    ProtonInvertedTheme {
        AppSettingsScreenContent(
            actions = AppSettingsScreenPreviewData.Actions,
            state = AppSettingsScreenPreviewData.Data,
            onIntent = { }
        )
    }
}

@Preview(
    name = "App customization settings screen dark mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewAppCustomizationScreenDark() {
    ProtonInvertedTheme {
        AppSettingsScreenContent(
            actions = AppSettingsScreenPreviewData.Actions,
            state = AppSettingsScreenPreviewData.Data,
            onIntent = { }
        )
    }
}
