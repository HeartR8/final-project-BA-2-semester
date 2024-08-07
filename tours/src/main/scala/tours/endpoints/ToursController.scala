package tours.endpoints

import cats.MonadThrow
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import tours.models.{AddTour, Tour}
import tours.services.TourService
import tours.services.errors.ToursServiceError.errorMapper

final class ToursController[F[_]: MonadThrow](
    tourService: TourService[F]
) {

  private val baseRoute =
    endpoint
      .in("api" / "v1")
      .tag("Tours")
      .errorOut(errorMapper)

  private val addTourRoute = baseRoute
    .post
    .summary("Добавить тур в базу")
    .in("add")
    .in(jsonBody[AddTour])
    .out(jsonBody[Unit])
    .serverLogic(body => tourService.addTour(body))

  private val bookTourRoute = baseRoute
    .put
    .summary("Забронировать тур по id")
    .in("book")
    .in(jsonBody[Tour.Id])
    .out(jsonBody[Unit])
    .serverLogic(tourId => tourService.bookTour(tourId))

  val endpoints = List(
    addTourRoute,
    bookTourRoute
  )
}

object ToursController {
  def impl[F[_]: MonadThrow](
      tourService: TourService[F]
  ): ToursController[F] = {
    new ToursController[F](tourService)
  }
}
