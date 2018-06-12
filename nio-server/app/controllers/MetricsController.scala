package controllers

import java.io.StringWriter

import auth.AuthAction
import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import javax.inject.Inject
import play.api.mvc.ControllerComponents

import scala.concurrent.ExecutionContext

class MetricsController @Inject()(
    val AuthAction: AuthAction,
    metricRegistry: MetricRegistry,
    val cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends ControllerUtils(cc) {

  def metrics = AuthAction { req =>
    Ok(toJson())
  }

  val mapper = new ObjectMapper()

  def toJson(): String = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, metricRegistry)
    stringWriter.toString
  }

}
