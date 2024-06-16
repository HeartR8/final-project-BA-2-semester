package tickets.database

import cats.data.NonEmptyList
import tickets.models.Ticket

trait TicketDao[F[_]] {
  def add(Ticket: Ticket): F[Int]
  def get(id: Ticket.Id): F[Option[Ticket]]
  def getAll: F[NonEmptyList[Ticket]]
  def editPrice(id: Ticket.Id, price: Double): F[Int]
  def delete(id: Ticket.Id): F[Int]
}
