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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailonboarding.presentation.OnboardingScreenTestTags
import ch.protonmail.android.mailonboarding.presentation.R
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSolidButton
import me.proton.core.compose.theme.ProtonDimens

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun OnboardingButton(
    onCloseOnboarding: () -> Unit,
    pagerState: PagerState,
    viewCount: Int
) {
    val scope = rememberCoroutineScope()

    ProtonSolidButton(
        modifier = Modifier
            .testTag(OnboardingScreenTestTags.BottomButton)
            .padding(ProtonDimens.DefaultSpacing)
            .height(MailDimens.onboardingBottomButtonHeight)
            .fillMaxWidth()
            .horizontalScroll(state = ScrollState(0), enabled = true),
        onClick = {
            val nextPageIndex = pagerState.currentPage.plus(1)
            if (nextPageIndex == viewCount) {
                onCloseOnboarding()
            } else {
                scope.launch {
                    pagerState.animateScrollToPage(nextPageIndex)
                }
            }
        }
    ) {
        val positiveButtonTextId =
            if (pagerState.currentPage == viewCount.minus(1)) R.string.onboarding_get_started
            else R.string.onboarding_next
        Text(text = stringResource(id = positiveButtonTextId))
    }
}
