package alientracker.demo.alientracker

import alientracker.demo.api.Ufo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.reactivex.Flowable
import io.reactivex.Single
import io.rsocket.kotlin.DefaultPayload
import io.rsocket.kotlin.Duration
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketFactory
import io.rsocket.transport.okhttp.client.OkhttpWebsocketClientTransport
import okhttp3.HttpUrl


/**
 * The WebSocket connection to the back end ufo with RSockets.
 */
class UfoSocket : UfoCoordinateProvider {
    // Our development host
    private val port = 9988
    private val host = "10.0.2.2"
    private val mapper = ObjectMapper().registerModule(KotlinModule())

    /**
     * Initializes the connection and requests the ufo stream.
     */
    override fun track(): Flowable<Ufo> {
        return initializeRSocket()
            .toFlowable()
            .flatMap { requestShipStream(it) }
    }

    /**
     * Request a  stream of ufos from the back end. Depending on the payload we could
     * configure the back end to return something else, or use some operator such as
     * requestChannel for bi-directional communication
     */
    private fun requestShipStream(socket: RSocket): Flowable<Ufo> {
        return socket
            .requestStream(DefaultPayload.EMPTY)
            .map { mapper.readValue(it.dataUtf8, Ufo::class.java) }
    }



    /**
     *  We should hold on to the socket separately to allow other request types and share the connection
     *  between subscribers.
     *  We should also do better error handling on it here.
     */
    private fun initializeRSocket(): Single<RSocket> {
        return RSocketFactory
            .connect()
            .keepAlive {
                it.keepAliveInterval(Duration.ofSeconds(42)).keepAliveMaxLifeTime(Duration.ofMinutes(1))
            }
            .transport(OkhttpWebsocketClientTransport.create(HttpUrl.get("http://$host:$port")))
            .start()
    }
}
