package ai.flox.advanced.di

import ai.flox.advanced.AdvancedModeViewModel
import ai.flox.advanced.model.AdvancedModeState
import ai.flox.arch.Reducer
import ai.flox.state.State
import android.content.Context
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
object AdvancedModeModule {

    @Provides
    @IntoMap
    @StringKey(AdvancedModeState.stateKey)
    fun provideReducer(@ApplicationContext context: Context): Reducer<*, *> {
        return AdvancedModeViewModel(context)
    }

    @Provides
    @IntoMap
    @StringKey(AdvancedModeState.stateKey)
    fun provideState(): State {
        return AdvancedModeState()
    }
}