package ai.flox.di

import ai.flox.AppState
import ai.flox.NavigationReducer
import ai.flox.arch.Reducer
import ai.flox.chat.model.ChatAction
import ai.flox.chat.model.ChatState
import ai.flox.model.AppAction
import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MainModule {

    @Singleton
    @Provides
    fun provideNavController(@ApplicationContext context: Context) : NavHostController = NavHostController(context)

    @Singleton
    @Provides
    @IntoMap
    @StringKey(AppState.stateKey)
    fun provideReducer(navController: NavController): Reducer<AppState, AppAction> {
        return NavigationReducer(navController)
    }

}
