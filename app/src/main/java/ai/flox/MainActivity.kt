package ai.flox

import ai.flox.chat.model.ChatAction
import ai.flox.state.Action
import ai.flox.state.ReducerManager
import ai.flox.ui.BottomBar
import ai.flox.ui.theme.FloxTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    private lateinit var reducerManager: ReducerManager

    @Inject
    private lateinit var routes: Set<Navigable>

    @Inject
    private lateinit var navController: NavHostController

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

    @Composable
    fun AppContent(state: AppState, dispatch: (action: Action) -> Unit) {
        FloxTheme {
            Scaffold(
                bottomBar = { BottomBar(state.bottomTabs, dispatch) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "chat"
                    ) {
                        for (route in routes) {
                            register(route, navController, Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FloxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state: AppState by reducerManager.store.state.collectAsStateWithLifecycle(
                        reducerManager.initialState
                    )
                    AppContent(state, reducerManager.store::dispatch)
                }
            }
        }
        reducerManager.store.dispatch(ChatAction.RecentChatsRendered)
    }
}