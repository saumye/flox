package ai.flox

import ai.flox.arch.Store
import ai.flox.chat.ChatRoutes
import ai.flox.conversation.model.ChatIds
import ai.flox.conversation.model.Conversation
import ai.flox.conversation.model.ConversationAction
import ai.flox.home.HomeRoutes
import ai.flox.di.NavigationComponent.register
import ai.flox.home.model.HomeIds
import ai.flox.model.AppAction
import ai.flox.model.AppIds.topBarIcon
import ai.flox.state.Action
import ai.flox.state.Resource
import ai.flox.state.State
import ai.flox.state.map
import ai.flox.ui.BottomBar
import ai.flox.ui.theme.FloxTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var store: Store<State, Action>

    @Inject
    lateinit var navigationReducer: NavigationReducer

    @Inject
    lateinit var routes: Set<@JvmSuppressWildcards Navigable>

    @Composable
    fun showNavBars(navController: NavHostController): Boolean {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        return navBackStackEntry?.destination?.route?.let {
            BottomTab.belongs(it)
        } ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appState: StateFlow<AppState> =
            store.state.map(CoroutineScope(Dispatchers.Main)) { (if (it is AppState) it else null)!! }
        setContent {
            val state: AppState by appState.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            navigationReducer.appNavController = navController
            FloxTheme {
                Scaffold(
                    topBar = {
                        if (showNavBars(navController)) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        }
                    },
                    bottomBar = {
                        if (showNavBars(navController)) {
                            BottomBar(
                                state.bottomBarState.bottomTabs,
                                navController,
                                store::dispatch
                            )
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoutes.home
                        ) {
                            for (route in routes) {
                                register(route, navController, Modifier.padding(innerPadding))
                            }
                        }
                    }
                }
            }
        }
    }
}