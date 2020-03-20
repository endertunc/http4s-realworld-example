//package com.real.world.http4s
//
//import io.circe.Decoder.Result
//
//// $COVERAGE-OFF$
//object KinderGarden extends App {
//
//  import io.circe.literal._
//  import io.circe.generic.auto._
//
//  case class Example(a: Int, b: Option[String])
//  import cats.syntax.show._
//  import io.circe.CursorOp._
//  val example1: Result[Example] =
//    json"""
//          {
//            "a" : "A",
//            "b" : null
//          }
//        """.as[Example]
//
//  val example2: Result[Example] =
//    json"""
//          {
//            "a" : 10
//          }
//        """.as[Example]
//
//  println(example1.left.get.show)
//  println(example2)
//  import com.github.andyglow.jsonschema.AsCirce._
//  import json.schema.Version._
//  import io.circe.generic.auto._
//  import io.circe.syntax._
//
//  case class Example(a: String = "Hello", b: Option[String])
//  implicit val exampleSchema: Schema[Example] = json.Json.schema[Example]
//  val validator                               = io.circe.schema.Schema.load(exampleSchema.asCirce(Draft07(id = "example")))
//
//  println(JsonFormatter.format(AsValue.schema(exampleSchema, Draft07(id = "example"))))
//  println(Example("Hello", Some("World")).asJson.noSpaces)
//  println(validator.validate(Example("Hello", Some("World")).asJson))
//  println(Example("Hello", None).asJson.noSpaces)
//  println(validator.validate(Example("Hello", None).asJson))
//  --
  //  implicit val config: Configuration = Configuration.default.withDefaults.withStrictDecoding
//  println(operationsJson.toOption.get.as[List[Operation]])
  //  import io.circe.generic.extras.Configuration
//  import io.circe.generic.extras.auto._
//  import io.circe.syntax._
//
//  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("what_am_i")
//
//  sealed trait Event
//
//  case class Foo(i: Int, c: Option[Int]) extends Event
//  case class Bar(s: String) extends Event
//  case class Baz(c: Char) extends Event
//  case class Qux(values: List[String]) extends Event
//
//  println((Foo(100, None): Event).asJson.noSpaces)
//
//  private val jsonWithNull    = """{"i":100,"c":null,"what_am_i":"Foo"}"""
//  private val jsonWithoutNull = """{"i":100,"what_am_i":"Foo"}"""
//
//  val decodedFooWithNull = io.circe.parser.decode[Event](jsonWithNull)
//  println(decodedFooWithNull)
//
//  val decodedFooWithoutNull = io.circe.parser.decode[Event](jsonWithoutNull)
//  println(decodedFooWithoutNull)

//  import cats.effect._
//  import cats.syntax.all._
//  import scala.concurrent.duration._

//  timer.clock.realTime(MILLISECONDS).flatMap { start =>
//    task *> timer.clock.realTime(MILLISECONDS).flatMap { finish =>
//      val nextDelay = period.toMillis - (finish - start)
//      val nextDelay = period.toMillis - (finish - start)
//      timer.sleep(nextDelay.millis) *> repeatAtFixedRate(period, task)
//    }
//  }

//  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.Implicits.global)
//
//  implicit val ctx: ContextShift[IO] = IO.contextShift(ExecutionContext.Implicits.global)
//
//  def repeatAtFixedRate(period: FiniteDuration, task: IO[Unit])(implicit timer: Timer[IO]): IO[Unit] =
//    for {
//      start  <- timer.clock.realTime(MILLISECONDS)
//      _      <- task
//      finish <- timer.clock.realTime(MILLISECONDS)
//      nextDelay = period.toMillis - (finish - start)
//      _ <- timer.sleep(nextDelay.millis)
//      _ <- repeatAtFixedRate(period, task)
//    } yield ()
//
//  def printFoo: IO[Unit] = IO.delay(println(s"foo"))
//
//  def printRepeatedly: IO[Unit] = printFoo >> IO.sleep(1 seconds) >> IO.suspend(printRepeatedly)
//
//  val app = for {
//    _ <- IO.delay(println("Basic Setup..."))
//    _ <- printRepeatedly.start
//    _ <- IO.delay(println("Rest of the app..."))
//  } yield ()

//  app.unsafeRunSync()
//  repeatAtFixedRate(2.seconds, IO.delay(println("Hello motto"))).unsafeRunSync()

//  import fs2._
//  import cats.implicits._
//  import scala.language.postfixOps
//
//  (Stream.emit("Basic setup...").covary[IO].showLinesStdOut ++ Stream.emit("Rest of the app...").covary[IO].showLinesStdOut ++ Stream.sleep(
//    5 seconds
//  )).concurrently(Stream.awakeEvery[IO](1 second).flatMap(_ => Stream.emit("foo").covary[IO].showLinesStdOut)).compile.drain.unsafeRunSync

//  import fs2.Stream
//
//  Stream.repeatEval(IO(println(java.time.LocalTime.now))).metered(2.second).compile.drain.unsafeRunSync()
//  sealed trait Resource
//
//  case class Playlist(id: Int, userId: Int, songIds: List[Int]) extends Resource
//  case class Song(id: Int, artist: String, title: String) extends Resource
//  case class User(id: Int, name: String) extends Resource
//
//  case class Operation(id: Int, action: String, resource: String, metadata: Resource)
//
//  import io.circe.{ Decoder, Encoder }
//  import io.circe.parser._
//  import io.circe.syntax._
//  import io.circe.generic.auto._, io.circe.shapes._
//
//  object ShapesDerivation {
//    import shapeless.{ Coproduct, Generic }
//
//    implicit def encodeAdtNoDiscr[A, Repr <: Coproduct](
//        implicit
//        gen: Generic.Aux[A, Repr],
//        encodeRepr: Encoder[Repr]
//    ): Encoder[A] = encodeRepr.contramap(gen.to)
//
//    implicit def decodeAdtNoDiscr[A, Repr <: Coproduct](
//        implicit
//        gen: Generic.Aux[A, Repr],
//        decodeRepr: Decoder[Repr]
//    ): Decoder[A] = decodeRepr.map(gen.from)
//  }
//
//  import ShapesDerivation._
//
//  println(Operation(0, "add", "ayaya", Song(1, "Kenshi Yonezu", "Peace Sign")).asJson.spaces2)
//  private val operations: List[Operation] = List[Operation](Operation(0, "add", "ayaya", Song(1, "Kenshi Yonezu", "Peace Sign")))
//  private val spaces: String              = operations.asJson.spaces2
//  println(spaces)
//  println(io.circe.parser.decode[List[Operation]](spaces))
//  println(io.circe.parser.decode[Operation](spaces))

//}
