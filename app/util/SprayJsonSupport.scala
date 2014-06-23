package util

import csw.util.cfg.ConfigJsonFormats
import csw.util.cfg.Configurations.SetupConfigList
import models.Assembly1Settings
import models.Assembly1Settings.{TelescopePos, BasePos}
import spray.json._

/**
 * Support for Spray JSON calls where the imports conflict with Play's JSON names (JsValue, etc.)
 */
object SprayJsonSupport extends ConfigJsonFormats{

  def setupConfigListToJson(scl: SetupConfigList): String = scl.toJson.toString()

  def jsonToSetupConfigList(json: String): SetupConfigList = json.parseJson.convertTo[SetupConfigList]

  def setupConfigListToAssembly1Settings(scl: SetupConfigList): Assembly1Settings = {
    // XXX assumes order in SetupConfigList
    val bp = scl(0)
    val tp = scl(1)
    Assembly1Settings(
      BasePos(
        bp("posName").elems.head.asInstanceOf[String],
        bp("c1").elems.head.asInstanceOf[String],
        bp("c2").elems.head.asInstanceOf[String],
        bp("equinox").elems.head.asInstanceOf[String]
      ),
      TelescopePos(
        tp("c1").elems.head.asInstanceOf[String],
        tp("c2").elems.head.asInstanceOf[String],
        tp("equinox").elems.head.asInstanceOf[String]
      ))
  }
}
