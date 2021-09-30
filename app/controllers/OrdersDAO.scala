package controllers

import helpers.OrderStatus
import models.Tables.{Order, OrderTable, Orders}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, OFormat, Reads, Writes}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class OrdersDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
                         (implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  implicit val timestampReads: Reads[Timestamp] = implicitly[Reads[Long]].map(new Timestamp(_))
  implicit val timestampWrites: Writes[Timestamp] = implicitly[Writes[String]].contramap(_.getTime.toString)

  implicit val orderFormat: OFormat[Order] = Json.format[Order]

  import OrderStatus._

  implicit val userRoleMapper =
    MappedColumnType.base[OrderStatus, String](_.toString, OrderStatus.withName)

  def getOrders: Action[AnyContent] = Action {
    val query = TableQuery[OrderTable]
    val action = db.run(query.result)
    val queryResult = Await.result(action, 30.seconds)

    val toJson = Json.toJson(queryResult.sortBy(_.orderId))
    Ok(toJson)
  }

  def addOrder(customerId: String, orderStatus: String): Action[AnyContent] = Action {
    val inst: Instant = LocalDateTime.now()
      .atZone(ZoneId.of("GMT"))
      .toInstant

    val action = (Orders returning Orders.map(_.orderId)) += Order(None, UUID.fromString(customerId), Timestamp.from(inst), orderStatus)
    val res = Await.result(db.run(action), 30.seconds)

    val newEntryQuery = Orders.filter(_.orderId === res)
    val newEntryResult = newEntryQuery.result
    val newEntryActionResult = Await.result(db.run(newEntryResult), 30.seconds)

    Ok(Json.toJson(newEntryActionResult)).as("applcation/json")
  }

  def deleteOrder(orderId: Int): Action[AnyContent] = Action {
    val toBeDeleted = Orders.filter(_.orderId === orderId)
    val deleteAction = toBeDeleted.delete
    val affectedRowsAction: Future[Int] = db.run(deleteAction)
    val affectedRowsCount = Await.result(affectedRowsAction, 10.seconds)

    val resp1 = Ok(s"Success: Order $orderId has been deleted")
    val resp2 = Ok(s"Order does not exist")

    if (affectedRowsCount > 0) resp1 else resp2
  }

}
