package tickets.endpoints

import cats.MonadThrow
import cats.data.NonEmptyList
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import tickets.models.Ticket
import tickets.services.TicketsService
import tickets.services.errors.TicketsServiceError.errorMapper

final class TicketsController[F[_] : MonadThrow](
                                                 ticketsService: TicketsService[F]
                                               ) {

  private val baseRoute =
    endpoint
      .in("api" / "v1")
      .tag("Tickets")
      .errorOut(errorMapper)

  private val addTicketRoute = baseRoute
    .post
    .summary("Добавление билета в базу")
    .in("add")
    .in(jsonBody[Ticket])
    .out(jsonBody[Unit])
    .serverLogic(body => ticketsService.addTicket(body))

  private val getTicketRoute = baseRoute
    .get
    .summary("Получение билета по id")
    .in(path[Ticket.Id]("ticketId"))
    .out(jsonBody[Ticket])
    .serverLogic(id => ticketsService.getTicket(id))

  private val getTicketsRoute = baseRoute
    .get
    .summary("Получение списка всех доступных билетов")
    .in("all")
    .out(jsonBody[NonEmptyList[Ticket]])
    .serverLogic(_ => ticketsService.getTickets)

  private val editTicketDescriptionRoute = baseRoute
    .post
    .summary("Изменение цены билета по id")
    .in(path[Ticket.Id]("id"))
    .in("price")
    .in(jsonBody[Double])
    .out(jsonBody[Unit])
    .serverLogic { case (id, price) =>
      ticketsService.editTicketPrice(id, price)
    }

  private val deleteTicketRoute = baseRoute
    .delete
    .summary("Удаление билета по id")
    .in(path[Ticket.Id]("id"))
    .out(jsonBody[Unit])
    .serverLogic(id => ticketsService.deleteTicket(id))

  val endpoints = List(
    addTicketRoute,
    getTicketsRoute,
    getTicketRoute,
    editTicketDescriptionRoute,
    deleteTicketRoute
  )
}

object TicketsController {
  def impl[F[_] : MonadThrow](
                               ticketsService: TicketsService[F]
                             ): TicketsController[F] = {
    new TicketsController[F](ticketsService)
  }
}
