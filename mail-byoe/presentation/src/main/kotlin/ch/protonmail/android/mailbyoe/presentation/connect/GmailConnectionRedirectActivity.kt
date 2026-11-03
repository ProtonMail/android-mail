/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailbyoe.presentation.connect

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Receives the `protonmail://connect-gmail/complete` redirect from browsers without Auth Tab support,
 * where the Gmail connection page runs in a plain Custom Tab.
 *
 * It keeps the redirect for [GmailConnectionScreen] and brings the app's task back to the front, which
 * closes the Custom Tab on top of it. The screen then gets the tab's cancelled result and reads the
 * redirect from [GmailConnectionRedirects].
 */
class GmailConnectionRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val uri = intent?.data
        Timber.d("byoe: Gmail connection redirect received, host = ${uri?.host}, path = ${uri?.path}")
        if (uri != null) GmailConnectionRedirects.deliver(uri)

        packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?.let { startActivity(it) }
        finish()
    }
}

/**
 * Hands the Custom Tab redirect from [GmailConnectionRedirectActivity] to [GmailConnectionScreen].
 *
 * Any app can fire the deep link, so the screen clears it before opening the page and only reads it
 * while a connection is in progress.
 */
internal object GmailConnectionRedirects {

    private val pending = AtomicReference<Uri?>(null)

    fun deliver(uri: Uri) {
        pending.set(uri)
    }

    fun consume(): Uri? = pending.getAndSet(null)
}
