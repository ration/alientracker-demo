package alientracker.demo.alientracker

import io.reactivex.Flowable
import alientracker.demo.api.Ufo

interface Sighting {
    fun sightings(): Flowable<Ufo>
}
