package appear.dao

import appear.domain._
import java.sql._
import scala.Some
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import slick.jdbc.meta.MTable

class IssueDAO {

  private val db = Database.forURL(url = "jdbc:mysql://localhost:3306/appear", 
     user = "appear", password = "test", driver = "com.mysql.jdbc.Driver")  

  db.withSession {
    if (MTable.getTables("issues").list().isEmpty) {
      Issues.ddl.create
    }
  }

  def create(issue: Issue): Either[Failure, Issue] = {
    try {
      val id = db.withSession {
        Issues returning Issues.id insert issue
      }
      Right(issue.copy(id = Some(id)))
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

    def update(id: Long, issue: Issue): Either[Failure, Issue] = {
    try
      db.withSession {
        Issues.where(_.id === id) update issue.copy(id = Some(id)) match {
          case 0 => Left(notFoundError(id))
          case _ => Right(issue.copy(id = Some(id)))
        }
      }
    catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

   def delete(id: Long): Either[Failure, Issue] = {
    try {
      db.withTransaction {
        val query = Issues.where(_.id === id)
        val issues = query.run.asInstanceOf[Vector[Issue]]
        issues.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(issues.head)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  
  def get(id: Long): Either[Failure, Issue] = {
    try {
      db.withSession {
        Issues.findById(id).firstOption match {
          case Some(issue: Issue) =>
            Right(issue)
          case _ =>
            Left(notFoundError(id))
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

 
  def search(params: IssueSearchPar): Either[Failure, List[Issue]] = {
    implicit val typeMapper = Issues.dateTypeMapper

    try {
      db.withSession {
        val query = for {
          issue <- Issues if {
          Seq(
            params.reporter.map(issue.reporter is _)
          ).flatten match {
            case Nil => ConstColumn.TRUE
            case seq => seq.reduce(_ && _)
          }
        }
        } yield issue

        Right(query.run.toList)
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  
  protected def databaseError(e: SQLException) =
    Failure("%d: %s".format(e.getErrorCode, e.getMessage), FailureType.DatabaseFailure)

 
  protected def notFoundError(issueId: Long) =
    Failure("Issue with id=%d does not exist".format(issueId), FailureType.NotFound)
}