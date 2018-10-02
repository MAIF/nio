package service

import akka.stream.scaladsl.Source
import akka.util.ByteString
import configuration.Env
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.FilePart

import scala.concurrent.{ExecutionContext, Future}

class NioService(env: Env, wSClient: WSClient)(
    implicit val executionContext: ExecutionContext) {

  def uploadFileToNio(tenant: String,
                      orgKey: String,
                      userId: String,
                      name: String,
                      src: Source[ByteString, _],
                      contentTypeHeader: Option[String]): Future[JsValue] = {

    wSClient
      .url(
        s"http://localhost:9000/api/$tenant/organisations/$orgKey/users/$userId/_files/$name")
      .post(Source.single(FilePart("name", name, contentTypeHeader, src)))
      .map(resp => {
        Logger.info(s"=========> ${resp.body}")
        resp.json
      })
  }
}
