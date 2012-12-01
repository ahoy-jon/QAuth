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



class Lauth extends unfiltered.filter.Plan {


  implicit val defaultTemplate = new TemplateEngine(new File("src/main/resources/templates") :: Nil , "dev")


  def jsonResponse[T](t:T)= JsonContent ~> ResponseString(CustomSerializer.generate(t))

  def intent = {
    case req @ GET(Path("/formAuth")) => Ok ~> Scalate(req, "formAuth.ssp")

    case GET(Path("/checkAuth")) => Ok ~> ResponseString("identified")

    case GET(Path("/qrcode.png")) => Ok ~> CharContentType("image/png") ~> new ResponseStreamer {
      def stream(os: OutputStream) {
        import SimpleQrcodeGenerator._
        writeImage2(os,"png", generateMatrix("ahoy", 400))
      }
    }

     /* def isKino[E](e:String => E) = watch((os:Option[String]) => {
        os.flatMap(s => findKino(s).headOption)
      },e)

      val expected = for (
        kino <- lookup("kinoid") is isKino(_ + " is not a kino") is required("missing");
        date <- lookup("date") is isDate(_ + " is not a date") is required("missing")
      ) yield {
        Ok ~> JsContent ~> ResponseString(CustomSerializer.generate(newKplaner(kino.get, date.get)))
      }

      expected(params) orFail (failures => {
        BadRequest ~> JsContent ~> ResponseString(CustomSerializer.generate(failures))
      })*/

/**
 *  Wanna try some HTML ? => https://github.com/unfiltered/unfiltered-scalate
 *  Just add
 */

    case Path("/help") => NotImplemented



    case req  => NotFound ~>  Scalate(req, "index.ssp")
  }
}

/** embedded server */
object Server {
  def main(args: Array[String]) {
    val http = unfiltered.jetty.Http(8080) // this will not be necessary in 0.4.0
    http.context("/assets") { _.resources(new java.net.URL(getClass().getResource("/www/"), ".")) }
      .filter(new Lauth).run({ svr =>
    {
      unfiltered.util.Browser.open(http.url)
    }
      }, { svr =>
        println("shutting down server")
      })
  }
}
