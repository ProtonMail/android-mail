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

package ch.protonmail.android.navigation

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject

class AddAttachmentsOrchestrator @Inject constructor() {

    private var launcher: ActivityResultLauncher<String>? = null
    private var onActivityResult: (List<Uri>) -> Unit = {}

    fun register(caller: AppCompatActivity) {
        launcher = caller.registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            onActivityResult(it)
        }
    }

    fun openFilePicker(onActivityResult: (List<Uri>) -> Unit) {
        this.onActivityResult = onActivityResult
        launcher?.launch(MIME_TYPE)
    }

    companion object {
        private const val MIME_TYPE = "*/*"
    }
}
