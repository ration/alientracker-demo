package alientracker.demo.alientracker

import io.reactivex.Flowable
import alientracker.demo.api.Ufo

/**
 * Interface for sighting sources
 */
interface Sighting {
    fun sightings(): Flowable<Ufo>
}
