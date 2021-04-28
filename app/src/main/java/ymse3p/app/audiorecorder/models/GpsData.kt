package ymse3p.app.audiorecorder.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val bearing: Float = 0F,
    val speed: Float = 0F,
    val time: Long = 0,
    val originalIndex: Int? = null,
) : Parcelable {
}