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

package ch.protonmail.android.mailsettings.presentation.settings.swipeactions

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailsettings.domain.model.SwipeActionDirection
import ch.protonmail.android.mailsettings.domain.model.SwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.swipeactions.ScreenDimens.ChevronSize
import ch.protonmail.android.mailsettings.presentation.settings.theme.SettingsDimens
import ch.protonmail.android.mailsettings.presentation.settings.theme.SwipeActionIllustrationDimens
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionHint
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultWeak
import me.proton.core.mailsettings.domain.entity.SwipeAction
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

@Composable
fun SwipeActionsPreferenceScreen(
    modifier: Modifier = Modifier,
    actions: SwipeActionsPreferenceScreen.Actions,
    viewModel: SwipeActionsPreferenceViewModel = hiltViewModel()
) {
    val state by rememberAsState(flow = viewModel.state, initial = viewModel.initialState)

    SwipeActionsPreferenceScreen(
        modifier = modifier,
        state = state,
        actions = actions
    )
}

@Composable
fun SwipeActionsPreferenceScreen(
    modifier: Modifier = Modifier,
    state: SwipeActionsPreferenceState,
    actions: SwipeActionsPreferenceScreen.Actions
) {

    Scaffold(
        modifier = modifier,
        topBar = { Toolbar(actions.onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (state) {
                is SwipeActionsPreferenceState.Data -> SwipeActionsPreferenceContent(
                    uiModel = state.model,
                    onChangeSwipeRightClick = actions.onChangeSwipeRightClick,
                    onChangeSwipeLeftClick = actions.onChangeSwipeLeftClick
                )

                SwipeActionsPreferenceState.Loading -> ProtonCenteredProgress()
                SwipeActionsPreferenceState.NotLoggedIn ->
                    ProtonErrorMessage(errorMessage = stringResource(commonString.x_error_not_logged_in))
            }
        }
    }
}

@Composable
private fun SwipeActionsPreferenceContent(
    uiModel: SwipeActionsPreferenceUiModel,
    onChangeSwipeRightClick: () -> Unit,
    onChangeSwipeLeftClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(ProtonDimens.SmallIconSize),
                painter = painterResource(id = R.drawable.ic_proton_info_circle),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = ProtonTheme.colors.iconHint
            )
            Text(
                modifier = Modifier.padding(start = ProtonDimens.SmallSpacing),
                text = stringResource(id = string.mail_settings_swipe_actions_subtitle),
                style = ProtonTheme.typography.captionHint
            )
        }
        SwipeActionItem(
            model = uiModel.right,
            actionDirection = SwipeActionDirection.RIGHT,
            actionNameRes = string.mail_settings_swipe_right_name,
            onChangeClick = onChangeSwipeRightClick
        )
        SwipeActionItem(
            model = uiModel.left,
            actionDirection = SwipeActionDirection.LEFT,
            actionNameRes = string.mail_settings_swipe_left_name,
            onChangeClick = onChangeSwipeLeftClick
        )
    }
}

@Composable
private fun SwipeActionItem(
    model: SwipeActionPreferenceUiModel,
    actionDirection: SwipeActionDirection,
    @StringRes actionNameRes: Int,
    onChangeClick: () -> Unit
) {
    val actionName = stringResource(id = actionNameRes)
    val actionDescription = stringResource(id = model.descriptionRes)

    Column(
        modifier = Modifier
            .clickable(onClick = onChangeClick)
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth()
                .background(ProtonTheme.colors.separatorNorm)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ProtonDimens.DefaultSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = actionName, style = ProtonTheme.typography.defaultNorm)
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionDescription,
                    style = ProtonTheme.typography.defaultWeak
                )

                Icon(
                    modifier = Modifier.size(ChevronSize),
                    painter = painterResource(id = R.drawable.ic_proton_chevron_right),
                    tint = ProtonTheme.colors.iconWeak,
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }
        }
        SwipeActionPreviewItem(model = model, actionDirection)
    }
}

@Composable
private fun SwipeActionPreviewItem(model: SwipeActionPreferenceUiModel, actionDirection: SwipeActionDirection) {
    val offset = when (actionDirection) {
        SwipeActionDirection.RIGHT -> ProtonDimens.MediumSpacing
        SwipeActionDirection.LEFT -> -ProtonDimens.MediumSpacing
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProtonTheme.colors.backgroundSecondary)
            .padding(vertical = ProtonDimens.MediumSpacing)
            .offset(x = offset)
    ) {
        if (actionDirection == SwipeActionDirection.RIGHT) {
            SwipeActionIcon(model = model, actionDirection = actionDirection)
        }
        ContentIllustration(modifier = Modifier.weight(1f))
        if (actionDirection == SwipeActionDirection.LEFT) {
            SwipeActionIcon(model = model, actionDirection = actionDirection)
        }
    }
}

@Composable
private fun SwipeActionIcon(model: SwipeActionPreferenceUiModel, actionDirection: SwipeActionDirection) {
    val noCorner = CornerSize(0)
    val shape = ProtonTheme.shapes.medium.let {
        when (actionDirection) {
            SwipeActionDirection.RIGHT -> it.copy(topEnd = noCorner, bottomEnd = noCorner)
            SwipeActionDirection.LEFT -> it.copy(topStart = noCorner, bottomStart = noCorner)
        }
    }

    Column(
        modifier = Modifier
            .size(SettingsDimens.SwipeActionIconSize)
            .background(model.getColor(), shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = model.imageRes),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconInverted
        )
        Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
        Text(text = stringResource(id = model.titleRes), color = ProtonTheme.colors.textInverted)
    }
}

@Composable
private fun ContentIllustration(modifier: Modifier) {
    val shape = ProtonTheme.shapes.small
    val color = ProtonTheme.colors.backgroundSecondary
    with(SwipeActionIllustrationDimens) {
        Row(
            modifier = modifier
                .height(SettingsDimens.SwipeActionIconSize)
                .background(ProtonTheme.colors.backgroundNorm)
        ) {
            Box(
                modifier = Modifier
                    .padding(start = SquareStartSpacing, top = SquareTopSpacing)
                    .size(SquareSize)
                    .background(color, shape)
            )
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(start = LineStartSpacing, top = LineTopSpacing, end = LineEndSpacing)
                        .fillMaxWidth()
                        .height(LineHeight)
                        .background(color, shape)
                )
                Box(
                    modifier = Modifier
                        .padding(start = LineStartSpacing, top = LineSpacing, end = LineEndSpacing)
                        .fillMaxWidth()
                        .height(LineHeight)
                        .background(color, shape)
                )
                Box(
                    modifier = Modifier
                        .padding(start = LineStartSpacing, top = LineSpacing, end = LineEndSpacing)
                        .width(ShortLineWidth)
                        .height(LineHeight)
                        .background(color, shape)
                )
            }
        }
    }
}

@Composable
private fun Toolbar(onBack: () -> Unit) {
    ProtonTopAppBar(
        title = { Text(text = stringResource(id = string.mail_settings_swipe_actions_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = stringResource(id = string.mail_settings_toolbar_button_content_description)
                )
            }
        }
    )
}

object SwipeActionsPreferenceScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onChangeSwipeRightClick: () -> Unit,
        val onChangeSwipeLeftClick: () -> Unit
    )
}

@Composable
@Preview(showBackground = true)
fun SwipeActionsPreferenceContentPreview() {
    val actions = SwipeActionsPreference(
        swipeLeft = SwipeAction.Spam,
        swipeRight = SwipeAction.MarkRead
    )
    val uiModel = SwipeActionPreferenceUiModelMapper().toUiModel(actions)
    ProtonTheme {
        SwipeActionsPreferenceContent(
            uiModel = uiModel,
            onChangeSwipeRightClick = {},
            onChangeSwipeLeftClick = {}
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SwipeActionsPreferenceScreenPreview() {
    val actions = SwipeActionsPreference(
        swipeLeft = SwipeAction.Trash,
        swipeRight = SwipeAction.Star
    )
    val uiModel = SwipeActionPreferenceUiModelMapper().toUiModel(actions)
    val state = SwipeActionsPreferenceState.Data(uiModel)
    ProtonTheme {
        SwipeActionsPreferenceScreen(
            state = state,
            actions = SwipeActionsPreferenceScreen.Actions(
                onBackClick = {},
                onChangeSwipeRightClick = {},
                onChangeSwipeLeftClick = {}
            )
        )
    }
}

private object ScreenDimens {
    val ChevronSize = 20.dp
}
