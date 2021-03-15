package ymse3p.app.audiorecorder.util

class Constants {
    companion object{
        // ROOM database
        const val DATABASE_NAME = "audio_database"
        const val AUDIO_TABLE = "audio_table"

        //Date format
        const val DATE_FORMAT = "yyyy/MM/dd HH:mm:ss:SSS"

        //Permission request code
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        //Data store preferences
        const val RECORDED_AUDIO_ID = "recorded_audio_id"
        const val PREFERENCES_NAME = "recorded_audio_number"
    }
}