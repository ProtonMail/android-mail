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

package ch.protonmail.android.mailcontact.domain

import ezvcard.VCard
import ezvcard.VCardVersion
import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Categories
import ezvcard.property.Email
import ezvcard.property.FormattedName
import ezvcard.property.Gender
import ezvcard.property.Key
import ezvcard.property.Language
import ezvcard.property.Logo
import ezvcard.property.Member
import ezvcard.property.Nickname
import ezvcard.property.Note
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.ProductId
import ezvcard.property.Related
import ezvcard.property.Role
import ezvcard.property.StructuredName
import ezvcard.property.Telephone
import ezvcard.property.Timezone
import ezvcard.property.Title
import ezvcard.property.Uid
import ezvcard.property.Url
import me.proton.core.util.kotlin.takeIfNotBlank

/**
 * Creates an empty VCard and copies into it only the properties supported by Proton Contacts.
 */
fun VCard.sanitizeAndBuildVCard(): VCard {

    val vCard = VCard(VCardVersion.V4_0).apply {
        productId = ProductId(VCARD_PROD_ID)
    }

    this.properties.forEach { vCardProperty ->
        // we ignore (Version, Prod ID) because we set our own values of them
        // but we copy all the supported properties if they were present in the original VCard
        when (vCardProperty) {
            is Uid,
            is FormattedName,
            is Email,
            is Categories,
            is StructuredName, // in VCARD it's a property called "N"
            is Telephone,
            is Address,
            is Birthday,
            is Note,
            is Photo,
            is Organization,
            is Title,
            is Role,
            is Timezone,
            is Logo,
            is Member,
            is Language,
            is Url,
            is Gender,
            is Anniversary,
            is Nickname,
            is Key,
            is Related -> vCard.addProperty(vCardProperty)
        }

    }

    this.extendedProperties.forEach {
        if ((it.propertyName?.takeIfNotBlank() ?: "").uppercase() in protonSupportedVCardExtendedProperties) {
            vCard.extendedProperties.add(it)
        }
    }

    // if UID is still blank after copying, set a random one
    if (vCard.uid?.value?.takeIfNotBlank() == null) {
        vCard.uid = Uid.random()
    }

    return vCard
}

const val VCARD_PROD_ID = "-//ProtonMail//ProtonMail for Android vCard 1.0.0//EN"

val protonSupportedVCardExtendedProperties = listOf(
    "X-PM-MIMETYPE",
    "X-PM-ENCRYPT",
    "X-PM-SIGN",
    "X-PM-SCHEME",
    "X-PM-TLS",
    "X-PM-ENCRYPT-UNTRUSTED"
)
