package tickets

import cats.effect.{IO, IOApp}
object Main extends IOApp.Simple {
  override def run: IO[Unit] = TicketsApp.run
}
