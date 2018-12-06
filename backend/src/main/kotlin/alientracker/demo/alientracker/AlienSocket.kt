package alientracker.demo.alientracker

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.rsocket.kotlin.DefaultPayload
import io.rsocket.kotlin.Payload
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketFactory
import io.rsocket.kotlin.transport.netty.server.NettyContextCloseable
import io.rsocket.kotlin.transport.netty.server.WebsocketServerTransport
import io.rsocket.kotlin.util.AbstractRSocket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller

/**
* RSocket endpoint for sightings
 */
@Controller
class SightingSocket(@Autowired private val sighting: Sighting) {
    private val LOG = LoggerFactory.getLogger(this.javaClass.name)
    private val port = 9988
    private val mapper = ObjectMapper()

    // Initializer for the RSocket. Incoming WebSocket requests are handled
    // by this RSocket receiver and handled by the handler() method
    // Uses Netty websocket transport
    private val closeable: Single<NettyContextCloseable> = RSocketFactory
        .receive()
        .acceptor { { _, _ -> handler() } } // server handler RSocket
        .transport(WebsocketServerTransport.create("localhost", port))
        .start()
    init {
        closeable.subscribe({
            LOG.info("subscribed = $it")
        }, {
            LOG.error("it = $it")
        })
    }

    /**
     * Handler for the socket. Connects the sightings to the RSocket
     * and maps the items into JSON
     */
    private fun handler(): Single<RSocket> {
        LOG.info("Single just")

        return Single.just(object : AbstractRSocket() {
            // Here we could implement more of the API from AbstractSocket and provide e.g. single request/response
            // data. We want just a stream
            override fun requestStream(payload: Payload): Flowable<Payload> {
                LOG.info("Client requested stream")
                return sighting.sightings().observeOn(Schedulers.io()).map {
                    DefaultPayload.text(mapper.writeValueAsString(it)) }
                // TODO filter with payload
            }
        })
    }
}