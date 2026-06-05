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

package ch.protonmail.android.debug

import java.util.Locale
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonOutlinedButton
import ch.protonmail.android.design.compose.component.ProtonSolidButton
import ch.protonmail.android.design.compose.component.appbar.ProtonMediumTopAppBar
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyLargeNorm
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import me.proton.core.presentation.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseExportScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: DatabaseExportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val errorMessage = stringResource(R.string.app_debug_screen_database_export_error)
    val savedMessage = stringResource(R.string.app_debug_screen_database_export_saved)

    var pendingSaveDb by remember { mutableStateOf<String?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val dbName = pendingSaveDb
        pendingSaveDb = null
        if (uri != null && dbName != null) viewModel.saveDatabaseToUri(dbName, uri)
    }

    ConsumableLaunchedEffect(state.shareEffect) { uri ->
        context.shareDatabaseFile(uri)
    }

    ConsumableLaunchedEffect(state.savedEffect) {
        context.showToast(savedMessage)
    }

    ConsumableLaunchedEffect(state.errorEffect) {
        context.showToast(errorMessage)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonMediumTopAppBar(
                title = { Text(text = stringResource(R.string.app_debug_screen_database_export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.presentation_back)
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            DatabaseExportContent(
                modifier = Modifier.padding(paddingValues),
                state = state,
                onExport = viewModel::exportDatabase,
                onSave = { dbName ->
                    pendingSaveDb = dbName
                    saveLauncher.launch("${dbName.removeSuffix(".db")}.zip")
                }
            )
        }
    )
}

@Composable
private fun DatabaseExportContent(
    modifier: Modifier = Modifier,
    state: DatabaseExportState,
    onExport: (String) -> Unit,
    onSave: (String) -> Unit
) {
    when {
        state.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.databases.isEmpty() -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(ProtonDimens.Spacing.Large),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(R.string.app_debug_screen_database_export_empty))
        }

        else -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(ProtonDimens.Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)
        ) {
            items(state.databases, key = { it.name }) { database ->
                DatabaseCard(
                    database = database,
                    onExport = { onExport(database.name) },
                    onSave = { onSave(database.name) }
                )
            }
        }
    }
}

@Composable
private fun DatabaseCard(
    database: DatabaseInfo,
    onExport: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ProtonTheme.shapes.large)
            .background(ProtonTheme.colors.backgroundSecondary)
            .padding(ProtonDimens.Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)
    ) {
        Text(
            text = database.name,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            color = ProtonTheme.colors.textNorm,
            style = ProtonTheme.typography.bodyLargeNorm
        )
        Text(
            text = database.formattedSize(),
            color = ProtonTheme.colors.textWeak,
            style = ProtonTheme.typography.bodyMediumNorm
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Standard)) {
            ProtonSolidButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.app_debug_screen_database_export_action_export))
            }
            ProtonOutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.app_debug_screen_database_export_action_save))
            }
        }
    }
}

private fun DatabaseInfo.formattedSize(): String {
    val kb = sizeBytes / 1024.0
    return if (kb < 1024) {
        String.format(Locale.US, "%.1f KB", kb)
    } else {
        String.format(Locale.US, "%.1f MB", kb / 1024.0)
    }
}

private fun Context.shareDatabaseFile(uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        clipData = ClipData.newRawUri("", uri)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(shareIntent, getString(R.string.app_debug_screen_database_export_title)))
}
