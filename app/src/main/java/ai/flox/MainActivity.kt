package ai.flox

import ai.flox.arch.Store
import ai.flox.chat.ChatRoutes
import ai.flox.chat.model.ChatAction
import ai.flox.state.Action
import ai.flox.state.State
import ai.flox.state.map
import ai.flox.ui.BottomBar
import ai.flox.ui.theme.FloxTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
    lateinit var routes: Set<@JvmSuppressWildcards Navigable>

    @Inject
    lateinit var navController: NavHostController

    private fun NavGraphBuilder.register(
        route: Navigable,
        navController: NavHostController,
        modifier: Modifier = Modifier
    ) {
        route.registerGraph(
            navGraphBuilder = this,
            navController = navController,
            modifier = modifier
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appState: StateFlow<AppState> = store.state.map(CoroutineScope(Dispatchers.Main)) { (if (it is AppState) it else null)!! }
        setContent {
            val navController = rememberNavController()
            val state: AppState by appState.collectAsStateWithLifecycle()
            FloxTheme {
                Scaffold(
                    bottomBar = { BottomBar(state.bottomBarState.bottomTabs, store::dispatch) }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = ChatRoutes.chat
                        ) {
                            for (route in routes) {
                                register(route, navController, Modifier.padding(innerPadding))
                            }
                        }
                    }
                }
            }
        }
        store.dispatch(ChatAction.RecentChatsRendered)
    }
}