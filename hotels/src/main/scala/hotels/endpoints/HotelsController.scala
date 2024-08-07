package hotels.endpoints

import cats.MonadThrow
import cats.data.NonEmptyList
import hotels.models.Hotel
import hotels.services.HotelsService
import hotels.services.errors.HotelsServiceError.errorMapper
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

final class HotelsController[F[_]: MonadThrow](
    hotelsService: HotelsService[F]
) {

  private val baseRoute =
    endpoint
      .in("api" / "v1")
      .tag("Hotels")
      .errorOut(errorMapper)

  private val addHotelRoute = baseRoute
    .post
    .summary("Добавление отеля в базу")
    .in("add")
    .in(jsonBody[Hotel])
    .out(jsonBody[Unit])
    .serverLogic(body => hotelsService.addHotel(body))

  private val getHotelRoute = baseRoute
    .get
    .summary("Получение отеля по id")
    .in(path[Hotel.Id]("hotelId"))
    .out(jsonBody[Hotel])
    .serverLogic(id => hotelsService.getHotel(id))

  private val getHotelsRoute = baseRoute
    .get
    .summary("Получение списка всех доступных отелей")
    .in("all")
    .out(jsonBody[NonEmptyList[Hotel]])
    .serverLogic(_ => hotelsService.getHotels)

  private val editHotelDescriptionRoute = baseRoute
    .put
    .summary("Изменение описания отеля по id")
    .in(path[Hotel.Id]("id"))
    .in("description")
    .in(jsonBody[String])
    .out(jsonBody[Unit])
    .serverLogic { case (id, description) =>
      hotelsService.editHotelDescription(id, description)
    }

  private val deleteHotelRoute = baseRoute
    .delete
    .summary("Удаление отеля по id")
    .in(path[Hotel.Id]("id"))
    .out(jsonBody[Unit])
    .serverLogic(id => hotelsService.deleteHotel(id))

  val endpoints = List(
    addHotelRoute,
    getHotelsRoute,
    getHotelRoute,
    editHotelDescriptionRoute,
    deleteHotelRoute
  )
}

object HotelsController {
  def impl[F[_]: MonadThrow](
      hotelsService: HotelsService[F]
  ): HotelsController[F] = {
    new HotelsController[F](hotelsService)
  }
}
