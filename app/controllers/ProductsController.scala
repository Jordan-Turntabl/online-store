package controllers

import models.Tables.{Product, ProductTable, Products}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import slick.jdbc.JdbcProfile

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class ProductsController @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, controllerComponents: ControllerComponents)
                                  (implicit ec: ExecutionContext)
  extends AbstractController(controllerComponents) with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  implicit val customerJson: OFormat[Product] = Json.format[Product]

  def getProducts: Action[AnyContent] = Action {
    val query = TableQuery[ProductTable]
    val action = db.run(query.result)
    val queryResult = Await.result(action, 30.seconds)

    val toJson = Json.toJson(queryResult)
    Ok(toJson)
  }

  def addProduct(productName: String, quantity: Int, price: Double): Action[AnyContent] = Action {
    val uuid = UUID.randomUUID()
    val action = Products += Product(uuid, productName, quantity, price)
    Await.result(db.run(action), 30.seconds)

    val newEntryQuery = Products.filter(_.productId === uuid)
    val newEntryResult = newEntryQuery.result

    val newEntryActionResult = Await.result(db.run(newEntryResult), 30.seconds)
    Ok(Json.toJson(newEntryActionResult)).as("applcation/json")
  }

  def deleteProduct(productId: String): Action[AnyContent] = Action {
    val toBeDeleted = Products.filter(_.productId === UUID.fromString(productId))
    val deleteAction = toBeDeleted.delete
    val affectedRowsAction: Future[Int] = db.run(deleteAction)
    val affectedRowsCount = Await.result(affectedRowsAction, 10.seconds)

    val resp1 = Ok(s"Success: $productId has been deleted")
    val resp2 = Ok(s"Product does not exist")

    if (affectedRowsCount > 0) resp1 else resp2
  }
}
