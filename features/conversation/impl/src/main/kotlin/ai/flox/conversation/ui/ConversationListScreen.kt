package ai.flox.conversation.ui

import ai.flox.arch.Store
import ai.flox.chat.ChatRoutes
import ai.flox.chat.model.ChatAction
import ai.flox.conversation.R
import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.Resource
import ai.flox.state.State
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ConversationListScreen(
    stateFlow: StateFlow<ConversationState>,
    store: Store<State, Action>
) {
    val state: ConversationState by stateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        store.dispatch(ConversationAction.RenderConversationList)
    }
    state.recentConversationList.let {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        store.dispatch(
                            ConversationAction.CreateOrUpdateConversation(
                                Resource.Pending(
                                    Conversation(title = "New Conversation")
                                )
                            ),
                            Action.Navigate(
                                route = ChatRoutes.chat
                            )
                        )
                    }) {
                    Icon(
                        painter = painterResource(id = R.drawable.create_chat),
                        modifier = Modifier.size(24.dp),
                        contentDescription = "Create Chat"
                    )
                }
            }
            val (messages, chatBox) = createRefs()
            val listState = rememberLazyListState()
            LazyColumn(
                userScrollEnabled = true,
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(messages) {
                        top.linkTo(parent.top)
                        bottom.linkTo(chatBox.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    }
            ) {
                items(it.values.toList()) { item ->
                    Conversation(item, store)
                }
            }
        }
    }
}


@Composable
fun Conversation(
    conversation: Conversation,
    store: Store<State, Action>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { store.dispatch(ChatAction.RenderChatList(conversation)) }
    ) {
        Text(
            text = conversation.title,
            color = Color.Black,
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Text(
            text = conversation.lastMessageTime.toString(),
            color = Color.Gray,
            textAlign = TextAlign.End,
            fontSize = 16.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}