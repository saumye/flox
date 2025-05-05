package ai.flox.chat.ui

import ai.flox.advanced.AdvancedModeRoutes
import ai.flox.advanced.model.AdvancedModeAction
import ai.flox.arch.Store
import ai.flox.chat.R
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatMessage
import ai.flox.chat.model.ChatState
import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow


@Composable
fun ChatListScreen(
    stateFlow: StateFlow<ChatState>,
    store: Store<State, Action>,
    conversationId: String
) {
    val state: ChatState by stateFlow.collectAsStateWithLifecycle()
    ConstraintLayout(modifier = Modifier.fillMaxSize()) {
        val (topBar, messages, chatBox) = createRefs()

        // Top Bar with online/offline toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .constrainAs(topBar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            // Online/Offline toggle with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Switch(
                    checked = state.isOnlineMode,
                    onCheckedChange = { isOnline ->
                        store.dispatch(ChatAction.ToggleOnlineMode(isOnline, conversationId))
                    }
                )
                Icon(
                    imageVector = if (state.isOnlineMode) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (state.isOnlineMode) "Online Mode" else "Offline Mode",
                    tint = if (state.isOnlineMode) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                )
            }

            // Model name in center
            Text(
                text = if (state.isOnlineMode) "OpenAI" else "Local LLM",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Messages
        val listState = rememberLazyListState()
        state.recentChatList.values.toList().let { list ->
            LazyColumn(
                userScrollEnabled = true,
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(messages) {
                        top.linkTo(topBar.bottom)
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
            LaunchedEffect(list.size) {
                listState.animateScrollToItem(list.size)
            }
        }

        // Compose Box
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
    dispatchEvent: (Action) -> Unit,
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
                    .weight(0.7f)
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
                    modifier = modifier
                        .size(48.dp)
                        .clickable {
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
            Column(modifier = Modifier.weight(0.15f)) {
                Image(
                    modifier = modifier
                        .size(48.dp)
                        .clickable {
                            conversation?.let {
                                dispatchEvent(AdvancedModeAction.RenderAdvancedMode(conversation = conversation))
                            }
                        },
                    imageVector = ImageVector.vectorResource(R.drawable.ic_mic),
                    contentDescription = "create audio",
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