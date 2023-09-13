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

package ch.protonmail.android.mailmessage.domain.sample

import ch.protonmail.android.mailmessage.domain.model.Recipient

object RecipientSample {

    val Doe = build(
        address = "doe@pm.me",
        name = "Doe"
    )

    val John = build(
        address = "john@pm.me",
        name = "John"
    )

    val Alice = build(
        address = "alice@pm.me",
        name = "Alice"
    )

    val Bob = build(
        address = "bob@pm.me",
        name = "Bob"
    )

    val Billing = build(
        address = "billing@company.com",
        name = "Billing Service"
    )

    val PreciWeather = build(
        address = "info@preciweather.com",
        name = "PreciWeather"
    )

    val Scammer = build(
        address = "definitely-not-a-scam@totally-legit.com",
        name = "definitely-not-a-scam@totally-legit.com"
    )

    val ExternalEncrypted = build(
        address = "external-encrypted@privacy.first.com",
        name = "external-encrypted@privacy.first.com"
    )

    fun build(address: String = "email@pm.me", name: String = "name") = Recipient(
        address = address,
        name = name
    )
}
