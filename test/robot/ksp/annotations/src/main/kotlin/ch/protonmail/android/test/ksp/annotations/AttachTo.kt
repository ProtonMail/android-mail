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

package ch.protonmail.android.test.ksp.annotations

import ch.protonmail.android.test.robot.ProtonMailRobot
import kotlin.reflect.KClass

/**
 * Creates an extension function for each provided [ProtonMailRobot] target to
 * allow using the annotated class as custom DSL with closures.
 *
 * @param targets the targets [ProtonMailRobot] to create the extension functions on.
 * @param identifier a custom identifier to be used as the name of the extension function.
 * If empty, it will default to the lowercased annotated class' name.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class AttachTo(
    val targets: Array<KClass<out ProtonMailRobot>>,
    val identifier: String = ""
)
