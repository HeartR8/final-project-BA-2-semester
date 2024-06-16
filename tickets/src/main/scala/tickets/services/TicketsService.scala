package tickets.services

import cats.data.NonEmptyList
import tickets.models.Ticket
import tickets.services.errors.TicketsServiceError

trait TicketsService[F[_]] {
  def addTicket(ticket: Ticket): F[Either[TicketsServiceError, Unit]]

  def getTickets: F[Either[
    TicketsServiceError,
    NonEmptyList[Ticket]
  ]]

  def getTicket(id: Ticket.Id): F[Either[TicketsServiceError, Ticket]]

  def editTicketPrice(
      id: Ticket.Id,
      price: Double
  ): F[Either[TicketsServiceError, Unit]]

  def deleteTicket(id: Ticket.Id): F[Either[TicketsServiceError, Unit]]
}
