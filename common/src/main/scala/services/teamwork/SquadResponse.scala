// Copyright (c) 2019 PSForever
package services.teamwork

import net.psforever.objects.teamwork.Squad
import net.psforever.packet.game._
import net.psforever.types.SquadResponseType

object SquadResponse {
  trait Response

  final case class ListSquadFavorite(line : Int, task : String) extends Response

  final case class InitList(info : Vector[SquadInfo]) extends Response
  final case class UpdateList(infos : Iterable[(Int, SquadInfo)]) extends Response
  final case class RemoveFromList(infos : Iterable[Int]) extends Response

  final case class InitSquad(squad_guid : PlanetSideGUID) extends Response
  final case class Unknown17(squad : Squad, char_id : Long) extends Response
  final case class Membership(request_type : SquadResponseType.Value, unk1 : Int, unk2 : Int, unk3 : Long, unk4 : Option[Long], player_name : String, unk5 : Boolean, unk6 : Option[Option[String]]) extends Response //see SquadMembershipResponse
  final case class Invite(from_char_id : Long, to_char_id : Long, name : String) extends Response
  final case class Join(squad : Squad, positionsToUpdate : List[Int]) extends Response
  final case class Leave(squad : Squad, positionsToUpdate : List[(Long, Int)]) extends Response
  final case class UpdateMembers(squad : Squad, update_info : List[SquadAction.Update]) extends Response
  final case class AssignMember(squad : Squad, from_index : Int, to_index : Int) extends Response

  final case class Detail(guid : PlanetSideGUID, squad_detail : SquadDetail) extends Response
}
