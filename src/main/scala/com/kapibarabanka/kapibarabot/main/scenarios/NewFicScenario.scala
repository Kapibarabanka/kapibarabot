package com.kapibarabanka.kapibarabot.main.scenarios

import com.kapibarabanka.airtable.AirtableError
import com.kapibarabanka.ao3scrapper.models.FicType
import com.kapibarabanka.ao3scrapper.{Ao3, Ao3Url}
import com.kapibarabanka.kapibarabot.domain.Fic
import com.kapibarabanka.kapibarabot.main.BotError.*
import com.kapibarabanka.kapibarabot.main.{BotApiWrapper, MessageData, WithErrorHandling}
import com.kapibarabanka.kapibarabot.persistence.AirtableClient
import com.kapibarabanka.kapibarabot.sqlite.FanficDb
import com.kapibarabanka.kapibarabot.utils.Buttons.getButtonsForNew
import com.kapibarabanka.kapibarabot.utils.{Buttons, MessageText}
import iozhik.OpenEnum
import scalaz.Scalaz.ToIdOps
import telegramium.bots.*
import zio.*

import scala.language.postfixOps

case class NewFicScenario(link: String)(implicit bot: BotApiWrapper, airtable: AirtableClient, ao3: Ao3, db: FanficDb)
    extends Scenario,
      WithErrorHandling(bot):

  protected override def startupAction: UIO[Unit] =
    bot.sendMessage(MessageData(MessageText.newFic(link), replyMarkup = getButtonsForNew)).unit

  override def onMessage(msg: Message): UIO[Scenario] = StartScenario().onMessage(msg)

  override def onCallbackQuery(query: CallbackQuery): UIO[Scenario] = {
    query.data match
      case Buttons.addToAirtable.callbackData => onSave(query)
      case _                                  => unknownCallbackQuery(query).map(_ => this)
  }

  private def onSave(query: CallbackQuery) = {
    query.message
      .collect { case startMsg: Message =>
        (for {
          _          <- bot.answerCallbackQuery(query, text = Some("Working on it..."))
          logParsing <- bot.editLogText(startMsg, "Parsing AO3...")
          ficLink <- startMsg.entities.collectFirst { case OpenEnum.Known(TextLinkMessageEntity(_, _, url)) => url } match
            case Some(value) => ZIO.succeed(value)
            case None        => ZIO.fail(NoLinkInMessage())
          fic          <- getFicByLink(ficLink)
          savingMsg    <- bot.editLogText(logParsing, "Saving to database...")
          fic          <- addFic(fic)
          _            <- bot.editLogText(savingMsg, "Enjoy:")
          nextScenario <- ExistingFicScenario(fic).withStartup
        } yield nextScenario) |> sendOnErrors({
          case ao3Error: Ao3Error           => s"getting fic from Ao3"
          case airtableError: AirtableError => s"adding fic to db"
        })
      }
      .getOrElse(ZIO.succeed(this))
  }

  private def getFicByLink(link: String): ZIO[Any, InvalidFicLink | Ao3Error, Fic] = Ao3Url.tryParseFicId(link) match
    case Some((FicType.Work, id)) => ao3.work(id).map(work => Fic.fromWork(work)).mapError(e => Ao3Error(e.getMessage))
    case Some((FicType.Series, id)) =>
      ao3.series(id).map(series => Fic.fromSeries(series)).mapError(e => Ao3Error(e.getMessage))
    case None => ZIO.fail(InvalidFicLink(link))
