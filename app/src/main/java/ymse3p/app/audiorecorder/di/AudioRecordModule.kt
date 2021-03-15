package ymse3p.app.audiorecorder.di

import android.content.Context
import android.media.MediaRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AudioRecordModule {

    @ViewModelScoped
    @Provides
    fun provideMediaRecorder() = MediaRecorder()
}