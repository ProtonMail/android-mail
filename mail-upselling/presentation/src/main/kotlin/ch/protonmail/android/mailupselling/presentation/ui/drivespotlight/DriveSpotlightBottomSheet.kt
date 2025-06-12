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

package ch.protonmail.android.mailupselling.presentation.ui.drivespotlight

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentViewEvent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DriveSpotlightBottomSheet(
    copy: TextUiModel,
    onDismiss: () -> Unit,
    onEvent: (DriveSpotlightContentViewEvent) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val orientation = LocalConfiguration.current.orientation
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = {},
        sheetMaxWidth = if (isLandscape) MailDimens.NarrowScreenWidth else BottomSheetDefaults.SheetMaxWidth
    ) {
        val ctx = LocalContext.current
        DriveSpotlightContent(
            copy,
            onDismiss = onDismiss,
            onDisplayed = {
                onEvent(DriveSpotlightContentViewEvent.ContentShown)
            },
            onCTAClicked = {
                onEvent(DriveSpotlightContentViewEvent.OpenDriveClicked)
                ctx.openDriveAppOrStore()
            }
        )
    }
}

private fun Context.openDriveAppOrStore() {
    val launchIntent = packageManager.getLaunchIntentForPackage(DRIVE_PACKAGE_NAME)
    if (launchIntent != null) {
        return startActivity(launchIntent)
    }
    try {
        startActivity(Intent(Intent.ACTION_VIEW, STORE_URI.toUri()))
    } catch (e: ActivityNotFoundException) {
        Timber.i(e)
        startActivity(Intent(Intent.ACTION_VIEW, PLAY_STORE_URL.toUri()))
    }
}

private const val DRIVE_PACKAGE_NAME = "me.proton.android.drive"
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$DRIVE_PACKAGE_NAME"
private const val STORE_URI = "market://details?id=$DRIVE_PACKAGE_NAME"
