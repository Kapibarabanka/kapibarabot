package com.kapibarabanka.kapibarabot.sqlite.tables

import com.kapibarabanka.kapibarabot.sqlite.docs.FicDetailsDoc
import slick.jdbc.PostgresProfile.api.*

class FicsDetailsTable(tag: Tag) extends Table[FicDetailsDoc](tag, FicsDetailsTable.name):
  def id          = column[Int]("id", O.PrimaryKey, O.Unique)
  def userId      = column[String]("userId")
  def ficId       = column[String]("ficId")
  def ficIsSeries = column[Boolean]("ficIsSeries")

  def read          = column[Boolean]("read")
  def backlog       = column[Boolean]("backlog")
  def isOnKindle    = column[Boolean]("isOnKindle")
  def quality       = column[Option[String]]("quality")
  def fire          = column[Boolean]("fire")
  def recordCreated = column[String]("recordCreated")

  def * = (id.?, userId, ficId, ficIsSeries, read, backlog, isOnKindle, quality, fire, recordCreated).mapTo[FicDetailsDoc]

object FicsDetailsTable extends MyTable:
  override val name: String     = "FicsDetails"
  override val keyField: String = "id"

  def createIfNotExists = TableQuery[FicsDetailsTable].schema.createIfNotExists
