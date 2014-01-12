package models

import scala.collection.JavaConversions._

import scala.language.implicitConversions
import com.datastax.driver.core.{DataType, Row, ResultSet}
import play.api.libs.json._
import java.math.BigInteger
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber

object Utils {
  def resultToJson(result: ResultSet): JsArray = {
    var response = Json.arr()

    result.iterator() map rowToJson foreach {
      jsRow =>
        response = response :+ jsRow
    }
    response
  }

  private def rowToJson(row: Row): JsObject = {
    var responseRow = Json.obj()
    val columnDefinitions = row.getColumnDefinitions
    columnDefinitions foreach {
      definition =>
        import DataType.Name._
        import java.math.{BigDecimal => BigDec}
        implicit def bigInteger2bigDecimal(b: BigInteger) = new BigDecimal(new BigDec(b))
        definition.getType.getName match {

          case ASCII | VARCHAR | TEXT => responseRow = responseRow + (definition.getName -> JsString(row.getString(definition.getName)))

          case BIGINT | DataType.Name.COUNTER => responseRow = responseRow + (definition.getName -> JsNumber(row.getLong(definition.getName)))

          case INT => responseRow = responseRow + (definition.getName -> JsNumber(row.getInt(definition.getName)))

          case BLOB => responseRow = responseRow + (definition.getName -> JsString("SPECIAL:BLOB"))

          case BOOLEAN => responseRow = responseRow + (definition.getName -> JsBoolean(row.getBool(definition.getName)))

          case DECIMAL => responseRow = responseRow + (definition.getName -> JsNumber(row.getDecimal(definition.getName)))

          case DOUBLE => responseRow = responseRow + (definition.getName -> JsNumber(row.getDouble(definition.getName)))

          case FLOAT => responseRow = responseRow + (definition.getName -> JsNumber(row.getFloat(definition.getName)))

          case INET => responseRow = responseRow + (definition.getName -> JsString(row.getInet(definition.getName).toString))

          case TIMESTAMP => responseRow = responseRow + (definition.getName -> JsString(row.getDate(definition.getName).toString))

          case UUID => responseRow = responseRow + (definition.getName -> JsString(row.getUUID(definition.getName).toString))

          case VARINT => responseRow = responseRow + (definition.getName -> JsNumber(row.getVarint(definition.getName)))

          case TIMEUUID => responseRow = responseRow + (definition.getName -> JsString(row.getUUID(definition.getName).toString))

          case LIST => responseRow = responseRow + (definition.getName -> JsString(row.getList(definition.getName, definition.getType.getTypeArguments.head.asJavaClass()).toString))

          case SET => responseRow = responseRow + (definition.getName -> JsString(row.getSet(definition.getName, definition.getType.getTypeArguments.head.asJavaClass()).toString))

          case MAP => responseRow = responseRow + (definition.getName -> JsString(row.getMap(definition.getName, definition.getType.getTypeArguments.head.asJavaClass(), definition.getType.getTypeArguments.last.asJavaClass()).toString))

          case CUSTOM => responseRow = responseRow + (definition.getName -> JsString("[[%s]]".format(definition.getType.getCustomTypeClassName))) //TODO: Read the byteBuffer and try to serialize if it has implicit bb => cc
        }
    }
    responseRow

  }

}
