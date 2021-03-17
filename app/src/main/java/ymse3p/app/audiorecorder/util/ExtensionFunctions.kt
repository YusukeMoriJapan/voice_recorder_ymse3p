package ymse3p.app.audiorecorder.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.text.SimpleDateFormat
import java.util.*

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            removeObserver(this)
            observer.onChanged(t)
        }
    })
}
//
//fun Calendar.toStringDate(): String {
//    val dataFormat = SimpleDateFormat(Constants.DATE_FORMAT)
//    return dataFormat.format(time)
//}