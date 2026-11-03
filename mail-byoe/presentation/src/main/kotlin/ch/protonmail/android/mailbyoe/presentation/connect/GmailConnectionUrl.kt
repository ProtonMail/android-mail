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

import ch.protonmail.android.mailsession.domain.model.ForkWithKeyPayload
import java.net.URLEncoder

/**
 * The web page that runs the Gmail sign-in and creates the address. The fork goes in the fragment, so it is
 * never sent to a server; the web app redeems it and uses the payload key to create the address keys.
 * The page returns to [GmailConnectionCallback.REDIRECT_URI] when it is done.
 */
internal fun gmailConnectionUrl(
    apiHost: String,
    appVersion: String,
    fork: ForkWithKeyPayload
): String = "https://account.$apiHost/lite?action=byoe-mobile&app-version=$appVersion" +
    "&redirect=${URLEncoder.encode(GmailConnectionCallback.REDIRECT_URI, Charsets.UTF_8)}" +
    "#selector=${fork.selector.value}&sk=${fork.payloadKey}&pv=${fork.payloadVersion}&pt=${fork.payloadType}"
