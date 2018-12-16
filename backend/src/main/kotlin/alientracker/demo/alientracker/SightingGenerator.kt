package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Action
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit


/**
 * Our fake sighting generator. Generates 100 ufos descending upon New York. Variable speed.
 */
@Component
class SightingGenerator : Sighting {
    private val LOG = LoggerFactory.getLogger(this.javaClass.name)

    private var shipCount = 100
    private val source = PublishSubject.create<Ufo>()
    private val ships = mutableSetOf<Ufo>()
    private lateinit var iterator: MutableIterator<Ufo>
    private var generationSpeed = 20L
    private var tickerSubject = BehaviorSubject.createDefault<Long>(generationSpeed)

    init {
        generateUfos()
        iterator = ships.iterator()
        setSpeed(1)
        createIntervalWithVariableTimer()
    }


    override fun sightings(): Flowable<Ufo> {
        return source.toFlowable(BackpressureStrategy.DROP).onBackpressureBuffer(100, {
            LOG.info("Dropping due to backpressure")
        }, BackpressureOverflowStrategy.DROP_OLDEST)
    }

    final override fun setSpeed(sightingsPerSecond: Int) {
        if (sightingsPerSecond > 0) {
            generationSpeed = 1000 / (sightingsPerSecond).toLong()
        }
    }

    private fun nextUfo(): Ufo {
        if (!iterator.hasNext()) {
            iterator = ships.iterator()
        }
        val ufo = iterator.next()
        moveRandomly(ufo)
        return ufo
    }

    private fun moveRandomly(ufo: Ufo) {
        val lat = ufo.coordinate.first + ThreadLocalRandom.current().nextDouble(-0.01, 0.01)
        val long = ufo.coordinate.second + ThreadLocalRandom.current().nextDouble(-0.01, 0.01)

        ufo.coordinate = Pair(lat, long)
    }

    private fun generateUfos() {
        repeat(shipCount) {
            ships.add(Ufo(it, Pair(40.7128, -74.0060)))
        }
    }

    private fun createIntervalWithVariableTimer() {
        tickerSubject
            .switchMap { Observable.interval(generationSpeed, TimeUnit.MILLISECONDS) }
            .subscribe {
                source.onNext(nextUfo())
                tickerSubject.onNext(generationSpeed)
            }
    }
}