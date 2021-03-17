package com.example.socialapp.di.main

import com.example.socialapp.repositories.main.MainRepository
import com.example.socialapp.repositories.main.MainRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped


@Module
@InstallIn(ActivityComponent::class)
object MainModule {


    @ActivityScoped
    @Provides
    fun provideMainRepository() : MainRepository =
        MainRepositoryImpl()
}