package com.my.nft_example.l0

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import com.my.nft_example.l0.custom_routes.CustomRoutes
import com.my.nft_example.shared_data.LifecycleSharedFunctions
import com.my.nft_example.shared_data.calculated_state.CalculatedState
import com.my.nft_example.shared_data.deserializers.Deserializers
import com.my.nft_example.shared_data.errors.Errors.valid
import com.my.nft_example.shared_data.serializers.Serializers
import com.my.nft_example.shared_data.types.Types.{NFTUpdate, NFTUpdatesCalculatedState, NFTUpdatesState}
import io.circe.{Decoder, Encoder}
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.BuildInfo
import org.tessellation.currency.dataApplication.dataApplication.{DataApplicationBlock, DataApplicationValidationErrorOr}
import org.tessellation.currency.dataApplication.{BaseDataApplicationL0Service, DataApplicationL0Service, DataState, DataUpdate, L0NodeContext}
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.currency.schema.currency
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.sdk.domain.rewards.Rewards
import org.tessellation.sdk.snapshot.currency.CurrencySnapshotEvent
import org.tessellation.security.SecurityProvider
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import java.util.UUID

object Main
  extends CurrencyL0App(
    "currency-l0",
    "currency L0 node",
    ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
    version = BuildInfo.version
  ) {
  def dataApplication: Option[BaseDataApplicationL0Service[IO]] =
    Option(BaseDataApplicationL0Service(new DataApplicationL0Service[IO, NFTUpdate, NFTUpdatesState, NFTUpdatesCalculatedState] {
      override def genesis: DataState[NFTUpdatesState, NFTUpdatesCalculatedState] =
        DataState(NFTUpdatesState(List.empty), NFTUpdatesCalculatedState(Map.empty))

      override def validateData(
        state  : DataState[NFTUpdatesState, NFTUpdatesCalculatedState],
        updates: NonEmptyList[Signed[NFTUpdate]]
      )(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] =
        LifecycleSharedFunctions.validateData[IO](state, updates)

      override def validateUpdate(
        update: NFTUpdate
      )(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] =
        valid.pure[IO]

      override def combine(
        state  : DataState[NFTUpdatesState, NFTUpdatesCalculatedState],
        updates: List[Signed[NFTUpdate]]
      )(implicit context: L0NodeContext[IO]): IO[DataState[NFTUpdatesState, NFTUpdatesCalculatedState]] =
        LifecycleSharedFunctions.combine[IO](state, updates)

      override def serializeState(
        state: NFTUpdatesState
      ): IO[Array[Byte]] =
        IO(Serializers.serializeState(state))

      override def serializeUpdate(
        update: NFTUpdate
      ): IO[Array[Byte]] =
        IO(Serializers.serializeUpdate(update))

      override def serializeBlock(
        block: Signed[DataApplicationBlock]
      ): IO[Array[Byte]] = IO(Serializers.serializeBlock(block)(dataEncoder.asInstanceOf[Encoder[DataUpdate]]))

      override def deserializeState(
        bytes: Array[Byte]
      ): IO[Either[Throwable, NFTUpdatesState]] =
        IO(Deserializers.deserializeState(bytes))

      override def deserializeUpdate(
        bytes: Array[Byte]
      ): IO[Either[Throwable, NFTUpdate]] =
        IO(Deserializers.deserializeUpdate(bytes))

      override def deserializeBlock(
        bytes: Array[Byte]
      ): IO[Either[Throwable, Signed[DataApplicationBlock]]] =
        IO(Deserializers.deserializeBlock(bytes)(dataDecoder.asInstanceOf[Decoder[DataUpdate]]))

      override def dataEncoder: Encoder[NFTUpdate] =
        implicitly[Encoder[NFTUpdate]]

      override def dataDecoder: Decoder[NFTUpdate] =
        implicitly[Decoder[NFTUpdate]]

      override def calculatedStateEncoder: Encoder[NFTUpdatesCalculatedState] =
        implicitly[Encoder[NFTUpdatesCalculatedState]]

      override def calculatedStateDecoder: Decoder[NFTUpdatesCalculatedState] =
        implicitly[Decoder[NFTUpdatesCalculatedState]]

      override def routes(implicit context: L0NodeContext[IO]): HttpRoutes[IO] =
        CustomRoutes[IO]().public

      override def signedDataEntityDecoder: EntityDecoder[IO, Signed[NFTUpdate]] =
        circeEntityDecoder

      override def getCalculatedState(implicit context: L0NodeContext[IO]): IO[(SnapshotOrdinal, NFTUpdatesCalculatedState)] =
        CalculatedState.getCalculatedState[IO]

      override def setCalculatedState(
        ordinal: SnapshotOrdinal,
        state  : NFTUpdatesCalculatedState
      )(implicit context: L0NodeContext[IO]): IO[Boolean] =
        CalculatedState.setCalculatedState[IO](ordinal, state)

      override def hashCalculatedState(
        state: NFTUpdatesCalculatedState
      )(implicit context: L0NodeContext[IO]): IO[Hash] =
        CalculatedState.hashCalculatedState[IO](state)
    }))

  def rewards(implicit sp: SecurityProvider[IO]): Option[Rewards[IO, currency.CurrencySnapshotStateProof, currency.CurrencyIncrementalSnapshot, CurrencySnapshotEvent]] =
    None
}
