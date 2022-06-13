package ch.protonmail.android.mailmailbox.domain.extension

import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.mailsettings.domain.entity.ViewMode

suspend fun Flow<ViewMode>.firstOrDefault() = firstOrNull() ?: ObserveCurrentViewMode.DefaultViewMode
