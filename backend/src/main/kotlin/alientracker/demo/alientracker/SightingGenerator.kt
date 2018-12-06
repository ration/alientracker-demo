package alientracker.demo.alientracker

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.springframework.stereotype.Component
import alientracker.demo.api.Ufo

@Component
class SightingGenerator : Sighting {
    private val source = PublishSubject.create<Ufo>()
    private var shipCount = 100
    private var generationSpeed = 1000L
    private var tickerSubject = BehaviorSubject.createDefault<Long>(generationSpeed)
    private val ships = mutableListOf<Ufo>()


    override fun sightings(): Flowable<Ufo> {
        return source.toFlowable(BackpressureStrategy.DROP)
    }


    private fun generateShips() {

    }
}