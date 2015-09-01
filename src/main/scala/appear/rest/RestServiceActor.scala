package appear.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import appear.dao.IssueDAO
import appear.domain._
import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import net.liftweb.json.Serialization._
import net.liftweb.json.{DateFormat, Formats}
import scala.Some
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._


class RestServiceActor extends Actor with RestService {

  implicit def actorRefFactory = context

  def receive = runRoute(rest)
}

trait RestService extends HttpService with SLF4JLogging {

  val issueService = new IssueDAO

  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss")

      def parse(s: String): Option[Date] = try {
        Some(sdf.parse(s))
      } catch {
        case e: Exception => None
      }

      def format(d: Date): String = sdf.format(d)
    }
  }

  implicit val string2Date = new FromStringDeserializer[Date] {
    def apply(value: String) = {
      val sdf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss")
      try Right(sdf.parse(value))
      catch {
        case e: ParseException => {
          Left(MalformedContent("'%s' is not a valid Date value" format (value), e))
        }
      }
    }
  }

  implicit val customRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
         response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          write(Map("error" -> response.entity.asString))))
    } {
      RejectionHandler.Default(rejections)
    }
  }

  val rest = respondWithMediaType(MediaTypes.`application/json`) {
    path("issue") {
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[Issue](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          issue: Issue =>
            ctx: RequestContext =>
              handleRequest(ctx, StatusCodes.Created) {
                log.debug("Creating issue: %s".format(issue))
                issueService.create(issue)
              }
        }
      } ~
        get {
          parameters( 'reporter.as[String] ?, 'issueState.as[String] ? ).as(IssueSearchPar) {
            searchParameters: IssueSearchPar => {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Searching for issues with parameters: %s".format(searchParameters))
                  issueService.search(searchParameters)
                }
            }
          }
        }
    } ~
      path("issue" / LongNumber) {
        issueId =>
          put {
            entity(Unmarshaller(MediaTypes.`application/json`) {
              case httpEntity: HttpEntity =>
                read[Issue](httpEntity.asString(HttpCharsets.`UTF-8`))
            }) {
              issue: Issue =>
                ctx: RequestContext =>
                  handleRequest(ctx) {
                    log.debug("Updating issue with id %d: %s".format(issueId, issue))
                    issueService.update(issueId, issue)
                  }
            }
          } ~
            delete {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Deleting issue with id %d".format(issueId))
                  issueService.delete(issueId)
                }
            } ~
            get {
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Retrieving issue with id %d".format(issueId))
                  issueService.get(issueId)
                }
            }
      }   
  }

 
  protected def handleRequest(ctx: RequestContext, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {
    action match {
      case Right(result: Object) =>
        ctx.complete(successCode, write(result))
      case Left(error: Failure) =>
        ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
      case _ =>
        ctx.complete(StatusCodes.InternalServerError)
    }
  }
}