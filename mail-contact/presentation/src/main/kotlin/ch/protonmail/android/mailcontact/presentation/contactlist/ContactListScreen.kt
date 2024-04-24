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

package ch.protonmail.android.mailcontact.presentation.contactlist

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.Avatar
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.mailcontact.presentation.R
import ch.protonmail.android.mailcontact.presentation.model.ContactGroupItemUiModel
import ch.protonmail.android.mailcontact.presentation.model.ContactListItemUiModel
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.contactGroupSampleData
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.contactSampleData
import ch.protonmail.android.mailcontact.presentation.previewdata.ContactListPreviewData.headerSampleData
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactCreate
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactDetails
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactGroupCreate
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactGroupDetails
import ch.protonmail.android.mailcontact.presentation.utils.ContactFeatureFlags.ContactImport
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSecondaryButton
import me.proton.core.compose.component.appbar.ProtonTopAppBar
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionNorm
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallStrongUnspecified
import me.proton.core.compose.theme.defaultSmallWeak
import me.proton.core.compose.theme.defaultStrongNorm
import me.proton.core.contact.domain.entity.ContactId
import me.proton.core.label.domain.entity.LabelId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ContactListScreen(actions: ContactListScreen.Actions, viewModel: ContactListViewModel = hiltViewModel()) {
    val bottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()

    val state = viewModel.state.collectAsStateWithLifecycle().value

    if (state is ContactListState.Loaded) {
        ConsumableLaunchedEffect(effect = state.bottomSheetVisibilityEffect) { bottomSheetEffect ->
            when (bottomSheetEffect) {
                BottomSheetVisibilityEffect.Hide -> scope.launch { bottomSheetState.hide() }
                BottomSheetVisibilityEffect.Show -> scope.launch { bottomSheetState.show() }
            }
        }
    }

    if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) { onDispose { viewModel.submit(ContactListViewAction.OnDismissBottomSheet) } }
    }

    BackHandler(bottomSheetState.isVisible) {
        viewModel.submit(ContactListViewAction.OnDismissBottomSheet)
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            ContactBottomSheetContent(
                actions = ContactSettingsScreen.Actions(
                    onNewContactClick = {
                        viewModel.submit(ContactListViewAction.OnNewContactClick)
                    },
                    onNewContactGroupClick = {
                        viewModel.submit(ContactListViewAction.OnNewContactGroupClick)
                    },
                    onImportContactClick = {
                        viewModel.submit(ContactListViewAction.OnImportContactClick)
                    }
                )
            )
        }
    ) {
        Scaffold(
            topBar = {
                ContactListTopBar(
                    actions = ContactListTopBar.Actions(
                        onBackClick = actions.onBackClick,
                        onAddClick = {
                            viewModel.submit(ContactListViewAction.OnOpenBottomSheet)
                        }
                    ),
                    isAddButtonVisible = state is ContactListState.Loaded.Data
                )
            },
            content = { paddingValues ->
                if (state is ContactListState.Loaded) {
                    ConsumableLaunchedEffect(effect = state.openContactForm) {
                        actions.openContactForm()
                    }
                    ConsumableLaunchedEffect(effect = state.openContactGroupForm) {
                        actions.openContactGroupForm()
                    }
                    ConsumableLaunchedEffect(effect = state.openImportContact) {
                        actions.openImportContact()
                    }
                }

                when (state) {
                    is ContactListState.Loaded.Data -> {
                        ContactTabLayout(
                            modifier = Modifier.padding(paddingValues),
                            scope = scope,
                            actions = actions,
                            state = state
                        )

                        ConsumableTextEffect(effect = state.subscriptionError) { message ->
                            actions.onSubscriptionUpgradeRequired(message)
                        }
                    }

                    is ContactListState.Loaded.Empty -> {
                        EmptyDataScreen(
                            iconResId = R.drawable.ic_proton_users_plus,
                            title = stringResource(R.string.no_contacts),
                            description = stringResource(R.string.no_contacts_description),
                            buttonText = stringResource(R.string.add_contact),
                            showAddButton = ContactCreate.value,
                            onAddClick = { viewModel.submit(ContactListViewAction.OnOpenBottomSheet) }
                        )

                        ConsumableTextEffect(effect = state.subscriptionError) { message ->
                            actions.onSubscriptionUpgradeRequired(message)
                        }
                    }
                    is ContactListState.Loading -> {
                        ProtonCenteredProgress(modifier = Modifier.fillMaxSize())

                        ConsumableTextEffect(effect = state.errorLoading) { message ->
                            actions.exitWithErrorMessage(message)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ContactBottomSheetContent(modifier: Modifier = Modifier, actions: ContactSettingsScreen.Actions) {
    Column(
        modifier = modifier.padding(top = ProtonDimens.SmallSpacing)
    ) {
        if (ContactCreate.value) {
            ContactBottomSheetItem(
                modifier = Modifier,
                titleResId = R.string.new_contact,
                iconResId = R.drawable.ic_proton_user_plus,
                onClick = actions.onNewContactClick
            )
        }
        if (ContactGroupCreate.value) {
            ContactBottomSheetItem(
                modifier = Modifier,
                titleResId = R.string.new_group,
                iconResId = R.drawable.ic_proton_users_plus,
                onClick = actions.onNewContactGroupClick
            )
        }
        if (ContactImport.value) {
            ContactBottomSheetItem(
                modifier = Modifier,
                titleResId = R.string.import_contact,
                iconResId = R.drawable.ic_proton_mobile_plus,
                onClick = actions.onImportContactClick
            )
        }
    }
}

@Composable
fun ContactBottomSheetItem(
    modifier: Modifier = Modifier,
    titleResId: Int,
    iconResId: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(id = titleResId),
                role = Role.Button,
                onClick = onClick
            )
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = NO_CONTENT_DESCRIPTION,
            tint = ProtonTheme.colors.iconNorm
        )
        Text(
            modifier = Modifier.padding(start = ProtonDimens.DefaultSpacing),
            text = stringResource(id = titleResId),
            style = ProtonTheme.typography.defaultNorm
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactTabLayout(
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
                        onAddClick = actions.openContactGroupForm
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactListScreenContent(
    modifier: Modifier = Modifier,
    state: ContactListState.Loaded.Data,
    actions: ContactListScreen.Actions
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(state.contacts) { contactListItemUiModel ->
            when (contactListItemUiModel) {
                is ContactListItemUiModel.Header -> {
                    HeaderListItem(
                        modifier = Modifier.animateItemPlacement(),
                        header = contactListItemUiModel
                    )
                }
                is ContactListItemUiModel.Contact -> {
                    ContactListItem(
                        modifier = Modifier.animateItemPlacement(),
                        contact = contactListItemUiModel,
                        actions = actions
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderListItem(modifier: Modifier = Modifier, header: ContactListItemUiModel.Header) {
    Text(
        text = header.value,
        modifier = modifier.padding(
            start = ProtonDimens.DefaultSpacing,
            end = ProtonDimens.DefaultSpacing,
            top = ProtonDimens.MediumSpacing,
            bottom = ProtonDimens.SmallSpacing
        ),
        style = ProtonTheme.typography.defaultSmallStrongUnspecified,
        color = ProtonTheme.colors.brandNorm
    )
    Divider()
}

@Composable
fun ContactListItem(
    modifier: Modifier = Modifier,
    contact: ContactListItemUiModel.Contact,
    actions: ContactListScreen.Actions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                enabled = ContactDetails.value,
                onClick = {
                    actions.onContactSelected(contact.id)
                }
            )
            .padding(start = ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatarUiModel = contact.avatar,
            onClick = { }
        )
        Column(
            modifier = Modifier.padding(
                start = ProtonDimens.ListItemTextStartPadding,
                top = ProtonDimens.ListItemTextStartPadding,
                bottom = ProtonDimens.ListItemTextStartPadding,
                end = ProtonDimens.DefaultSpacing
            )
        ) {
            Text(
                text = contact.name,
                style = ProtonTheme.typography.defaultNorm
            )
            Text(
                text = contact.emailSubtext.string(),
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactGroupsScreenContent(
    modifier: Modifier = Modifier,
    state: ContactListState.Loaded.Data,
    actions: ContactListScreen.Actions,
    onAddClick: () -> Unit
) {
    if (state.contactGroups.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = ProtonDimens.ListItemTextStartPadding)
        ) {
            items(state.contactGroups) { contactGroupItemUiModel ->
                ContactGroupItem(
                    modifier = Modifier.animateItemPlacement(),
                    contact = contactGroupItemUiModel,
                    actions = actions
                )
            }
        }
    } else {
        EmptyDataScreen(
            iconResId = R.drawable.ic_proton_users_plus,
            title = stringResource(R.string.no_contact_groups),
            description = stringResource(R.string.no_contact_groups_description),
            buttonText = stringResource(R.string.add_contact_group),
            showAddButton = ContactGroupCreate.value,
            onAddClick = onAddClick
        )
    }
}

@Composable
fun ContactGroupItem(
    modifier: Modifier = Modifier,
    contact: ContactGroupItemUiModel,
    actions: ContactListScreen.Actions
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                enabled = ContactGroupDetails.value,
                onClick = {
                    actions.onContactGroupSelected(contact.labelId)
                }
            )
            .padding(start = ProtonDimens.DefaultSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .sizeIn(
                        minWidth = MailDimens.AvatarMinSize,
                        minHeight = MailDimens.AvatarMinSize
                    )
                    .background(
                        color = contact.color,
                        shape = ProtonTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(ProtonDimens.SmallIconSize),
                    painter = painterResource(id = R.drawable.ic_proton_users_filled),
                    tint = Color.White,
                    contentDescription = NO_CONTENT_DESCRIPTION
                )
            }
        }
        Column(
            modifier = Modifier.padding(
                start = ProtonDimens.ListItemTextStartPadding,
                top = ProtonDimens.ListItemTextStartPadding,
                bottom = ProtonDimens.ListItemTextStartPadding,
                end = ProtonDimens.DefaultSpacing
            )
        ) {
            Text(
                text = contact.name,
                style = ProtonTheme.typography.defaultNorm
            )
            Text(
                text = pluralStringResource(
                    R.plurals.contact_group_details_member_count,
                    contact.memberCount,
                    contact.memberCount
                ),
                style = ProtonTheme.typography.defaultSmallWeak
            )
        }
    }
}

@Composable
fun EmptyDataScreen(
    iconResId: Int,
    title: String,
    description: String,
    buttonText: String,
    modifier: Modifier = Modifier,
    showAddButton: Boolean, // Can be deleted once we get rid of ContactFeatureFlags
    onAddClick: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier
                .padding(start = ProtonDimens.ExtraSmallSpacing)
                .background(
                    color = ProtonTheme.colors.backgroundSecondary,
                    shape = RoundedCornerShape(MailDimens.IconWeakRoundBackgroundRadius)
                )
                .padding(ProtonDimens.SmallSpacing),
            painter = painterResource(id = iconResId),
            tint = ProtonTheme.colors.iconNorm,
            contentDescription = NO_CONTENT_DESCRIPTION
        )
        Text(
            title,
            Modifier.padding(
                start = ProtonDimens.LargeSpacing,
                top = ProtonDimens.MediumSpacing,
                end = ProtonDimens.LargeSpacing
            ),
            style = ProtonTheme.typography.defaultStrongNorm
        )
        Text(
            description,
            Modifier.padding(
                start = ProtonDimens.LargeSpacing,
                top = MailDimens.TinySpacing,
                end = ProtonDimens.LargeSpacing
            ),
            style = ProtonTheme.typography.defaultSmallWeak,
            textAlign = TextAlign.Center
        )
        if (showAddButton) {
            ProtonSecondaryButton(
                modifier = Modifier.padding(top = ProtonDimens.LargeSpacing),
                onClick = onAddClick
            ) {
                Text(
                    text = buttonText,
                    Modifier.padding(
                        horizontal = ProtonDimens.SmallSpacing
                    ),
                    style = ProtonTheme.typography.captionNorm
                )
            }
        }
    }
}

@Composable
fun ContactListTopBar(
    modifier: Modifier = Modifier,
    actions: ContactListTopBar.Actions,
    isAddButtonVisible: Boolean
) {
    ProtonTopAppBar(
        modifier = modifier.fillMaxWidth(),
        title = {
            Text(text = stringResource(id = R.string.contact_list_title))
        },
        navigationIcon = {
            IconButton(onClick = actions.onBackClick) {
                Icon(
                    tint = ProtonTheme.colors.iconNorm,
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.presentation_back)
                )
            }
        },
        actions = {
            // hasOneCreateOption can be deleted once we get rid of ContactFeatureFlags
            val hasOneCreateOption = ContactCreate.value || ContactGroupCreate.value || ContactImport.value
            if (isAddButtonVisible && hasOneCreateOption) {
                IconButton(onClick = actions.onAddClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_proton_plus),
                        tint = ProtonTheme.colors.iconNorm,
                        contentDescription = stringResource(R.string.add_contact_content_description)
                    )
                }
            }
        }
    )
}

object ContactListScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onContactSelected: (ContactId) -> Unit,
        val onContactGroupSelected: (LabelId) -> Unit,
        val openContactForm: () -> Unit,
        val openContactGroupForm: () -> Unit,
        val openImportContact: () -> Unit,
        val onSubscriptionUpgradeRequired: (String) -> Unit,
        val exitWithErrorMessage: (String) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onContactSelected = {},
                onContactGroupSelected = {},
                openContactForm = {},
                openContactGroupForm = {},
                openImportContact = {},
                onSubscriptionUpgradeRequired = {},
                exitWithErrorMessage = {}
            )
        }
    }
}

object ContactSettingsScreen {

    data class Actions(
        val onNewContactClick: () -> Unit,
        val onNewContactGroupClick: () -> Unit,
        val onImportContactClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onNewContactClick = {},
                onNewContactGroupClick = {},
                onImportContactClick = {}
            )
        }
    }
}

object ContactListTopBar {

    data class Actions(
        val onBackClick: () -> Unit,
        val onAddClick: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onBackClick = {},
                onAddClick = {}
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactListScreenPreview() {
    ContactListScreenContent(
        state = ContactListState.Loaded.Data(
            contacts = listOf(
                headerSampleData,
                contactSampleData,
                contactSampleData,
                contactSampleData
            ),
            contactGroups = listOf(
                contactGroupSampleData,
                contactGroupSampleData,
                contactGroupSampleData
            )
        ),
        actions = ContactListScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactListScreenPreview() {
    EmptyDataScreen(
        iconResId = R.drawable.ic_proton_users_plus,
        title = stringResource(R.string.no_contacts),
        description = stringResource(R.string.no_contacts_description),
        buttonText = stringResource(R.string.add_contact),
        showAddButton = true,
        onAddClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactGroupsScreenPreview() {
    EmptyDataScreen(
        iconResId = R.drawable.ic_proton_users_plus,
        title = stringResource(R.string.no_contact_groups),
        description = stringResource(R.string.no_contact_groups_description),
        buttonText = stringResource(R.string.add_contact_group),
        showAddButton = true,
        onAddClick = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactBottomSheetScreenPreview() {
    ContactBottomSheetContent(
        actions = ContactSettingsScreen.Actions.Empty
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun ContactListTopBarPreview() {
    ContactListTopBar(
        actions = ContactListTopBar.Actions.Empty,
        isAddButtonVisible = true
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyContactListTopBarPreview() {
    ContactListTopBar(
        actions = ContactListTopBar.Actions.Empty,
        isAddButtonVisible = false
    )
}
