package hotels.services.errors

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody

sealed trait HotelsServiceError

object HotelsServiceError {
  val errorMapper: EndpointOutput.OneOf[HotelsServiceError, HotelsServiceError] = oneOf(
    oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequest]),
    oneOfVariant(StatusCode.Unauthorized, jsonBody[Unauthorized]),
    oneOfVariant(StatusCode.NotFound, jsonBody[NotFound]),
    oneOfDefaultVariant(statusCode(StatusCode.InternalServerError) and jsonBody[InternalError])
  )

  case class Unauthorized(msg: String) extends HotelsServiceError

  object Unauthorized {
    val default: Unauthorized = Unauthorized("Пользователь не авторизован")

    implicit val jsonEncoder: Encoder[Unauthorized] = deriveEncoder
    implicit val jsonDecoder: Decoder[Unauthorized] = deriveDecoder
  }

  case class NotFound(msg: String) extends HotelsServiceError

  object NotFound {
    val default: NotFound = NotFound("Пользователь не найден")

    implicit val jsonEncoder: Encoder[NotFound] = deriveEncoder
    implicit val jsonDecoder: Decoder[NotFound] = deriveDecoder
  }

  case class BadRequest(msg: String) extends HotelsServiceError

  object BadRequest {
    val default: BadRequest = BadRequest("Неверный формат запроса")

    implicit val jsonEncoder: Encoder[BadRequest] = deriveEncoder
    implicit val jsonDecoder: Decoder[BadRequest] = deriveDecoder
  }

  case class InternalError(msg: String) extends HotelsServiceError

  object InternalError {
    val default: InternalError = InternalError("Внутренняя ошибка сервера")

    implicit val jsonEncoder: Encoder[InternalError] = deriveEncoder
    implicit val jsonDecoder: Decoder[InternalError] = deriveDecoder
  }

  implicit val jsonEncoder: Encoder[HotelsServiceError] = deriveEncoder
  implicit val jsonDecoder: Decoder[HotelsServiceError] = deriveDecoder
}
