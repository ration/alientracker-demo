package alientracker.demo.alientracker

import android.os.Handler
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker


/**
 * Helper to make the marker transitions more pretty
 */
fun Marker.moveMarker(
    toPosition: LatLng,
    hideMarker: Boolean, mGoogleMapObject: GoogleMap
) {
    val handler = Handler()
    val start = SystemClock.uptimeMillis()
    val proj = mGoogleMapObject.projection
    val startPoint = proj.toScreenLocation(this.position)
    val startLatLng = proj.fromScreenLocation(startPoint)
    val duration: Long = 200

    val interpolator = LinearInterpolator()

    handler.post(object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - start
            val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
            val lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude
            val lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude
            this@moveMarker.position = LatLng(lat, lng)

            if (t < 1.0) {
                // Post again 16ms later.
                    handler.postDelayed(this, 16)
            } else {
                this@moveMarker.isVisible = !hideMarker
            }
        }
    })
    this.position = toPosition

}
