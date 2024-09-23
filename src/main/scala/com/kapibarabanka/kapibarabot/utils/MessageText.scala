package com.kapibarabanka.kapibarabot.utils

import com.kapibarabanka.kapibarabot.domain.*
import scalaz.Scalaz.ToIdOps

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object MessageText {
  def existingFic(model: FicDisplayModel): String =
    s"""
       |${info(model)}
       |${model |> displayMyRating}
       |${model |> displayStats}
       |""".stripMargin

  def newFic(link: String): String =
    s"""
       |<a href="$link">That's a new one!</a>
       |It's not in the database yet, but it could be.
       |""".stripMargin

  private def info(fic: FicDisplayModel) =
    s"""<b>${fic.title}</b>
       |<i>${fic.authors.mkString(", ")}</i>
       |
       |${fic.relationships.map(formatShip).mkString("\n")}
       |
       |${f"${fic.words}%,d"} words
       |""".stripMargin

  private def displayStats(fic: FicDisplayModel) =
    s"""${if (fic.stats.backlog) s"${Emoji.backlog} Is in backlog" else s"${Emoji.cross} Not in backlog"}
       |${if (fic.stats.isOnKindle) s"${Emoji.kindle} Is on Kindle" else s"${Emoji.cross} Not on Kindle"}
       |${readDates(fic)}
       |""".stripMargin

  private def displayMyRating(fic: FicDisplayModel) =
    (if (fic.stats.fire) s"${Emoji.fire}<b>It has fire!</b>${Emoji.fire}\n" else "")
      + fic.stats.quality.fold("")(q => s"You rated it ${formatQuality(q)}\n")
      + (if (fic.comments.isEmpty) ""
         else
           s"\nYour thoughts on it:\n<i>${fic.comments.map(_.format()).mkString("\n")}</i>")

  private def formatQuality(quality: Quality.Value) = quality match
    case Quality.Brilliant => s"<b>Brilliant</b> ${Emoji.brilliant}"
    case Quality.Nice      => s"<b>Nice</b> ${Emoji.nice}"
    case Quality.Ok        => s"<b>Ok</b> ${Emoji.ok}"
    case Quality.Meh       => s"<b>Meeeh</b> ${Emoji.meh}"
    case Quality.Never     => s"<b>Never</b> Again ${Emoji.never}"

  private def readDates(fic: FicDisplayModel) =
    fic.readDatesInfo.readDates match
      case List()            => if (fic.stats.read) s"${Emoji.finish} Already read" else s"${Emoji.cross} Not read"
      case List(Start(date)) => s"${Emoji.start} Started reading on $date"
      case dates =>
        s"${Emoji.finish} Already read:\n" + dates
          .map {
            case StartAndFinish(start, finish) if start == finish => s"   - on ${format(start)} (read in one day)"
            case StartAndFinish(start, finish)                    => s"   - from ${format(start)} to ${format(finish)}"
            case Start(date)                                      => s"   - started reading on ${format(date)}"
            case SingleDayRead(date)                              => s"   - on ${format(date)} (read in one day)"
          }
          .mkString("\n")

  private def format(isoDate: String) = LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd MMM uuuu"))

  private def formatShip(shipName: String) = shipName.replace("/", "  /  ").replace(" & ", "  &  ")
}
