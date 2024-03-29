/*
 * Copyright Company
 */

package example.app.utils.api

import scala.concurrent.{ ExecutionContext, Future }

import io.circe.Decoder
import io.circe.parser._
import japgolly.scalajs.react.Callback
import org.scalajs.dom.XMLHttpRequest
import org.scalajs.dom.ext.{ Ajax => FutureAjax }

// scalastyle:off multiple.string.literals
trait Api {

  protected val ContentType           = "Content-Type"
  protected val ApplicationJson       = "application/json"
  protected val JsonContentTypeHeader = Map(ContentType -> ApplicationJson)

  protected val baseUrl: String

  protected def error(errorCode: Int, errorMessage: String) = Callback { (errorCode, errorMessage) }

  def futureGet[Response](url: String, successCode: Int = ApiStatusCodes.OK)(
      implicit decoder: Decoder[Response],
      ec: ExecutionContext
  ): Future[Response] =
    FutureAjax
      .get(
        s"$baseUrl/$url",
        headers = JsonContentTypeHeader
      )
      .flatMap(onCompleteFuture(successCode))

  protected def onCompleteFuture[Response](
      successCode: Int
  )(implicit decoder: Decoder[Response]): XMLHttpRequest => Future[Response] = res => {
    res.status match {
      case `successCode` =>
        Future.fromTry(decode[Response](res.responseText).toTry)
      case e: Int => Future.failed(new Exception(s"An Error occured: ${res.responseText}"))
    }
  }

  protected def onComplete[Response](
      successCode: Int
  )(success: Response => Callback)(implicit decoder: Decoder[Response]): XMLHttpRequest => Callback = res => {
    res.status match {
      case `successCode` =>
        decode[Response](res.responseText) match {
          case Right(response) => success(response)
          case Left(e) => error(ApiStatusCodes.INTERNAL_ERROR, e.getMessage)
        }
      case e: Int => error(e, res.responseText)
    }
  }
}

object ApiMethods {
  val GET    = "GET"
  val POST   = "POST"
  val PUT    = "PUT"
  val DELETE = "DELETE"
}

object ApiStatusCodes {
  val OK             = 200
  val CREATED        = 201
  val ACCEPTED       = 202
  val NOT_FOUND      = 404
  val CONFLICT       = 409
  val UNAUTHORIZED   = 401
  val INTERNAL_ERROR = 500
}
