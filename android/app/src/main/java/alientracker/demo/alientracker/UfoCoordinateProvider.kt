package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import io.reactivex.Completable
import io.reactivex.Flowable

/**
 * Flowable interface for Ufo location providers.
 */
interface UfoCoordinateProvider {
    fun track(): Flowable<Ufo>
    fun setSpeed(value: Int): Completable
}