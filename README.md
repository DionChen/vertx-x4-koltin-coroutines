# Vert.x4 Kotlin Coroutines with Mongodb Example

A movie rating REST application written in Kotlin to demonstrate how it can Kotlin coroutines can be used with Vert.x.

To run this example, you need a running Mongo instance. Once running, you can configure the verticles with the mongo host:

```
{
"mongo_uri": "mongodb://localhost:27017",
"mongo_db" : "test"
}
```

By default it uses `localhost` as host. The port is set to `27017`.
In this example ,it will create two collection. (MOVIE and RATING)

## Running from the IDE

Run the main function from the IDE

## Running as a far jar

```
> mvn package
> java -jar target/kotlin-coroutines-examples.jar
```

## API

The application exposes a REST API for getRating movies:

You can know more about a movie
```
> curl http://localhost:8080/movie/starwars
{"id":"starwars","title":"Star Wars"}
```
You can get the current rating of a movie:

```
> curl http://localhost:8080/getRating/indianajones
{"id":"indianajones","rating":5}
```
You can rate a movie
```
> curl -X POST http://localhost:8080/rateMovie/starwars?rating=4
```

You can update or create a movie

```
> curl -X PUT http://localhost:8080/movie/starwars?title=StarWars2
{"id":"starwars","title":"Star Wars"} -> {"id":"starwars","title":"StarWars2"}
```

```
> curl -X PUT http://localhost:8080/movie/avengers?title=InfinityWar
```

Finally, you can delete a movie
```
curl -X DELETE http://localhost:8080/movie/avengers
```
