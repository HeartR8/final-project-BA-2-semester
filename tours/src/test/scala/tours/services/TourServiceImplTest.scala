package tours.services

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import doobie.implicits._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.MockitoSugar.mock
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}
import tours.database.{TourDao, TourDaoImpl}
import tours.models.{AddTour, Hotel, Ticket}

import java.util.UUID

class TourServiceImplTest extends AsyncWordSpec with Matchers with BeforeAndAfterAll with GivenWhenThen {
  trait mocks {
    val tourDao: TourDao[IO] = mock[TourDaoImpl[IO]]

    val service = new TourServiceImpl[IO](tourDao)

    val addTour = AddTour(
      hotelId = Hotel.Id.fromString(UUID.randomUUID().toString),
      ticketToId = Ticket.Id.fromString(UUID.randomUUID().toString),
      ticketFromId = Ticket.Id.fromString(UUID.randomUUID().toString)
    )
  }


  "addTour should work correctly if database doesn't answer with an error" in new mocks {
    when(tourDao.add(any())).thenReturn(1.pure[IO])

    service.addTour(addTour).unsafeRunSync shouldEqual Right(())

    verify(service).addTour(addTour)
  }
}
