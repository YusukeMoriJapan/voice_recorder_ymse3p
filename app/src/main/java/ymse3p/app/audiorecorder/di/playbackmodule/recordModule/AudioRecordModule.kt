package ymse3p.app.audiorecorder.di.playbackmodule.recordModule

import android.media.MediaRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object AudioRecordModule {

    @ViewModelScoped
    @Provides
    fun provideMediaRecorder() = MediaRecorder()
}