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

package ch.protonmail.android.mailcomposer.presentation.usecase

import java.io.IOException
import android.content.Context
import android.content.res.Resources
import ch.protonmail.android.mailcomposer.domain.model.ComposerValues.EDITOR_ID
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.ui.JAVASCRIPT_CALLBACK_INTERFACE_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class GetCustomJs @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        try {
            context.resources.openRawResource(R.raw.rich_text_editor)
                .use { it.readBytes().decodeToString() }
                .replace("\$EDITOR_ID", EDITOR_ID)
                .replace("\$JAVASCRIPT_CALLBACK_INTERFACE_NAME", JAVASCRIPT_CALLBACK_INTERFACE_NAME)
        } catch (notFoundException: Resources.NotFoundException) {
            Timber.e(notFoundException, "Raw js resource is not found")
            ""
        } catch (ioException: IOException) {
            Timber.e(ioException, "Failed to read raw js resource")
            ""
        }
    }
}
