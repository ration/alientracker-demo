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
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlinx.android.synthetic.main.activity_alien_tracker.*
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * The single activity that has the google map fragment to show the destroyers
 *
 */
class AlienTrackerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val ufos = mutableMapOf<Int, Marker>()
    private lateinit var destroyer: Bitmap
    private var disposables: CompositeDisposable = CompositeDisposable()

    private var sightingsPerSec = 0L
    private val tracker = UfoSocket()
    private var settingSpeed = false
    private val speedSubject: BehaviorSubject<Int> = BehaviorSubject.create()
    private var slider = 10

    init {
        initializeSpeedSettingSubject()
    }

    private fun initializeSpeedSettingSubject() {
        disposables.add(speedSubject.filter { !settingSpeed }
            .debounce(1, TimeUnit.SECONDS)
            .flatMap {
                tracker.setSpeed(it)
                    .subscribeOn(Schedulers.io()).toObservable<Int>()
            }
            .subscribe())
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
        disposables.clear()
    }


    private fun setupSeekBar() {
        seekBar.progress = 1
        updateSeekBar()
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                slider = i * 10 + 1
                updateSeekBar()
                speedSubject.onNext(slider)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Center and zoom to New York
        val newYork = LatLng(40.7128, -74.0060)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newYork, 11.0f))
        connectToAlienSocket()
    }


    /**
     * connect to the alien socket and subscribe to the alien tracking stream
     */
    private fun connectToAlienSocket() {
        // Wrap the socket flowable into another subject to just count the emissions and count them
        // We could do all kinds operators here as well to get the per/sec speed but let's keep it clean
        val seekBarUpdater = Subject.create<Irrelevant> { seekBarUpdater ->
            disposables.add(tracker.track()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    { ufo ->
                        doAndWaitOnUiThread {
                            val marker = ufos.getOrPut(ufo.id) { createMarker(ufo) }
                            marker.moveMarker(LatLng(ufo.coordinate.first, ufo.coordinate.second), false, mMap)
                            seekBarUpdater.onNext(Irrelevant.INSTANCE)
                        }
                    }, {
                        Log.e("aliens", "Failed ${it.message}", it)
                    })
            )
        }
        disposables.add(seekBarUpdater.buffer(5, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                sightingsPerSec = (it.count() / 5).toLong()
                updateSeekBar()
            }
        )
    }

    private fun doAndWaitOnUiThread(f: () -> Unit) {
        val latch = CountDownLatch(1)
        runOnUiThread {
            f.invoke()
            latch.countDown()
        }
        latch.await()
    }

    private enum class Irrelevant {
        INSTANCE
    }

    private fun updateSeekBar() {
        seekBarText.text = getString(R.string.seekbar_text, slider, sightingsPerSec)
    }

    private fun createMarker(ufo: Ufo): Marker {
        return mMap.addMarker(
            MarkerOptions()
                .position(LatLng(ufo.coordinate.first, ufo.coordinate.second))
                .icon(BitmapDescriptorFactory.fromBitmap(destroyer))
        )
    }


}
