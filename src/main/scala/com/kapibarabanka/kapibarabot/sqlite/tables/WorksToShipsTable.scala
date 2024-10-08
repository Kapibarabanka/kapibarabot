package com.kapibarabanka.kapibarabot.sqlite.tables

import com.kapibarabanka.kapibarabot.sqlite.docs.WorksToShipsDoc
import com.kapibarabanka.kapibarabot.sqlite.services.KapibarabotDb
import slick.jdbc.PostgresProfile.api.*

class WorksToShipsTable(tag: Tag) extends Table[WorksToShipsDoc](tag, WorksToShipsTable.name):
  def id       = column[Int]("id", O.PrimaryKey, O.Unique)
  def workId    = column[String]("workId")
  def shipName = column[String]("shipName")

  def * = (id.?, workId, shipName).mapTo[WorksToShipsDoc]

object WorksToShipsTable extends MyTable:
  override val name: String     = "WorksToShips"
  override val keyField: String = "id"

  def createIfNotExists = KapibarabotDb.createManyToManyTableAction(
    name = name,
    leftFieldName = "workId",
    leftTable = WorksTable,
    rightFieldName = "shipName",
    rightTable = RelationshipsTable
  )
