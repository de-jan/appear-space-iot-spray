package appear.domain

import scala.slick.driver.MySQLDriver.simple._

case class Issue(id: Option[Long], description: String, reporter: String, issueState: String, transionDate: Option[java.util.Date])

object Issues extends Table[Issue]("issues") {
  def id = column[Long]("id", O PrimaryKey, O AutoInc)
  def description= column[String]("description")
  def reporter = column[String]("reporter")
  def issueState = column[String]("issue_state", O Default("CREATED"))
  def transitionDate = column[java.util.Date]("transition_date", O NotNull, O Default(new java.util.Date), O DBType("timestamp")) 

  def * = id.? ~ description ~ reporter ~ issueState ~ transitionDate.? <>(Issue, Issue.unapply _)

  implicit val dateTypeMapper = MappedTypeMapper.base[java.util.Date, java.sql.Date](
  {
    ud => new java.sql.Date(ud.getTime)
  }, {
    sd => new java.util.Date(sd.getTime)
  })

  val findById = for {
    id <- Parameters[Long]
    c <- this if c.id is id
  } yield c
}
