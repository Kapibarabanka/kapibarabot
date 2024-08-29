package com.kapibarabanka.kapibarabot.sqlite

import com.kapibarabanka.kapibarabot.sqlite.repos.*
import com.kapibarabanka.kapibarabot.sqlite.tables.*
import slick.jdbc.PostgresProfile.api.*

class Ao3Db(userId: String) extends WithDb(userId):
  val fics       = FicsRepo(userId)
  val characters = CharactersRepo(userId)

  val allTables: List[MyTable] = List(
    FicsTable,
    FandomsTable,
    FicsToFandomsTable,
    CharactersTable,
    FicsToCharactersTable,
    RelationshipsTable,
    ShipsToCharactersTable,
    FicsToShipsTable,
    TagsTable,
    FicsToTagsTable
  )

  def init = for {
    _ <- db(DBIO.sequence(allTables.map(_.createIfNotExists)))
  } yield ()

  def beginWithTestData = {
    for {
      _ <- db(DBIO.sequence(allTables.map(_.dropIfExists)))
      _ <- init
      _ <- fics.add(TestData.angstyZoSan)
      _ <- fics.add(TestData.friendly)
      _ <- fics.add(TestData.ratiorine)
    } yield ()
  }
