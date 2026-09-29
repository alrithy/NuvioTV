package com.nuvio.tv.fork.foundation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ForkFoundationModule {
    @Provides
    @Singleton
    fun provideFeatureRegistry(): FeatureRegistry = FeatureRegistry()
}
