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

import me.proton.core.auth.fido.domain.entity.Fido2AuthenticationExtensionsClientInputs
import me.proton.core.auth.fido.domain.entity.Fido2AuthenticationOptions
import me.proton.core.auth.fido.domain.entity.Fido2PublicKeyCredentialDescriptor
import me.proton.core.auth.fido.domain.entity.Fido2PublicKeyCredentialRequestOptions
import uniffi.mail_account_uniffi.Fido2AuthenticationExtensionsClientInputsFfi
import uniffi.mail_account_uniffi.Fido2AuthenticationOptionsFfi
import uniffi.mail_account_uniffi.Fido2PublicKeyCredentialDescriptorFfi
import uniffi.mail_account_uniffi.Fido2PublicKeyCredentialRequestOptionsFfi

fun Fido2AuthenticationOptionsFfi.toNative(): Fido2AuthenticationOptions {
    return Fido2AuthenticationOptions(
        publicKey = this.publicKey.toNative()
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Fido2PublicKeyCredentialRequestOptionsFfi.toNative(): Fido2PublicKeyCredentialRequestOptions {
    return Fido2PublicKeyCredentialRequestOptions(
        challenge = this.challenge.toUByteArray(),
        timeout = this.timeout,
        rpId = this.rpId,
        allowCredentials = this.allowCredentials?.map { it.toNative() },
        userVerification = this.userVerification,
        extensions = this.extensions?.toNative()
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Fido2PublicKeyCredentialDescriptorFfi.toNative(): Fido2PublicKeyCredentialDescriptor {
    return Fido2PublicKeyCredentialDescriptor(
        type = this.credentialType,
        id = this.id.toUByteArray(),
        transports = this.transports
    )
}

fun Fido2AuthenticationExtensionsClientInputsFfi.toNative(): Fido2AuthenticationExtensionsClientInputs {
    return Fido2AuthenticationExtensionsClientInputs(
        appId = this.appId,
        thirdPartyPayment = this.thirdPartyPayment,
        uvm = this.uvm
    )
}
