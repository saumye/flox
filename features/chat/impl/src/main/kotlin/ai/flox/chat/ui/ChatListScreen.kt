package ai.flox.chat.ui

import ai.flox.arch.Store
import ai.flox.chat.R
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatState
import ai.flox.conversation.model.Conversation
import ai.flox.state.Action
import ai.flox.state.State
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow


@Composable
fun ChatListScreen(
    stateFlow: StateFlow<ChatState>,
    store: Store<State, Action>
) {
    val state: ChatState by stateFlow.collectAsStateWithLifecycle()
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (messages, chatBox) = createRefs()
        val listState = rememberLazyListState()
        state.recentChatList.values.toList().let { list ->
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
                items(count = list.size, key = {
                    list[it].id
                }, itemContent = { index ->
                    ChatMessage(list[index])
                })
            }
        }
        ComposeBox(
            composeState = state.composeState,
            dispatchEvent = store::dispatch,
            conversation = state.conversation,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(chatBox) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )
    }
}


@Composable
fun ComposeBox(
    composeState: ChatState.ComposeState,
    conversation: Conversation?,
    dispatchEvent: (ChatAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by rememberSaveable { mutableStateOf(composeState.userInput) }
    Row(modifier = modifier) {
        Row(
            modifier = Modifier
                .weight(1f, false)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxWidth()
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(bottom = 16.dp))
                        .clip(
                            RoundedCornerShape(
                                topStart = 64f,
                                topEnd = 64f,
                                bottomStart = 64f,
                                bottomEnd = 64f
                            )
                        ),
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    label = {
                        Text("Enter Message")
                    }
                )
            }
            Column(modifier = Modifier.weight(0.15f)) {
                Image(
                    modifier = modifier.size(48.dp).clickable {
                        conversation?.let {
                            dispatchEvent(
                                ChatAction.SendMessage(text, it)
                            )
                        }
                    },
                    imageVector = ImageVector.vectorResource(R.drawable.top_arrow_circle),
                    contentDescription = "create chat",
                    alignment = Alignment.Center
                )

            }
        }
    }
}

@Composable
fun ChatMessage(
    message: ChatMessage
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .align(if (message.isSelf()) Alignment.End else Alignment.Start)
                .clip(
                    RoundedCornerShape(
                        topStart = 48f,
                        topEnd = 48f,
                        bottomStart = if (message.isSelf()) 48f else 0f,
                        bottomEnd = if (message.isSelf()) 0f else 48f
                    )
                )
                .background(if (message.isSelf()) Color.Black else Color.DarkGray)
                .padding(16.dp)
        ) {
            Text(
                text = message.message,
                color = Color.White
            )
        }
    }
}