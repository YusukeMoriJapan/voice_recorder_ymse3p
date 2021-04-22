package ymse3p.app.audiorecorder.util

import android.app.Application
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun getSampleJson(application: Application): String {
    val inputStream: InputStream =
        application.assets.open("snap_to_roads_sample.json")
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))

    var json = ""
    var str: String? = bufferedReader.readLine()

    while (str != null) {
        json += str
        str = bufferedReader.readLine()
    }

    return json
}

