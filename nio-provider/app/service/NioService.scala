package service

import org.apache.pekko.http.scaladsl.model.HttpMethods
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import configuration.{Env, NioConfig}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.{ExecutionContext, Future}

class NioService(env: Env, wSClient: WSClient)(
    implicit val executionContext: ExecutionContext) {

  val otoroshiHeaderClientId: String =
    env.config.filter.otoroshi.headerGatewayHeaderClientId
  val otoroshiHeaderClientSecret: String =
    env.config.filter.otoroshi.headerGatewayHeaderClientSecret

  val nio: NioConfig = env.config.nio

  def uploadFileToNio(tenant: String,
                      orgKey: String,
                      userId: String,
                      name: String,
                      src: Source[ByteString, ?],
                      contentTypeHeader: Option[String]): Future[JsValue] = {
    import play.api.libs.ws.bodyWritableOf_Multipart
    wSClient
      .url(
        s"${env.config.nio.url}/api/$tenant/organisations/$orgKey/users/$userId/_files/$name")
      .addHttpHeaders(
        otoroshiHeaderClientId -> nio.headerValueClientId,
        otoroshiHeaderClientSecret -> nio.headerValueClientSecret
      )
      .withBody(Source.single(FilePart(name, name, contentTypeHeader, src)))
      .withMethod(HttpMethods.POST.value)
      .execute()
      .map(resp => {
        resp.json
      })
  }
}
