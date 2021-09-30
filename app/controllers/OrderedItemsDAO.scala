package controllers

import models.Tables.{OrderedItem, OrderedItemTable, OrderedItems}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class OrderedItemsDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, cc: ControllerComponents)
                               (implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  val orderedItemTable = OrderedItems

  implicit val customerJson: OFormat[OrderedItem] = Json.format[OrderedItem]

  def getOrderedItems: Action[AnyContent] = Action {
    val query = TableQuery[OrderedItemTable]
    val action = db.run(query.result)
    val queryResult = Await.result(action, 30.seconds)

    val toJson = Json.toJson(queryResult)
    Ok(toJson)
  }

  def addOrderedItem(orderId: Int, itemQty: Int, productId: String): Action[AnyContent] = Action {
    val action = OrderedItems += OrderedItem(UUID.randomUUID(), orderId, itemQty, UUID.fromString(productId))
    Await.result(db.run(action), 30.seconds)

    val newEntry = OrderedItems.filter(_.orderId === orderId)
    val newEntryResult = newEntry.result
    val newEntryActionResult = Await.result(db.run(newEntryResult), 30.seconds)

    Ok(Json.toJson(newEntryActionResult)).as("applcation/json")
  }

  def deleteOrderedItem(itemId: String): Action[AnyContent] = Action {
    val toBeDeleted = orderedItemTable.filter(_.itemId === UUID.fromString(itemId))
    val deleteAction = toBeDeleted.delete
    val affectedRowsAction: Future[Int] = db.run(deleteAction)
    val affectedRowsCount = Await.result(affectedRowsAction, 30.seconds)

    val resp1 = Ok(s"Success: $itemId has been deleted")
    val resp2 = Ok(s"Item does not exist")

    if (affectedRowsCount > 0) resp1 else resp2
  }

}
