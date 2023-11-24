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

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailsettings.domain.model.Signature
import ch.protonmail.android.mailsettings.domain.model.SignatureValue
import ch.protonmail.android.mailsettings.domain.usecase.identity.GetAddressSignature
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.toPlainText
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase.GetMobileFooter
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class InjectAddressSignature @Inject constructor(
    private val getAddressSignature: GetAddressSignature,
    private val getMobileFooter: GetMobileFooter
) {

    suspend operator fun invoke(
        userId: UserId,
        draftBody: DraftBody,
        senderEmail: SenderEmail,
        previousSenderEmail: SenderEmail? = null
    ): Either<DataError, DraftBody> = either {

        val addressSignature = getAddressSignature(userId, senderEmail.value).getOrElse {
            Timber.e("InjectAddressSignature: error getting address signature: $it")
            Signature(enabled = false, SignatureValue(""))
        }

        val mobileFooter = getMobileFooter(userId).bind()

        previousSenderEmail?.let { senderEmail ->
            getAddressSignature(userId, senderEmail.value).fold(
                ifLeft = { Timber.e("Error getting previous address signature: $senderEmail") },
                ifRight = { previousAddressSignature ->
                    getBodyWithReplacedSignature(
                        draftBody,
                        previousAddressSignature.value,
                        mobileFooter.value,
                        addressSignature
                    ).let {
                        return it.right()
                    }
                }
            )
        }

        val draftBodyWithAddressSignature = StringBuilder().apply {
            append(draftBody.value)

            if (addressSignature.enabled && addressSignature.value.toPlainText().isNotBlank()) {
                append(SignatureFooterSeparator)
                append(addressSignature.value.toPlainText())
            }

            if (mobileFooter.enabled && mobileFooter.value.isNotBlank()) {
                append(SignatureFooterSeparator)
                append(mobileFooter.value)
            }
        }.let { DraftBody(it.toString()) }

        return@either draftBodyWithAddressSignature
    }

    private fun getBodyWithReplacedSignature(
        draftBody: DraftBody,
        previousAddressSignature: SignatureValue,
        existingMobileFooter: String,
        addressSignature: Signature
    ): DraftBody {
        val previousSignatureIndex = previousAddressSignature.toPlainText()
            .takeIf { it.isNotEmpty() }
            ?.let { signature ->
                draftBody.value.lastIndexOf(signature).takeIf { it != -1 }
            }

        val bodyStringBuilder = StringBuilder(draftBody.value)
        val signatureReplacement = StringBuilder().apply {
            if (addressSignature.enabled) append(addressSignature.value.toPlainText()) else append("")
        }

        // If it has a signature.
        previousSignatureIndex?.let { lastIndex ->
            bodyStringBuilder.replace(
                lastIndex,
                previousAddressSignature.toPlainText().length + lastIndex,
                signatureReplacement.toString()
            )
            return DraftBody(bodyStringBuilder.toString())
        }

        // Footer needs not to be empty, in that case we add a separator to the signature replacement.
        val footerIndex = if (existingMobileFooter.isNotEmpty()) {
            draftBody.value.indexOf(existingMobileFooter).takeIf { it != -1 }
        } else {
            null
        }?.also { signatureReplacement.append(SignatureFooterSeparator) }

        // If it has no signature but a footer.
        footerIndex?.let { lastIndex ->
            bodyStringBuilder.replace(
                lastIndex,
                lastIndex,
                signatureReplacement.toString()
            )
            return DraftBody(bodyStringBuilder.toString())
        }

        // If it has nothing, add some spacing but only if the signature replacement is NOT blank.
        if (!draftBody.value.startsWith(SignatureFooterSeparator) && signatureReplacement.isNotBlank()) {
            bodyStringBuilder.append(SignatureFooterSeparator)
        }

        bodyStringBuilder.append(signatureReplacement.toString())
        return DraftBody(bodyStringBuilder.toString())
    }

    private companion object {

        const val SignatureFooterSeparator = "\n\n"
    }
}
