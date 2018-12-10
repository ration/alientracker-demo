package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import io.reactivex.Flowable
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit


/**
 * Our fake sighting generator. Generates 100 ufos descending upon New York and they
 * are sighted every 20 ms (so each ufo moves every 2s)
 */
@Component
class SightingGenerator : Sighting {
    private var shipCount = 100
    private val ships = mutableSetOf<Ufo>()
    private lateinit var iterator: MutableIterator<Ufo>

    init {
        generateUfos()
        iterator = ships.iterator()
    }


    override fun sightings(): Flowable<Ufo> {
        return Flowable.interval(20, TimeUnit.MILLISECONDS).map { nextUfo() }
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
}