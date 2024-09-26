package com.kapibarabanka.kapibarabot.main.scenarios

import com.kapibarabanka.ao3scrapper.models.FicType
import com.kapibarabanka.kapibarabot.domain.{FicDetails, Quality, UserFicRecord}
import com.kapibarabanka.kapibarabot.main.BotError.*
import com.kapibarabanka.kapibarabot.main.MessageData
import com.kapibarabanka.kapibarabot.services.{BotWithChatId, DbService}
import com.kapibarabanka.kapibarabot.utils.Buttons.*
import com.kapibarabanka.kapibarabot.utils.{Buttons, MessageText}
import scalaz.Scalaz.ToIdOps
import telegramium.bots.*
import zio.*

import java.time.LocalDate

case class ExistingFicStateProcessor(currentState: ExistingFicBotState, bot: BotWithChatId,  db: DbService)
  extends StateProcessor(currentState, bot),
      WithErrorHandling(bot):
  private val record = currentState.displayedFic

  override def startup: UIO[Unit] =
    bot.sendMessage(MessageData(MessageText.existingFic(record), getButtonsForExisting(record))).unit

  override def onMessage(msg: Message): UIO[BotState] = StartStateProcessor(StartBotState(), bot, db).onMessage(msg)

  override def onCallbackQuery(query: CallbackQuery): UIO[BotState] =
    query.data match
      case Buttons.addToBacklog.callbackData      => patchStats(record.details.copy(backlog = true), query)
      case Buttons.removeFromBacklog.callbackData => patchStats(record.details.copy(backlog = false), query)

      case Buttons.markAsRead.callbackData          => patchStats(record.details.copy(read = true), query)
      case Buttons.markAsStartedToday.callbackData  => patchDates(db.details.addStartDate(_, LocalDate.now().toString))(query)
      case Buttons.markAsFinishedToday.callbackData => patchDates(db.details.addFinishDate(_, LocalDate.now().toString))(query)
      case Buttons.cancelStartedToday.callbackData  => patchDates(db.details.cancelStartedToday)(query)
      case Buttons.cancelFinishedToday.callbackData => patchDates(db.details.cancelFinishedToday)(query)

      case Buttons.rateNever.callbackData     => patchStats(record.details.copy(quality = Some(Quality.Never)), query)
      case Buttons.rateMeh.callbackData       => patchStats(record.details.copy(quality = Some(Quality.Meh)), query)
      case Buttons.rateOk.callbackData        => patchStats(record.details.copy(quality = Some(Quality.Ok)), query)
      case Buttons.rateNice.callbackData      => patchStats(record.details.copy(quality = Some(Quality.Nice)), query)
      case Buttons.rateBrilliant.callbackData => patchStats(record.details.copy(quality = Some(Quality.Brilliant)), query)

      case Buttons.rateFire.callbackData    => patchStats(record.details.copy(fire = true), query)
      case Buttons.rateNotFire.callbackData => patchStats(record.details.copy(fire = false), query)

      case Buttons.addComment.callbackData => bot.answerCallbackQuery(query).map(_ => CommentBotState(record))

      case Buttons.sendToKindle.callbackData =>
        record.fic.ficType match
          case FicType.Work => bot.answerCallbackQuery(query).map(_ => SendToKindleBotState(record))
          case FicType.Series =>
            bot
              .answerCallbackQuery(query, text = Some("Sorry, can't send series to Kindle yet, please send each work separately"))
              .map(_ => currentState)

      case _ => unknownCallbackQuery(query).map(_ => currentState)

  private def patchStats(newStats: FicDetails, query: CallbackQuery) =
    patchFic(s"patching fic with id ${record.key}")(db.details.patchFicStats(_, newStats))(query)

  private def patchDates(patch: UserFicRecord => IO[Throwable, UserFicRecord])(query: CallbackQuery) =
    patchFic(s"updating fic ${record.key} read dates")(patch)(query)

  private def patchFic(actionName: String)(patch: UserFicRecord => IO[Throwable, UserFicRecord])(query: CallbackQuery) =
    query.message
      .collect { case msg: Message =>
        (for {
          patchedFic <- patch(record)
          msgData <- ZIO.succeed(
            MessageData(MessageText.existingFic(patchedFic), getButtonsForExisting(patchedFic))
          )
          _ <- bot.answerCallbackQuery(query)
          _ <- bot.editMessage(msg, msgData)
        } yield ExistingFicBotState(patchedFic, false)) |> sendOnError(actionName)
      }
      .getOrElse(ZIO.succeed(currentState))
