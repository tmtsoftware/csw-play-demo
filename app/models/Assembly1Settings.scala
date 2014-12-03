package models

import util.SprayJsonSupport
import csw.util.cfg.Configurations.{SetupConfig, SetupConfigList}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import Assembly1Settings._


object Assembly1Settings {

  // Some telescope position
  case class TelescopePos(c1: String, c2: String, equinox: String)

  // The base position info
  case class BasePos(posName: String, c1: String, c2: String, equinox: String)

  // The response from the command server for a submit
  case class SubmitResponse(runId: String)

  // The response from the command server for a status query
  case class StatusResponse(name: String, runId: String, message: String)

  def defaultSettings = Assembly1Settings(
    BasePos("", "00:00:00", "00:00:00", "J2000"),
    TelescopePos("00:00:00", "00:00:00", "J2000"))


  // Conversion from JSON to BasePos
  implicit val basePosReads = (
    (__ \ "posName").read[String] ~
      (__ \ "c1").read[String] ~
      (__ \ "c2").read[String] ~
      (__ \ "equinox").read[String]
    )(BasePos)

  // Conversion from JSON to TelescopePos
  implicit val telescopePosReads = (
    (__ \ "c1").read[String] ~
      (__ \ "c2").read[String] ~
      (__ \ "equinox").read[String]
    )(TelescopePos)


  // JSON formats
  implicit val basePosFormat = Json.format[BasePos]
  implicit val telescopePosFormat = Json.format[TelescopePos]
  implicit val submitResponseFormat = Json.format[SubmitResponse]
  implicit val statusResponseFormat = Json.format[StatusResponse]

  // Returns an Assembly1Settings object for the given JSON, wrapped in a JsResult
  def fromJson(json: JsValue): JsResult[Assembly1Settings] = {
//    for {
//      basePos <- Json.fromJson[BasePos](json \ "[0]" \ "setup" \ "tmt.tel.base.pos" )
//      telescopePos <- Json.fromJson[TelescopePos](json \ "[1]" \ "setup" \ "tmt.tel.ao.pos.one")
//    } yield Assembly1Settings(basePos, telescopePos)

    val scl = SprayJsonSupport.jsonToSetupConfigList(json.toString())
    JsSuccess(SprayJsonSupport.setupConfigListToAssembly1Settings(scl))
  }
}

// Corresponds to the form that is displayed for editing
case class Assembly1Settings(basePos: BasePos, aoPos: TelescopePos) {
  val obsId = "obsId0001" // XXX TODO FIXME: need to keep track of obsId
  def getConfig: SetupConfigList = List(
      SetupConfig(
        obsId = obsId,
        "tmt.tel.base.pos",
        "posName" -> basePos.posName,
        "c1" -> basePos.c1,
        "c2" -> basePos.c2,
        "equinox" -> basePos.equinox
      ),
      SetupConfig(
        obsId = obsId,
        "tmt.tel.base.pos",
        "c1" -> aoPos.c1,
        "c2" -> aoPos.c2,
        "equinox" -> aoPos.equinox
      )
  )
}


