package de.nielsfalk.playground.ktor.swagger

import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.CallLogging
import org.jetbrains.ktor.features.Compression
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.gson.GsonSupport
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.locations.Locations
import org.jetbrains.ktor.locations.location
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing
import java.lang.Integer.getInteger

/**
 * @author Niels Falk
 */

data class PetModel(val id: Int?, val name: String)
data class PetsModel(val pets: MutableList<PetModel>)

val data = PetsModel(mutableListOf(PetModel(1, "max"), PetModel(2, "moritz")))
fun newId() = ((data.pets.map { it.id ?: 0 }.max()) ?: 0) + 1

@location("/pets/{id}")
class pet(val id: Int)

@location("/pets")
class pets

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, getInteger("server.port", 8080)) {
        install(DefaultHeaders)
        install(Compression)
        install(CallLogging)
        install(GsonSupport) {
            setPrettyPrinting()
        }
        install(Locations)
        routing {
            get<pets>("all".responds(ok<PetsModel>())) {
                call.respond(data)
            }
            post<pets, PetModel>("create".responds(ok<PetModel>())) { _, entity ->
                //http201 would be better but there is no way to do this see org.jetbrains.ktor.gson.GsonSupport.renderJsonContent
                call.respond(entity.copy(id = newId()).apply {
                    data.pets.add(this)
                })
            }
            get<pet>("find".responds(ok<PetModel>(), notFound())) { params ->
                data.pets.find { it.id == params.id }
                        ?.let {
                            call.respond(it)
                        }
            }
            put<pet, PetModel>("update".responds(ok<PetModel>(), notFound())) { params, entity ->
                if (data.pets.removeIf { it.id == params.id && it.id == entity.id }) {
                    data.pets.add(entity)
                    call.respond(entity)
                }
            }
            delete<pet>("delete".responds(ok<Unit>(), notFound())) { params ->
                if (data.pets.removeIf { it.id == params.id }) {
                    call.respond(Unit)
                }
            }
            swagger.attribute("info").apply {
                put("description", "an example to generate swagger with ktor")
                put("version", "0.1")
                put("title", "sample api implemented in ktor")
                attribute("contact").apply {
                    put("name", "Niels Falk")
                    put("url", "https://github.com/nielsfalk/ktor-swagger")
                }
            }
            swaggerUi("apidocs")
            get("/") {
                call.respondRedirect("apidocs")
            }
        }
    }
    server.start(wait = true)
}