package service

import akka.http.scaladsl.model._
import akka.http.scaladsl.util.FastFuture
import configuration.{Env, MailGunConfig}
import play.api.Logger
import play.api.libs.ws.{WSClient, _}

import scala.concurrent.{ExecutionContext, Future}

trait MailService {

  def sendDownloadedFile(to: String, downloadedFileUrl: String): Future[String]
}

class MailMockService(env: Env)(implicit val executionContext: ExecutionContext)
    extends MailService {
  override def sendDownloadedFile(to: String,
                                  downloadedFileUrl: String): Future[String] = {
    Logger.info(
      s"Mock send mail to $to with download file url $downloadedFileUrl")

    FastFuture.successful(downloadedFileUrl)
  }
}

class MailGunService(env: Env, wSClient: WSClient)(
    implicit val executionContext: ExecutionContext)
    extends MailService {

  val mailConfig: MailGunConfig = env.config.mailGunConfig

  override def sendDownloadedFile(to: String,
                                  downloadedFileUrl: String): Future[String] = {

    Logger.info(s"send mail to $to with download file url $downloadedFileUrl")

    wSClient
      .url(s"${mailConfig.endpoint}/messages")
      .withQueryStringParameters(
        ("from", mailConfig.from),
        ("to", to),
        ("subject", "Extraction de vos données personnelles"),
        ("text", s"Cliquez sur le lien suivant $downloadedFileUrl")
      )
      .withAuth("api", mailConfig.apiKey, WSAuthScheme.BASIC)
      .withMethod(HttpMethods.POST.value)
      .execute()
      .flatMap { res =>
        if (res.status == StatusCodes.OK.intValue) {
          FastFuture.successful(downloadedFileUrl)
        } else {
          Future.failed(new RuntimeException("error during mail sending"))
        }
      }
  }
}
