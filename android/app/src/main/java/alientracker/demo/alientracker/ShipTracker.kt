package alientracker.demo.alientracker

import io.reactivex.Flowable
// TODO to api
interface ShipTracker {
    fun ships(): Flowable<Ship>
}
