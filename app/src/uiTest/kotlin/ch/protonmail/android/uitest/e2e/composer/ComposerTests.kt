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

package ch.protonmail.android.uitest.e2e.composer

import ch.protonmail.android.networkmocks.mockwebserver.MockNetworkDispatcher
import ch.protonmail.android.networkmocks.mockwebserver.requests.get
import ch.protonmail.android.networkmocks.mockwebserver.requests.ignoreQueryParams
import ch.protonmail.android.networkmocks.mockwebserver.requests.matchWildcards
import ch.protonmail.android.networkmocks.mockwebserver.requests.post
import ch.protonmail.android.networkmocks.mockwebserver.requests.put
import ch.protonmail.android.networkmocks.mockwebserver.requests.respondWith
import ch.protonmail.android.networkmocks.mockwebserver.requests.serveOnce
import ch.protonmail.android.networkmocks.mockwebserver.requests.withStatusCode
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.models.mailbox.ParticipantEntry
import ch.protonmail.android.uitest.robot.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.composer.composerRobot
import ch.protonmail.android.uitest.robot.composer.section.messageBodySection
import ch.protonmail.android.uitest.robot.composer.section.recipients.bccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.ccRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.recipients.toRecipientSection
import ch.protonmail.android.uitest.robot.composer.section.subjectSection
import ch.protonmail.android.uitest.robot.helpers.mockRobot
import ch.protonmail.android.uitest.robot.helpers.section.time

internal interface ComposerTests {

    fun composerMockNetworkDispatcher(
        useDefaultMessagesList: Boolean = true,
        useDefaultContacts: Boolean = true,
        useDefaultDraftUploadResponse: Boolean = false,
        useDefaultSendMessageResponse: Boolean = false,
        useDefaultRecipientKeys: Boolean = true,
        mockDefinitions: MockNetworkDispatcher.() -> Unit = {}
    ) = mockNetworkDispatcher(useDefaultContacts = useDefaultContacts) {

        if (useDefaultMessagesList) {
            addMockRequests(
                get("/mail/v4/messages")
                    respondWith "/mail/v4/messages/messages_empty.json"
                    withStatusCode 200 ignoreQueryParams true
            )
        }

        if (useDefaultDraftUploadResponse) {
            addMockRequests(
                post("/mail/v4/messages")
                    respondWith "/mail/v4/messages/post/post_messages_base_create_placeholder.json"
                    withStatusCode 200 matchWildcards true serveOnce true,
                put("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/put/put_messages_base_placeholder.json"
                    withStatusCode 200 matchWildcards true,
            )
        }

        if (useDefaultSendMessageResponse) {
            addMockRequests(
                post("/mail/v4/messages/*")
                    respondWith "/mail/v4/messages/post/post_messages_base_send_placeholder.json"
                    withStatusCode 200 matchWildcards true serveOnce true
            )
        }

        if (useDefaultRecipientKeys) {
            addMockRequests(
                get("/core/v4/keys?Email=royalcat%40proton.black")
                    respondWith "/core/v4/keys/keys_royalcat.json"
                    withStatusCode 200 serveOnce true,
                get("/core/v4/keys?Email=royaldog%40proton.black")
                    respondWith "/core/v4/keys/keys_royaldog.json"
                    withStatusCode 200 serveOnce true,
                get("/core/v4/keys?Email=specialfox%40proton.black")
                    respondWith "/core/v4/keys/keys_specialfox.json"
                    withStatusCode 200 serveOnce true,
                get("/core/v4/keys?Email=sleepykoala%40proton.black")
                    respondWith "/core/v4/keys/keys_sleepykoala.json"
                    withStatusCode 200 serveOnce true,
                get("/core/v4/keys?Email=strangewalrus%40proton.black")
                    respondWith "/core/v4/keys/keys_strangewalrus.json"
                    withStatusCode 200 serveOnce true,
                get("/core/v4/keys?Email=happyllama%40proton.black")
                    respondWith "/core/v4/keys/keys_happyllama.json"
                    withStatusCode 200 serveOnce true,
            )
        }

        mockDefinitions()
    }

    fun ComposerRobot.prepareDraft(
        toRecipients: List<ParticipantEntry> = emptyList(),
        ccRecipients: List<ParticipantEntry> = emptyList(),
        bccRecipients: List<ParticipantEntry> = emptyList(),
        subject: String? = null,
        body: String? = null
    ) {
        mockRobot {
            time { forceCurrentMillisTo(1_688_211_755) } // Jul 1st, 2023
        }

        composerRobot {
            toRecipientSection {
                toRecipients.forEach { typeRecipient(it.value, autoConfirm = true) }
            }

            if (ccRecipients.isNotEmpty() || bccRecipients.isNotEmpty()) {
                toRecipientSection { expandCcAndBccFields() }
            }

            ccRecipientSection {
                ccRecipients.forEach { typeRecipient(it.value, autoConfirm = true) }
            }

            bccRecipientSection {
                bccRecipients.forEach { typeRecipient(it.value, autoConfirm = true) }
            }

            subject?.let {
                subjectSection { typeSubject(it) }
            }

            body?.let {
                messageBodySection { typeMessageBody(body) }
            }
        }
    }
}
