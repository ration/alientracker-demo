package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


/**
 * The single activity that has the google map fragment to show the destroyers
 *
 */
class AlienTrackerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val ufos = mutableMapOf<Int, Marker>()
    private lateinit var destroyer: Bitmap
    private var disposable: Disposable? = null

    private fun initDestroyer() {
        val height = 100
        val width = 100
        val b = BitmapFactory.decodeResource(resources, R.drawable.star_destroyer)
        destroyer = Bitmap.createScaledBitmap(b, width, height, false)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alien_tracker)
        initDestroyer()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Center and zoom to New York
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.7128, -74.0060), 11.0f))

        connectToAlienSocket()
    }


    /**
     * connect to the alien socket and subscribe to the alien tracking stream
     */
    private fun connectToAlienSocket() {
        val tracker = UfoSocket()

        disposable =
                tracker.track().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                    val marker = ufos.getOrPut(it.id) { createMarker(it) }
                    marker.moveMarker(LatLng(it.coordinate.first, it.coordinate.second), false, mMap)
                }, {
                    Log.e("aliens", "Failed ${it.message}", it)
                })
    }

    private fun createMarker(ufo: Ufo): Marker {
        return mMap.addMarker(
            MarkerOptions()
                .position(LatLng(ufo.coordinate.first, ufo.coordinate.second))
                .icon(BitmapDescriptorFactory.fromBitmap(destroyer))
        )
    }


}
