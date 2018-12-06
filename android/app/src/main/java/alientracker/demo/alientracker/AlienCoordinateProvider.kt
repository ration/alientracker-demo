package alientracker.demo.alientracker

import io.reactivex.Flowable

interface AlienCoordinateProvider {
    fun track(): Flowable<AlienShip>
}