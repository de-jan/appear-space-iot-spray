package appear.domain

import java.util.Date

case class IssueSearchPar( reporter: Option[String] = None,
    issueState: Option[String]= None )