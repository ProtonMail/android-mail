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

import ezvcard.Ezvcard
import ezvcard.VCard
import ezvcard.property.Gender
import me.proton.core.contact.domain.decryptContactCard
import me.proton.core.contact.domain.entity.ContactCard
import me.proton.core.contact.domain.entity.DecryptedVCard
import me.proton.core.crypto.common.pgp.Signature
import me.proton.core.crypto.common.pgp.VerificationStatus
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.encryptText
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.signText
import me.proton.core.key.domain.verifyText

fun KeyHolderContext.decryptContactCardTrailingSpacesFallback(contactCard: ContactCard): DecryptedVCard {
    return when (contactCard) {
        is ContactCard.ClearText -> decryptContactCard(contactCard)
        is ContactCard.Encrypted -> decryptContactCardEncryptedTrailingSpacesFallback(contactCard)
        is ContactCard.Signed -> decryptContactCardSignedTrailingSpacesFallback(contactCard)
    }
}

@Suppress("FunctionMaxLength")
private fun KeyHolderContext.decryptContactCardSignedTrailingSpacesFallback(
    contactCard: ContactCard.Signed
): DecryptedVCard {
    val verified = verifyTextWithTrailingSpacesFallback(contactCard.data, contactCard.signature)
    return DecryptedVCard(
        card = Ezvcard.parse(contactCard.data).first(),
        status = VerificationStatus.Success.takeIf { verified } ?: VerificationStatus.Failure
    )
}

@Suppress("FunctionMaxLength")
private fun KeyHolderContext.decryptContactCardEncryptedTrailingSpacesFallback(
    contactCard: ContactCard.Encrypted
): DecryptedVCard {
    val decryptedText = decryptText(contactCard.data)
    val signature = contactCard.signature
    val status = when {
        signature == null -> VerificationStatus.NotSigned
        verifyTextWithTrailingSpacesFallback(decryptedText, signature) -> VerificationStatus.Success
        else -> VerificationStatus.Failure
    }

    val parsedVCard = Ezvcard.parse(decryptedText).first()

    return DecryptedVCard(
        card = parsedVCard.also {
            // hack for MAILANDR-1819
            val extractedGenderValue = decryptedText.extractProperty("GENDER")
            if (extractedGenderValue != null) {
                it.gender = Gender(extractedGenderValue)
            }
        },
        status = status
    )
}

/**
 * This fallback should be ported to core. Other functions in this file have been copied from core
 * and signature verification replaced with this function that does the fallback.
 */
private fun KeyHolderContext.verifyTextWithTrailingSpacesFallback(data: String, signature: Signature): Boolean {
    return if (!verifyText(data, signature, trimTrailingSpaces = true)) {
        verifyText(data, signature, trimTrailingSpaces = false)
    } else true
}

fun KeyHolderContext.encryptAndSignNoTrailingSpacesTrim(vCard: VCard): ContactCard.Encrypted {
    val vCardData = vCard.write()
    val encryptedVCardData = encryptText(vCardData)
    val vCardSignature = signText(vCardData, trimTrailingSpaces = false)
    return ContactCard.Encrypted(encryptedVCardData, vCardSignature)
}

fun KeyHolderContext.signNoTrailingSpacesTrim(vCard: VCard): ContactCard.Signed {
    val vCardData = vCard.write()
    val vCardSignature = signText(vCardData, trimTrailingSpaces = false)
    return ContactCard.Signed(vCardData, vCardSignature)
}
