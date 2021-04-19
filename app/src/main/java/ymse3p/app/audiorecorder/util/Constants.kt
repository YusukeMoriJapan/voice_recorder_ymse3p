package ymse3p.app.audiorecorder.util

import ymse3p.app.audiorecorder.BuildConfig

class Constants {
    companion object {
        const val BASE_URL = "https://roads.googleapis.com/v1/"
        const val API_KEY = BuildConfig.ApiKey

        // ROOM database
        const val DATABASE_NAME = "audio_database"
        const val AUDIO_TABLE = "audio_table"

        //Date format
        const val DATABASE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss:SSS"

        //Permission request code
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        //Data store preferences
        const val RECORDED_AUDIO_ID = "recorded_audio_id"
        const val PREFERENCES_NAME = "recorded_audio_number"

        //Notification Channel ID
        const val NOTIFICATION_CHANNEL_ID_PLAYBACK = "playback_channel"

        //Foreground notification ID
        const val FOREGROUND_NOTIFICATION_ID_PLAYBACK = 1

        //Custom MediaDescription Key
        const val MEDIA_METADATA_QUEUE = "media_metadata_queue"
    }
}