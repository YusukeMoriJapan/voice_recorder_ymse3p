package ymse3p.app.voicelogger.util

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

fun hasInternetConnection(application: Application): Boolean {
    val connectivityManager = application.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

