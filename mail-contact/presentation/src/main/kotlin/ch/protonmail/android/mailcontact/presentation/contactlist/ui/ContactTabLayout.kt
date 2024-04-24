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

package ch.protonmail.android.mailcontact.presentation.contactlist.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.contactlist.ContactListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallStrongUnspecified

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactTabLayout(
    modifier: Modifier = Modifier,
    scope: CoroutineScope,
    actions: ContactListScreen.Actions,
    state: ContactListState.Loaded.Data
) {
    val pages = listOf(
        stringResource(R.string.all_contacts_tab),
        stringResource(R.string.contact_groups_tab)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column {
        TabRow(
            backgroundColor = ProtonTheme.colors.backgroundNorm,
            modifier = modifier,
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    color = ProtonTheme.colors.brandNorm,
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                )
            }
        ) {
            pages.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    text = {
                        Text(
                            text = title,
                            style = ProtonTheme.typography.defaultSmallStrongUnspecified
                        )
                    },
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState
        ) { index ->
            when (index) {
                0 -> {
                    ContactListScreenContent(
                        state = state,
                        actions = actions
                    )
                }

                1 -> {
                    ContactGroupsScreenContent(
                        state = state,
                        actions = actions,
                        onNewGroupClick = actions.onNewGroupClick
                    )
                }
            }
        }
    }
}
