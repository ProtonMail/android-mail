/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.signature

import android.widget.Toast
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonAppSettingsItemInvert
import ch.protonmail.android.design.compose.component.ProtonCenteredProgress
import ch.protonmail.android.design.compose.component.ProtonMainSettingsIcon
import ch.protonmail.android.design.compose.component.ProtonSettingsDetailsAppBar
import ch.protonmail.android.design.compose.component.ProtonSettingsHintNavigationItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonInvertedTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailsettings.domain.model.MobileSignatureStatus
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.signature.model.MobileSignatureMenuState
import ch.protonmail.android.mailsettings.presentation.settings.signature.model.MobileSignatureUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility

@Composable
fun SignatureSettingsMenuScreen(modifier: Modifier = Modifier, actions: SignatureSettingsMenuScreen.Actions) {
    SignatureSettingsMenuScreenContent(modifier, actions)
}

@Composable
private fun SignatureSettingsMenuScreenContent(
    modifier: Modifier = Modifier,
    actions: SignatureSettingsMenuScreen.Actions,
    viewModel: SignatureSettingsMenuViewModel = hiltViewModel()
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsDetailsAppBar(
                title = stringResource(id = R.string.mail_settings_app_customization_signature_header),
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            when (val state = viewModel.state.collectAsStateWithLifecycle().value) {
                is MobileSignatureMenuState.Data -> {
                    SignatureSettingsMenuScreenContent(
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(horizontal = ProtonDimens.Spacing.Large),
                        actions = actions,
                        mobileSignature = state.settings,
                        upsellingVisibility = state.upsellingVisibility
                    )
                }

                is MobileSignatureMenuState.Loading -> ProtonCenteredProgress(modifier = Modifier.fillMaxSize())
            }
        }
    )

}

@Composable
private fun SignatureSettingsMenuScreenContent(
    modifier: Modifier = Modifier,
    mobileSignature: MobileSignatureUiModel,
    upsellingVisibility: UpsellingVisibility,
    actions: SignatureSettingsMenuScreen.Actions
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ProtonTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(),
            colors = CardDefaults.cardColors().copy(
                containerColor = ProtonTheme.colors.backgroundInvertedSecondary
            )
        ) {
            ProtonSettingsHintNavigationItem(
                name = stringResource(id = R.string.mail_settings_app_customization_email_signature_header),
                onClick = actions.onNavigateToEmailSignatureSettings,
                hint = stringResource(R.string.mail_settings_app_customization_email_signature_explanation)
            )
        }

        Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Large))

        if (mobileSignature.signatureStatus == MobileSignatureStatus.NeedsPaidVersion) {
            MobileSignatureSettingsItemForFreePlan(
                mobileSignature = mobileSignature,
                upsellingVisibility = upsellingVisibility,
                onNavigateToUpselling = actions.onNavigateToUpselling
            )
        } else {
            MobileSignatureSettingsItemForPaidPlan(
                mobileSignature = mobileSignature,
                onNavigateToMobileSignatureSettings = actions.onNavigateToMobileSignatureSettings
            )
        }
    }
}

@Composable
private fun MobileSignatureSettingsItemForFreePlan(
    modifier: Modifier = Modifier,
    mobileSignature: MobileSignatureUiModel,
    upsellingVisibility: UpsellingVisibility,
    onNavigateToUpselling: (UpsellingEntryPoint.Feature, UpsellingVisibility) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ProtonTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {

        val context = LocalContext.current
        val fallbackText =
            stringResource(R.string.upselling_upgrade_plan_generic)
        val hint = stringResource(R.string.mail_settings_app_customization_mobile_signature_explanation)
        val onNavigateToUpsell: () -> Unit = {
            when (upsellingVisibility) {
                is UpsellingVisibility.Hidden -> {
                    Toast.makeText(context, fallbackText, Toast.LENGTH_SHORT).show()
                }

                is UpsellingVisibility.Promotional,
                is UpsellingVisibility.Normal -> onNavigateToUpselling(
                    UpsellingEntryPoint.Feature.MobileSignature,
                    upsellingVisibility
                )
            }
        }
        val name = stringResource(id = R.string.mail_settings_app_customization_mobile_signature_header)
        ProtonAppSettingsItemInvert(
            modifier = Modifier.semantics {
                contentDescription = "$name $hint $fallbackText"
            },
            name = name,
            hint = mobileSignature.statusText.string(),
            onClick = onNavigateToUpsell,
            icon = {
                UpsellIcon()
            },
            iconContainerSize = DpSize(
                ProtonDimens.IconSize.ExtraLarge, ProtonDimens.IconSize.MediumLarge
            ),
            content = {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.Compact),
                    text = hint,
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.bodyMediumNorm
                )
            }
        )
    }
}

@Composable
private fun MobileSignatureSettingsItemForPaidPlan(
    modifier: Modifier = Modifier,
    mobileSignature: MobileSignatureUiModel,
    onNavigateToMobileSignatureSettings: () -> Unit
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
            name = stringResource(id = R.string.mail_settings_app_customization_mobile_signature_header),
            hint = mobileSignature.statusText.string(),
            onClick = onNavigateToMobileSignatureSettings,
            icon = {
                ProtonMainSettingsIcon(
                    iconRes = R.drawable.ic_proton_chevron_right,
                    contentDescription = "",
                    tint = ProtonTheme.colors.iconHint
                )
            },
            content = {
                Text(
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.Compact),
                    text = stringResource(R.string.mail_settings_app_customization_mobile_signature_explanation),
                    color = ProtonTheme.colors.textWeak,
                    style = ProtonTheme.typography.bodyMediumNorm
                )
            },
            iconContainerSize = DpSize(
                ProtonDimens.IconSize.Default, ProtonDimens.IconSize.Default
            )
        )
    }
}

@Composable
private fun UpsellIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_mail_upsell),
            contentDescription = null,
            tint = Color.Unspecified
        )
    }
}

object SignatureSettingsMenuScreen {
    data class Actions(
        val onBackClick: () -> Unit,
        val onNavigateToMobileSignatureSettings: () -> Unit,
        val onNavigateToEmailSignatureSettings: () -> Unit,
        val onNavigateToUpselling: (UpsellingEntryPoint.Feature, UpsellingVisibility) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onNavigateToMobileSignatureSettings = {},
                onNavigateToEmailSignatureSettings = {},
                onNavigateToUpselling = { _, _ -> }
            )
        }
    }
}

@Preview(name = "Signature Settings Mobile– Enabled", showBackground = true)
@Composable
private fun PreviewMobileSignatureEnabled() {
    ProtonInvertedTheme {
        SignatureSettingsMenuScreenContent(
            mobileSignature = MobileSignatureUiModel(
                MobileSignatureStatus.Enabled,
                "This is a mobile signature",
                TextUiModel.TextRes(R.string.mail_settings_app_customization_mobile_signature_on)
            ),
            upsellingVisibility = UpsellingVisibility.Hidden,
            actions = SignatureSettingsMenuScreen.Actions.Empty
        )
    }
}

@Preview(name = "Signature Settings Mobile– Disabled", showBackground = true)
@Composable
private fun PreviewMobileSignatureDisabled() {
    ProtonInvertedTheme {
        SignatureSettingsMenuScreenContent(
            mobileSignature = MobileSignatureUiModel(
                MobileSignatureStatus.Disabled,
                "This is a mobile signature",
                TextUiModel.TextRes(R.string.mail_settings_app_customization_mobile_signature_off)
            ),
            upsellingVisibility = UpsellingVisibility.Hidden,
            actions = SignatureSettingsMenuScreen.Actions.Empty
        )
    }
}

@Preview(name = "Signature Settings Mobile– Upselling", showBackground = true)
@Composable
private fun PreviewMobileSignatureUpselling() {
    ProtonInvertedTheme {
        SignatureSettingsMenuScreenContent(
            mobileSignature = MobileSignatureUiModel(
                MobileSignatureStatus.Disabled,
                "This is a mobile signature",
                TextUiModel.TextRes(R.string.mail_settings_app_customization_mobile_signature_off)
            ),
            upsellingVisibility = UpsellingVisibility.Normal.MailPlus,
            actions = SignatureSettingsMenuScreen.Actions.Empty
        )
    }
}
