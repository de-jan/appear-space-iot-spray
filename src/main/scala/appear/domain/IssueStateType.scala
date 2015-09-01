package appear.domain

object IssueStateType extends Enumeration {

  type IssueState = Value

  val Created = Value("CREATED")
  val Started = Value("STARTED")
  val Done = Value("DONE")
}

