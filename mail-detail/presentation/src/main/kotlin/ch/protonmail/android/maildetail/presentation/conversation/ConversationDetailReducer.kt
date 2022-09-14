package ch.protonmail.android.maildetail.presentation.conversation

import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.conversation.model.ConversationDetailState
import javax.inject.Inject

class ConversationDetailReducer @Inject constructor() {

    fun reduce(
        currentState: ConversationDetailState,
        event: ConversationDetailEvent
    ) = when (event) {
        is ConversationDetailEvent.NoPrimaryUser -> ConversationDetailState.Error.NotLoggedIn
        is ConversationDetailEvent.NoConversationIdProvided -> ConversationDetailState.Error.NoConversationIdProvided
        is ConversationDetailEvent.ConversationData -> ConversationDetailState.Data(event.conversationUiModel)
        is ConversationDetailEvent.ErrorLoadingConversation -> TODO()
    }

}
