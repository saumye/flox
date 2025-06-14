package ai.flox.detail.di

import ai.flox.arch.Reducer
import ai.flox.detail.DetailViewModel
import ai.flox.detail.model.DetailState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
@InstallIn(SingletonComponent::class)
object DetailModule {

    @Provides
    @IntoMap
    @StringKey(DetailState.stateKey)
    fun provideDetailReducer(): Reducer<*, *> {
        return DetailViewModel()
    }
    
    @Provides
    @IntoMap
    @StringKey(DetailState.stateKey)
    fun provideDetailState(): ai.flox.state.State {
        return DetailState()
    }
} 