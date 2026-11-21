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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import ch.protonmail.android.R
import timber.log.Timber

/**
 * Opens [link] in the browser, for a link the app is verified to open itself. Returns whether a browser opened it.
 *
 * A plain view intent would come straight back to the app, so the intent is aimed at the browser: the selector
 * picks the app, and the browser then gets the link.
 */
internal fun Context.openInBrowser(link: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, link.toUri()).apply {
        selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Timber.d(e, "Failed to open a browser app")
        Toast.makeText(this, R.string.intent_failure_no_app_found_to_handle_this_action, Toast.LENGTH_LONG).show()
        false
    }
}
