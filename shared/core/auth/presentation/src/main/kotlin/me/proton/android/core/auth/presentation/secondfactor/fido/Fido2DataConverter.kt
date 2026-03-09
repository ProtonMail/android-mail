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

package me.proton.android.core.auth.presentation.secondfactor.fido

import me.proton.core.auth.fido.domain.entity.SecondFactorProof
import uniffi.mail_account_uniffi.Fido2AuthenticationExtensionsClientInputsFfi
import uniffi.mail_account_uniffi.Fido2AuthenticationOptionsFfi
import uniffi.mail_account_uniffi.Fido2PublicKeyCredentialDescriptorFfi
import uniffi.mail_account_uniffi.Fido2PublicKeyCredentialRequestOptionsFfi
import uniffi.mail_account_uniffi.Fido2RequestFfi

fun SecondFactorProof.Fido2.toFido2Data(): Fido2RequestFfi {
    val authenticationOptions = createAuthenticationOptions()

    return Fido2RequestFfi(
        authenticationOptions = authenticationOptions,
        clientData = clientData,
        authenticatorData = authenticatorData,
        signature = signature,
        credentialId = credentialID
    )
}

private fun SecondFactorProof.Fido2.createAuthenticationOptions(): Fido2AuthenticationOptionsFfi =
    Fido2AuthenticationOptionsFfi(
        publicKey = createPublicKeyCredentialRequestOptions()
    )

@OptIn(ExperimentalUnsignedTypes::class)
private fun SecondFactorProof.Fido2.createPublicKeyCredentialRequestOptions():
    Fido2PublicKeyCredentialRequestOptionsFfi =
    Fido2PublicKeyCredentialRequestOptionsFfi(
        challenge = publicKeyOptions.challenge.toByteArray(),
        timeout = publicKeyOptions.timeout,
        rpId = publicKeyOptions.rpId,
        allowCredentials = publicKeyOptions.allowCredentials?.map { credential ->
            Fido2PublicKeyCredentialDescriptorFfi(
                credentialType = credential.type,
                id = credential.id.toByteArray(),
                transports = credential.transports
            )
        },
        userVerification = publicKeyOptions.userVerification,
        extensions = Fido2AuthenticationExtensionsClientInputsFfi(
            appId = publicKeyOptions.extensions?.appId,
            thirdPartyPayment = publicKeyOptions.extensions?.thirdPartyPayment,
            uvm = publicKeyOptions.extensions?.uvm
        )
    )
