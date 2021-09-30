package models

import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import slick.model.ForeignKeyAction

import java.sql.Timestamp
import java.util.UUID

object Tables {

  //  getAll queries => Orders.result.statements.mkString

  lazy val Customers = TableQuery[CustomerTable]
  lazy val Products = TableQuery[ProductTable]
  lazy val Orders = TableQuery[OrderTable]
  lazy val OrderedItems = TableQuery[OrderedItemTable]

  case class Customer(customerId: java.util.UUID, fullName: String, email: String, phone: String, dateRegistered: Timestamp)

  case class Product(productId: java.util.UUID, productName: String, quantity: Int, price: Double)

  case class Order(orderId: Option[Int], customerId: java.util.UUID, dateOrdered: Timestamp, orderStatus: String)

  case class OrderedItem(itemId: java.util.UUID, orderId: Int, itemQty: Int, productId: java.util.UUID)

  class CustomerTable(tag: Tag) extends Table[Customer](tag, "customers") {
    val index1 = index("customers_email_key", email, unique = true)

    def * : ProvenShape[Customer] = (customerId, fullName, email, phone, dateRegistered) <> (Customer.tupled, Customer.unapply)

    def customerId: Rep[UUID] = column[java.util.UUID]("customer_id", O.PrimaryKey)

    def fullName: Rep[String] = column[String]("full_name")

    def email: Rep[String] = column[String]("email", O.Unique)

    def phone: Rep[String] = column[String]("phone")

    def dateRegistered: Rep[Timestamp] = column[Timestamp]("date_registered")
  }

  class ProductTable(tag: Tag) extends Table[Product](tag, "products") {
    def * : ProvenShape[Product] = (productId, productName, quantity, price) <> (Product.tupled, Product.unapply)

    def productId: Rep[UUID] = column[java.util.UUID]("product_id", O.PrimaryKey)

    def productName: Rep[String] = column[String]("product_name", O.Length(40, varying = true))

    def quantity: Rep[Int] = column[Int]("quantity")

    def price: Rep[Double] = column[Double]("price")
  }

  class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
    lazy val customersFk = foreignKey("orders_customerid_fkey", customerId, Customers)(r => r.customerId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * : ProvenShape[Order] = (orderId.?, customerId, dateOrdered, orderStatus) <> (Order.tupled, Order.unapply)

    def orderId: Rep[Int] = column[Int]("order_id", O.AutoInc, O.PrimaryKey)

    def customerId: Rep[UUID] = column[java.util.UUID]("customer_id")

    def dateOrdered: Rep[Timestamp] = column[Timestamp]("date_ordered")

    def orderStatus: Rep[String] = column[String]("order_status")
  }

  class OrderedItemTable(tag: Tag) extends Table[OrderedItem](tag, "ordered_items") {
    lazy val ordersFk = foreignKey("ordereditems_orderid_fkey", orderId, Orders)(r => r.orderId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * : ProvenShape[OrderedItem] = (itemId, orderId, itemQty, productId) <> (OrderedItem.tupled, OrderedItem.unapply)

    def itemId: Rep[UUID] = column[java.util.UUID]("item_id", O.PrimaryKey)

    def orderId: Rep[Int] = column[Int]("order_id")

    def itemQty: Rep[Int] = column[Int]("item_qty")

    def productId: Rep[UUID] = column[java.util.UUID]("product_id")
  }
}
