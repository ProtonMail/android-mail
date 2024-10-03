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

package ch.protonmail.android.mailonboarding.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailonboarding.presentation.OnboardingScreenTestTags
import ch.protonmail.android.mailonboarding.presentation.R
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.compose.theme.headlineNorm

@Composable
internal fun OnboardingContent(content: OnboardingUiModel) {
    Column(Modifier.fillMaxHeight()) {
        Image(
            modifier = Modifier
                .testTag(OnboardingScreenTestTags.OnboardingImage)
                .fillMaxHeight(MailDimens.OnboardingIllustrationWeight)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit,
            painter = painterResource(id = content.illustrationId),
            contentDescription = stringResource(id = R.string.onboarding_illustration_content_description)
        )

        Text(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = ProtonDimens.DefaultSpacing)
                .padding(horizontal = ProtonDimens.DefaultSpacing),
            text = stringResource(id = content.headlineId),
            style = ProtonTheme.typography.headlineNorm.copy(textAlign = TextAlign.Center)
        )

        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .padding(ProtonDimens.DefaultSpacing),
                text = stringResource(id = content.descriptionId),
                style = ProtonTheme.typography.defaultWeak.copy(textAlign = TextAlign.Center)
            )
        }
    }
}
