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

package ch.protonmail.android.mailupselling.presentation.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import ch.protonmail.android.mailcommon.presentation.mapper.MarkdownMapper
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingLayoutValues
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme

@Composable
internal fun SocialProofDescription() {
    val descMarkdown = stringResource(R.string.upselling_description_social_proof)
    val desc = remember(Unit) {
        MarkdownMapper.parseBoldToAnnotatedString(
            descMarkdown,
            boldColor = UpsellingLayoutValues.subtitleColor,
            normalColor = UpsellingLayoutValues.SocialProofDescColor
        )
    }

    Text(
        modifier = Modifier
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(top = ProtonDimens.SmallSpacing),
        text = desc,
        style = ProtonTheme.typography.body2Medium,
        color = UpsellingLayoutValues.subtitleColor,
        textAlign = TextAlign.Center
    )

    Text(
        modifier = Modifier
            .padding(horizontal = ProtonDimens.DefaultSpacing)
            .padding(top = ProtonDimens.SmallSpacing),
        text = stringResource(R.string.upselling_description_social_proof_second),
        style = ProtonTheme.typography.body2Regular,
        color = UpsellingLayoutValues.SocialProofDescColor,
        textAlign = TextAlign.Center
    )
}
