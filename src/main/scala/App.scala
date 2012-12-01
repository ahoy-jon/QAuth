package lauth

import kinoplaner._
import unfiltered.request._
import unfiltered.response._

import unfiltered.response.Html
import unfiltered.response.ResponseString
import org.joda.time.format.ISODateTimeFormat

import common._
import org.fusesource.scalate.TemplateEngine
import java.io.{OutputStream, File}


import unfiltered.scalate._

import org.fusesource.scalate.{ TemplateEngine, Binding}
import unfiltered.request.QParams._
import unfiltered.response.CharContentType
import unfiltered.response.ResponseString
import lauth.model.{InMemoryServiceImpl, InMemoModel}
import lauth.model.InMemoModel.User

import util.{Properties, Random}
import java.net.NetworkInterface
import java.util.Collections
import java.lang.System._
import scala.Some
import unfiltered.response.CharContentType
import lauth.model.InMemoModel.User
import unfiltered.response.ResponseString


object IpAdresse {



  import scala.collection.JavaConversions._
  lazy val inter = List("en1", "wlan0","en0")

  lazy val interfaces  =  NetworkInterface.getNetworkInterfaces.toList


  lazy val nets =  NetworkInterface.getNetworkInterfaces.toList.filter(i => inter.contains(i.getName)).flatMap(_.getInetAddresses).filter(! _.isLoopbackAddress).sortBy(_.getHostAddress.size).head.getHostAddress


}


class Lauth(val url:String  =  "http://192.168.43.77:8080/") extends unfiltered.filter.Plan with InMemoModel with InMemoryServiceImpl {


  lazy val URL = "http://%s:8080/".format(IpAdresse.nets)

  implicit val defaultTemplate = new TemplateEngine(new File("src/main/resources/templates") :: Nil , "dev")


  def jsonResponse[T](t:T)= JsonContent ~> ResponseString(CustomSerializer.generate(t))


  def challengeParser[T](f: String => T) = for (challenge <- lookup("challenge") is required("missing")) yield {
    f(challenge.get)
  }

  val r=new Random()

  def intent = {
    case req @ GET(Path("/formAuth")) & Params(params) => {
      challengeParser(c =>  Ok ~> Scalate(req, "formAuth.ssp", ("challenge", c) ))(params) orFail (failures => {
        BadRequest ~>  ResponseString(CustomSerializer.generate(failures))
      })

    }

    case GET(Path("/checkAuth")) => Ok ~> ResponseString("identified")

    case req @ GET(Path("/app")) & Params(params) => {
      params.get("forcedChallenge") match  {
        case Some(s) => Ok ~> Scalate(req, "apli.ssp", ("forcedChallenge", s.mkString))
        case _ => Ok ~> Scalate(req, "apli.ssp")

      }
    }



    case GET(Path("/qrcode.png")) & Params(params) => {
      challengeParser(challenge => {
        Ok ~> CharContentType("image/png") ~> {

          val backGround:Int = params.get("backgroundcolor").map(_.mkString).map(s => Integer.parseInt(s, 16)).getOrElse(0xFFFFFF)
          val foreGround:Int = params.get("foregroundcolor").map(_.mkString).map(s => Integer.parseInt(s, 16)).getOrElse(0x3a4166)


          new ResponseStreamer {
            def stream(os: OutputStream) {
              FinalQrCodeGen.encodeData(URL + "ma?c=" + challenge, os, foreGround, backGround)
            }
          }
        }
      })(params) orFail (failures => {
        BadRequest
      })
    }


    case req@PUT(Path("/ma")) & Params(params) => {

      params.get("c").map(_.mkString) match {

        case Some(challenge) => {
          registerChallengeInProgress(challenge)
          Ok ~> ResponseString("ok chérie, je t'aime")
        }
        case _ => BadRequest
      }

    }
    case req@POST(Path("/ma")) & Params(params) => {

      params.get("c").map(_.mkString) match {

        case Some(challenge) => {
          val name = params.get("name").map(_.mkString).getOrElse("")
          val email = params.get("email").map(_.mkString).getOrElse("")
          registerUserForChallenge(challenge, User(name, email))
          Ok ~> ResponseString("ok chérie, je t'aime")

        }
        case _ => BadRequest
      }

    }

    case GET(Path("/checkPoll")) & Params(params) => {
      challengeParser(challenge => {
        val res = (userForChallenge(challenge), inProgressForChallenge(challenge)) match {
          case (Some(u), _) => Some(Map("user" -> u))
          case (_ , true ) => Some(Map("inprogress" -> true))
          case _ => None
        }
        res match {
          case Some(d) => Ok ~> jsonResponse(d)
          case None => NotFound ~> ResponseString("no content")
        }
      })(params) orFail (failures => {
        BadRequest ~> ResponseString("bad request is bad")
      })
    }

    case Path("/help") => NotImplemented



    case req  => NotFound ~>  Scalate(req, "index.ssp")
  }
}

/** embedded server */
object Server {
  def main(args: Array[String]) {
    val http = unfiltered.jetty.Http(8080) // this will not be necessary in 0.4.0
    http.context("/assets") { _.resources(new java.net.URL(getClass().getResource("/www/"), ".")) }.context("/prez") {
      _.resources(new java.net.URL(getClass().getResource("/prez/"), "."))
    }.filter(new Lauth).run
  }
}
