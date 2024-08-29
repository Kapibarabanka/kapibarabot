package com.kapibarabanka.kapibarabot.sqlite.tables

import com.kapibarabanka.kapibarabot.sqlite.docs.TagDoc
import slick.jdbc.PostgresProfile.api.*

class TagsTable(tag: Tag) extends Table[TagDoc](tag, TagsTable.name):
  def name     = column[String]("name", O.PrimaryKey, O.Unique)
  def category = column[Option[String]]("category")

  def * = (name, category).mapTo[TagDoc]

object TagsTable extends MyTable:
  override val name: String     = "Tags"
  override val keyField: String = "name"

  def createIfNotExists = TableQuery[TagsTable].schema.createIfNotExists
