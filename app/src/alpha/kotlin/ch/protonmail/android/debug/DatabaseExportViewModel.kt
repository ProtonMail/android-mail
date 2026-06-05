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

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.presentation.Effect
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DatabaseExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val mutableState = MutableStateFlow(DatabaseExportState())
    val state: StateFlow<DatabaseExportState> = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val databases = runCatching { listDatabases() }
                .onFailure { Timber.tag(TAG).e(it, "Unable to list databases") }
                .getOrDefault(emptyList())

            mutableState.update { it.copy(databases = databases, isLoading = false) }
        }
    }

    /** Builds the zip and shares it through the share sheet (ACTION_SEND). */
    fun exportDatabase(dbName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                runCatching {
                    val zipFile = buildZip(dbName)
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
                }.fold(
                    onSuccess = { uri -> mutableState.update { it.copy(shareEffect = Effect.of(uri)) } },
                    onFailure = { error ->
                        Timber.tag(TAG).e(error, "Failed to export database $dbName")
                        mutableState.update { it.copy(errorEffect = Effect.of(Unit)) }
                    }
                )
            }
        }
    }

    /** Builds the zip and writes it to the user-picked [destination] (save-to-disk). */
    fun saveDatabaseToUri(dbName: String, destination: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(NonCancellable) {
                runCatching {
                    val zipFile = buildZip(dbName)
                    context.contentResolver.openOutputStream(destination)?.use { output ->
                        zipFile.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Unable to open output stream for $destination")
                }.fold(
                    onSuccess = { mutableState.update { it.copy(savedEffect = Effect.of(Unit)) } },
                    onFailure = { error ->
                        Timber.tag(TAG).e(error, "Failed to save database $dbName")
                        mutableState.update { it.copy(errorEffect = Effect.of(Unit)) }
                    }
                )
            }
        }
    }

    /**
     * Zips the main database file together with its WAL/SHM/journal sidecars so the exported
     * snapshot can be opened consistently by any SQLite client (the WAL is replayed on open).
     * The database is only read, never opened for writing, so the live Rust session is untouched.
     */
    private fun buildZip(dbName: String): File {
        val databasesDir = databasesDirectory()

        val relatedFiles = databasesDir
            .listFiles { file -> file.name == dbName || file.name.startsWith("$dbName-") }
            .orEmpty()
            .filter { it.isFile }
            .sortedBy { it.name }

        check(relatedFiles.isNotEmpty()) { "No files found for database $dbName" }

        val exportDir = File(context.cacheDir, DB_EXPORT_DIR).apply { mkdirs() }
        val zipFile = File(exportDir, "${dbName.removeSuffix(DB_EXTENSION_SUFFIX)}.zip")

        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            relatedFiles.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        return zipFile
    }

    private fun listDatabases(): List<DatabaseInfo> = databasesDirectory()
        .listFiles { file -> file.isFile && file.name.endsWith(DB_EXTENSION_SUFFIX) }
        .orEmpty()
        .map { file -> DatabaseInfo(name = file.name, sizeBytes = file.length()) }
        .sortedBy { it.name }

    private fun databasesDirectory(): File = context.filesDir.parent
        ?.let { File(it, DATABASES_DIR) }
        ?: File(context.filesDir, DATABASES_DIR)

    companion object {

        private const val TAG = "mail-debug-db-export"
        private const val DATABASES_DIR = "databases"
        private const val DB_EXPORT_DIR = "db_export"
        private const val DB_EXTENSION_SUFFIX = ".db"
    }
}
