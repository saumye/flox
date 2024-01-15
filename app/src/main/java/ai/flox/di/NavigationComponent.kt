package ai.flox.di

import ai.flox.AppState
import ai.flox.Navigable
import ai.flox.arch.Store
import ai.flox.chat.ChatNavigator
import ai.flox.chat.model.ChatState
import ai.flox.conversation.ConversationNavigator
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.State
import ai.flox.state.map
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object NavigationComponent {
    fun NavGraphBuilder.register(
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
    @Provides
    @IntoSet
    fun provideChatRoutes(store: Store<State, Action>): Navigable {
        return ChatNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[ChatState.stateKey] as ChatState else ChatState() })
    }

    @Provides
    @IntoSet
    fun provideConversationRoutes(store: Store<State, Action>): Navigable {
        return ConversationNavigator(store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[ConversationState.stateKey] as ConversationState else ConversationState() })
    }
}