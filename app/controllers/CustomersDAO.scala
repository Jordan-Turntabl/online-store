package controllers

import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json._
import play.api.mvc._
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.inject._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class CustomersDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
                            (implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  implicit val timestampReads: Reads[Timestamp] = implicitly[Reads[Long]].map(new Timestamp(_))
  implicit val timestampWrites: Writes[Timestamp] = implicitly[Writes[String]].contramap(_.getTime.toString)
  implicit val customerFormat: OFormat[Customer] = Json.format[Customer]

  def getCustomers: Action[AnyContent] = Action {
    val query = TableQuery[CustomerTable]
    val action = db.run(query.result)
    val queryResult = Await.result(action, 30.seconds)

    val toJson = Json.toJson(queryResult)
    Ok(toJson).as("applcation/json")
  }

  def addCustomer(fullName: String, email: String, phone: String): Action[AnyContent] = Action {
    val inst: Instant = LocalDateTime.now()
      .atZone(ZoneId.of("GMT"))
      .toInstant
    val action = Customers += Customer(UUID.randomUUID(), fullName, email, phone, Timestamp.from(inst))
    Await.result(db.run(action), 30.seconds)

    val newEntryQuery = Customers.filter(_.email === email)
    val newEntryResult = newEntryQuery.result
    val newEntryActionResult = Await.result(db.run(newEntryResult), 30.seconds)

    Ok(Json.toJson(newEntryActionResult)).as("applcation/json")
  }

  def deleteCustomer(customerId: String): Action[AnyContent] = Action {
    val toBeDeleted = Customers.filter(_.customerId === UUID.fromString(customerId))
    val deleteAction = toBeDeleted.delete
    val affectedRowsAction: Future[Int] = db.run(deleteAction)
    val affectedRowsCount = Await.result(affectedRowsAction, 30.seconds)

    val resp1 = Ok(s"Success: $customerId has been deleted")
    val resp2 = Ok(s"Customer does not exist")

    if (affectedRowsCount > 0) resp1 else resp2
  }
}
