package alientracker.demo.alientracker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.rsocket.kotlin.DefaultPayload
import io.rsocket.kotlin.Duration
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketFactory
import io.rsocket.transport.okhttp.client.OkhttpWebsocketClientTransport
import okhttp3.HttpUrl
import alientracker.demo.api.Ufo


class AlienSocketClient : AlienCoordinateProvider {
    val subject: PublishSubject<AlienShip> = PublishSubject.create()
    val port = 9988
    val host = "10.0.2.2"
    var subscribe: Disposable? = null
    val mapper = ObjectMapper().registerModule(KotlinModule())

    override fun track(): Flowable<AlienShip> {
        return initializeRSocket()
            .toFlowable()
            .flatMap { requestShipStream(it) }
    }

    private fun requestShipStream(socket: RSocket): Flowable<AlienShip>? {
        return socket
            .requestStream(DefaultPayload.EMPTY).map { mapper.readValue(it.dataUtf8, Ufo::class.java) }
            .map { AlienShip(it.id, LatLng(it.coordinate.first, it.coordinate.second)) }
    }

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