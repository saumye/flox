package ai.flox.di

import ai.flox.AppState
import ai.flox.Navigable
import ai.flox.advanced.AdvancedModeNavigator
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.arch.Store
import ai.flox.chat.ChatNavigator
import ai.flox.chat.model.ChatState
import ai.flox.detail.DetailNavigator
import ai.flox.detail.model.DetailState
import ai.flox.home.HomeNavigator
import ai.flox.home.model.HomeState
import ai.flox.conversation.ConversationNavigator
import ai.flox.conversation.model.ConversationState
import ai.flox.state.Action
import ai.flox.state.State
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Singleton

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
    @Singleton
    fun provideChatRoutes(store: Store<State, Action>): Navigable {
        return ChatNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[ChatState.stateKey] as ChatState else ChatState() })
    }

    @Provides
    @IntoSet
    @Singleton
    fun provideHomeRoutes(store: Store<State, Action>): Navigable {
        return HomeNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[HomeState.stateKey] as HomeState else HomeState() })
    }

    @Provides
    @IntoSet
    @Singleton
    fun provideConversationRoutes(store: Store<State, Action>): Navigable {
        return ConversationNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[ConversationState.stateKey] as ConversationState else ConversationState() })
    }
    @Provides
    @IntoSet
    @Singleton
    fun provideAdvancedModeRoutes(store: Store<State, Action>): Navigable {
        return AdvancedModeNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[AdvancedModeState.stateKey] as AdvancedModeState else AdvancedModeState() })
    }

    @Provides
    @IntoSet
    @Singleton
    fun provideDetailRoutes(store: Store<State, Action>): Navigable {
        return DetailNavigator(store, store.state.map(CoroutineScope(Dispatchers.Main)) { if(it is AppState) it.featureStates[DetailState.stateKey] as DetailState else DetailState() })
    }

    fun <T, M> StateFlow<T>.map(
        coroutineScope : CoroutineScope,
        mapper : (value : T) -> M
    ) : StateFlow<M> = map { mapper(it) }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        mapper(value)
    )
}