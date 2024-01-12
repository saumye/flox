package ai.flox.conversation.ui

import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun ConversationListScreen(
    modifier: Modifier,
    state: ConversationState,
    dispatchEvent: (Action) -> Unit
) {
    Log.d("ConversationListScreen", "State: $state")
    state.recentConversationList.let {
        ConstraintLayout(modifier = Modifier.fillMaxSize()) {
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
                    Conversation(item)
                }
            }
        }
    }
}


@Composable
fun Conversation(
    conversation: Conversation
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Text(
                text = conversation.title,
                color = Color.White
            )
        }
    }
}