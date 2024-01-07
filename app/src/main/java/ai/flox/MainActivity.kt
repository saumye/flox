package ai.flox

import ai.flox.arch.CompositeReducer
import ai.flox.arch.PullbackReducer
import ai.flox.arch.Store
import ai.flox.arch.createStore
import ai.flox.chat.ChatListViewModel
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.chat.ui.ChatListScreen
import ai.flox.state.Action
import ai.flox.ui.theme.FloxTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val chatlistViewModel: ChatListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialState = ai.flox.AppState()
        val store: Store<ai.flox.AppState, Action> by lazy {
            createStore(
                initialState = initialState,
                reducer = CompositeReducer(
                    listOf(
                        PullbackReducer<ChatState, ai.flox.AppState, ChatAction, Action>(
                            innerReducer = chatlistViewModel,
                            mapToChildAction = { action -> action as ChatAction },
                            mapToChildState = { state -> state.chatState },
                            mapToParentAction = { action -> action },
                            mapToParentState = { state, chatState -> state.copy(chatState = chatState) },
                        )
                    )
                )
            )
        }

        setContent {
            FloxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BaseView(store, initialState)
                }
            }
        }
        store.dispatch(ChatAction.RecentChatsRendered)
    }

    @Composable
    fun BaseView(store: Store<ai.flox.AppState, Action>, initialState: ai.flox.AppState) {
        val state : ai.flox.AppState by store.state.collectAsStateWithLifecycle(initialState)
        ChatListScreen(state.chatState, store::dispatch)
    }

}