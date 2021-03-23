package movierating

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.MongoClient
import io.vertx.ext.mongo.MongoClient.*
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.toChannel


class App : CoroutineVerticle() {

    private lateinit var client: MongoClient
    override suspend fun start() {

        client = createShared(
            vertx,
            JsonObject().put("connection_string", "mongodb://localhost:27017").put("db_name", "test123"),
            "mongodbPool"
        )


        // MongoDB Populate database

        val movieStatements = listOf<JsonObject>(
            JsonObject().put("_id", "starwars").put("TITLE", "Star Wars"),
            JsonObject().put("_id", "indianajones").put("TITLE", "Indiana Jones")
        )
        movieStatements.forEach {
            client.insert("MOVIE", it)
        }

        val ratingStatement = listOf<JsonObject>(
            JsonObject().put("VALUE", 1).put("MOVIE_ID", "starwars"),
            JsonObject().put("VALUE", 5).put("MOVIE_ID", "starwars"),
            JsonObject().put("VALUE", 9).put("MOVIE_ID", "starwars"),
            JsonObject().put("VALUE", 10).put("MOVIE_ID", "starwars"),
            JsonObject().put("VALUE", 4).put("MOVIE_ID", "indianajones"),
            JsonObject().put("VALUE", 7).put("MOVIE_ID", "indianajones"),
            JsonObject().put("VALUE", 3).put("MOVIE_ID", "indianajones"),
            JsonObject().put("VALUE", 9).put("MOVIE_ID", "indianajones"),

            )
        ratingStatement.forEach {
            client.insert("RATING", it)
        }

        // Build Vert.x Web router + bug report
        val router = Router.router(vertx).errorHandler(500) {
            it.failure()?.printStackTrace()
        }
        router.get("/movie/:id").coroutineHandler { ctx -> getMovie(ctx) }
        router.post("/rateMovie/:id").coroutineHandler { ctx -> rateMovie(ctx) }
        router.get("/getRating/:id").coroutineHandler { ctx -> getRating(ctx) }
        router.put("/movie/:id").coroutineHandler { ctx -> putMovie(ctx) }
        router.delete("/movie/:id").coroutineHandler { ctx -> deleteMovie(ctx) }


        // Start the server
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(config.getInteger("http.port", 8080))
            .await()
    }

    // Send info about a movie

    suspend fun getMovie(ctx: RoutingContext) {
        val id = ctx.pathParam("id")
        val row = client.findOne("MOVIE", JsonObject().put("_id", id), null).await()
        if (row != null) {
            ctx.response().end(json {
                obj("id" to id, "title" to row.get("TITLE")).encode()
            })
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }

    // Rate a movie

    suspend fun rateMovie(ctx: RoutingContext) {
        val movieId = ctx.pathParam("id")
        val rating = Integer.parseInt(ctx.queryParam("rating")[0])
        val row = client.findOne("MOVIE", JsonObject().put("_id", movieId), null).await()

        if (row != null) {
            client.insert("RATING", JsonObject().put("VALUE", rating).put("MOVIE_ID", movieId)).await()
            ctx.response().setStatusCode(200).end()
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }

    // Get the current rating of a movie
    suspend fun getRating(ctx: RoutingContext) {
        val id = ctx.pathParam("id")
        val pipeline = JsonArray().add(JsonObject().put("\$match", JsonObject().put("MOVIE_ID", id)))
            .add(
                JsonObject().put(
                    "\$group",
                    JsonObject().put("_id", "avg").put("value_avg", JsonObject().put("\$avg", "\$VALUE"))
                )
            )
        val labels = client.aggregate("RATING", pipeline).toChannel(vertx).iterator()


        if (labels.hasNext()) {
            ctx.response().end(json {
                obj("id" to id, "rating" to labels.next().getDouble("value_avg").toInt()).encode()
            })
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }


    // Put a movie
    suspend fun putMovie(ctx: RoutingContext) {
        val id = ctx.pathParam("id")
        val title = ctx.queryParam("title")[0]
//        val testparam = ctx.queryParams().toString().split("?")
//        val test2 = ctx.request().params().toString()
//        println(test2)
//        val map = testparam.associate {
//            val (letf,right) = it.split("=")
//            letf.toUpperCase() to right.replace("\n","")
//        }
//        println(JsonObject(map as Map<String, Any>?).toString())

        val query = JsonObject().put("_id", id)
        val update = JsonObject().put("\$set", JsonObject().put("_id", id).put("TITLE", title))
        val row = client.findOneAndUpdate("MOVIE", query, update).await()

        if (row != null) {
            println("Update suc")
            ctx.response().setStatusCode(200).end()
        } else {
            client.insert("MOVIE",JsonObject().put("_id",id).put("TITLE",title)).await()
            println("Create new")
            ctx.response().setStatusCode(200).end()
        }
    }

    // Delete a movie
    suspend fun deleteMovie(ctx: RoutingContext) {
        val id = ctx.pathParam("id")
        val query = JsonObject().put("_id", id)
        val row = client.findOneAndDelete("MOVIE", query).await()

        if (row != null) {
            ctx.response().end(json {
                row.encodePrettily()
            })
        } else {
            ctx.response().setStatusCode(404).end()
        }
    }

    /**
     * An extension method for simplifying coroutines usage with Vert.x Web routers
     */
    fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
        handler { ctx ->
            launch(ctx.vertx().dispatcher()) {
                try {
                    fn(ctx)
                } catch (e: Exception) {
                    ctx.fail(e)
                }
            }
        }
    }
}