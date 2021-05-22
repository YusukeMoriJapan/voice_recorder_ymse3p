package ymse3p.app.voicelogger.util

import android.location.Location
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import ymse3p.app.voicelogger.models.GpsData

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            removeObserver(this)
            observer.onChanged(t)
        }
    })
}

fun <T> List<T>.divideList(paginationOverlap: Int, pageSizeLimit: Int): List<List<T>> {
    val dividedList = mutableListOf<List<T>>()
    var offset = 0
    while (offset < this.size) {
        if (offset > 0) offset -= paginationOverlap
        val lowerBound = offset
        val upperBound = (offset + pageSizeLimit).coerceAtMost(this.size)

        val quota = this.subList(lowerBound, upperBound)

        dividedList.add(quota)
        offset = upperBound
    }
    return dividedList
}

fun <T> List<List<T>>.reAllocateOneByOne(): List<T> {
    val oneByOneList = mutableListOf<T>()
    this.forEach { list ->
        list.forEach { element -> oneByOneList.add(element) }
    }
    return oneByOneList
}

fun List<Location>.locationListToGpsList(): List<GpsData> {
    return this.mapIndexed { index, location ->
        location.run { GpsData(latitude, longitude, altitude, bearing, speed, time, index) }
    }
}
