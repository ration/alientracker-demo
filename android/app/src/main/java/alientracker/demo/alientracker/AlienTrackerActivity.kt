package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_alien_tracker.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


/**
 * The single activity that has the google map fragment to show the destroyers
 *
 */
class AlienTrackerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val ufos = mutableMapOf<Int, Marker>()
    private lateinit var destroyer: Bitmap
    private var disposable: CompositeDisposable = CompositeDisposable()

    private var sightingsPerSec = 0L
    private val tracker = UfoSocket()
    private var settingSpeed = false
    private val speedSubject: BehaviorSubject<Int> = BehaviorSubject.create()

    init {
        initializeSpeedSettingSubject()
    }

    private fun initializeSpeedSettingSubject() {
        disposable.add(speedSubject.filter { !settingSpeed }
            .debounce(1, TimeUnit.SECONDS)
            .subscribe {
                settingSpeed = true
                tracker.setSpeed(it)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io()).subscribe {
                        settingSpeed = false
                    }
            })
    }

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

        setupSeekBar()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }


    private fun setupSeekBar() {
        updateSeekBar()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                seekBarText.text = getString(R.string.seekbar_text, i, sightingsPerSec)

                speedSubject.onNext(i)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
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
        disposable.add(tracker
            .track()
            .timeInterval()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    val ufo = it.value()
                    val marker = ufos.getOrPut(ufo.id) { createMarker(ufo) }
                    marker.moveMarker(LatLng(ufo.coordinate.first, ufo.coordinate.second), false, mMap)
                    sightingsPerSec = (1 / (it.time().toDouble() / 1000)).toLong()
                    updateSeekBar()
                }, {
                    Log.e("aliens", "Failed ${it.message}", it)
                }))
    }

    private fun updateSeekBar() {
        seekBarText.text = getString(R.string.seekbar_text, seekBar.progress, sightingsPerSec)
    }

    private fun createMarker(ufo: Ufo): Marker {
        return mMap.addMarker(
            MarkerOptions()
                .position(LatLng(ufo.coordinate.first, ufo.coordinate.second))
                .icon(BitmapDescriptorFactory.fromBitmap(destroyer))
        )
    }


}
