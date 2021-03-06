// Copyright (c) 2017 PSForever
package net.psforever.objects.serverobject.structures

import java.util.concurrent.TimeUnit

import akka.actor.{ActorContext, ActorRef}
import net.psforever.objects.{GlobalDefinitions, Player}
import net.psforever.objects.definition.ObjectDefinition
import net.psforever.objects.serverobject.hackable.Hackable
import net.psforever.objects.serverobject.resourcesilo.ResourceSilo
import net.psforever.objects.serverobject.terminals.CaptureTerminal
import net.psforever.objects.serverobject.tube.SpawnTube
import net.psforever.objects.zones.Zone
import net.psforever.packet.game._
import net.psforever.types.{PlanetSideEmpire, Vector3}
import scalax.collection.{Graph, GraphEdge}

class Building(private val name: String,
               private val building_guid : Int,
               private val map_id : Int,
               private val zone : Zone,
               private val buildingType : StructureType.Value,
               private val buildingDefinition : ObjectDefinition) extends AmenityOwner {
  /**
    * The map_id is the identifier number used in BuildingInfoUpdateMessage. This is the index that the building appears in the MPO file starting from index 1
    * The GUID is the identifier number used in SetEmpireMessage / Facility hacking / PlanetSideAttributeMessage.
  */
  private var faction : PlanetSideEmpire.Value = PlanetSideEmpire.NEUTRAL
  private var playersInSOI : List[Player] = List.empty
  super.Zone_=(zone)

  GUID = PlanetSideGUID(building_guid)

  override def toString = name

  def Name : String = name

  def MapId : Int = map_id

  def Faction : PlanetSideEmpire.Value = faction

  override def Faction_=(fac : PlanetSideEmpire.Value) : PlanetSideEmpire.Value = {
    faction = fac
    TriggerZoneMapUpdate()
    Faction
  }

  def CaptureConsoleIsHacked : Boolean = {
    Amenities.find(x => x.Definition == GlobalDefinitions.capture_terminal).asInstanceOf[Option[CaptureTerminal]] match {
      case Some(obj: CaptureTerminal) =>
        obj.HackedBy.isDefined
      case None => false
    }
  }

  def PlayersInSOI : List[Player] = playersInSOI

  def PlayersInSOI_=(list : List[Player]) : List[Player] = {
    playersInSOI = list
    playersInSOI
  }

  // Get all lattice neighbours
  def Neighbours: Option[Set[Building]] = {
    zone.Lattice find this match {
      case Some(x) => Some(x.diSuccessors.map(x => x.toOuter))
      case None => None;
    }
  }

  def NtuLevel : Int = {
    //if we have a silo, get the NTU level
    Amenities.find(_.Definition == GlobalDefinitions.resource_silo) match {
      case Some(obj: ResourceSilo) =>
        obj.CapacitorDisplay.toInt
      case _ => //we have no silo; we have unlimited power
        10
    }
  }

  def TriggerZoneMapUpdate(): Unit = {
    if(Actor != ActorRef.noSender) Actor ! Building.TriggerZoneMapUpdate(Zone.Number)
  }

  // Get all lattice neighbours matching the specified faction
  def Neighbours(faction: PlanetSideEmpire.Value): Option[Set[Building]] = {
    this.Neighbours match {
      case Some(x: Set[Building]) =>
        val matching = x.filter(b => b.Faction == faction)
        if(matching.isEmpty) None else Some(matching)
      case None => None
    }
  }

  def Info : (
    Int,
      Boolean, PlanetSideEmpire.Value, Long, PlanetSideEmpire.Value,
      Long, Option[Additional1],
      PlanetSideGeneratorState.Value, Boolean, Boolean,
      Int, Int,
      List[Additional2], Long, Boolean,
      Int, Option[Additional3],
      Boolean, Boolean
    ) = {
    val ntuLevel : Int = NtuLevel
    //if we have a capture terminal, get the hack status & time (in milliseconds) from control console if it exists
    val (hacking, hackingFaction, hackTime) : (Boolean, PlanetSideEmpire.Value, Long) = Amenities.find(_.Definition == GlobalDefinitions.capture_terminal) match {
      case Some(obj: CaptureTerminal with Hackable) =>
        obj.HackedBy match {
          case Some(Hackable.HackInfo(_, _, hfaction, _, start, length)) =>
            val hack_time_remaining_ms = TimeUnit.MILLISECONDS.convert(math.max(0, start + length - System.nanoTime), TimeUnit.NANOSECONDS)
            (true, hfaction, hack_time_remaining_ms)
          case _ =>
            (false, PlanetSideEmpire.NEUTRAL, 0L)
        }
      case _ =>
        (false, PlanetSideEmpire.NEUTRAL, 0L)
    }
    //TODO if we have a generator, get the current repair state
    val (generatorState, bootGeneratorPain) = (PlanetSideGeneratorState.Normal, false)
    //if we have spawn tubes, determine if any of them are active
    val (spawnTubesNormal, boostSpawnPain) : (Boolean, Boolean) = {
      val o = Amenities.collect({ case _ : SpawnTube => true }) ///TODO obj.Health > 0
      if(o.nonEmpty) {
        (o.foldLeft(false)(_ || _), false) //TODO poll pain field strength
      }
      else {
        (true, false)
      }
    }

    val latticeBenefit : Int = {
      if(Faction == PlanetSideEmpire.NEUTRAL) 0
      else {
        def FindLatticeBenefit(wantedBenefit: ObjectDefinition, subGraph: Graph[Building, GraphEdge.UnDiEdge]): Boolean = {
          var found = false

          subGraph find this match {
            case Some(self) =>
              if (this.Definition == wantedBenefit) found = true
              else {
                self pathUntil (_.Definition == wantedBenefit) match {
                  case Some(_) => found = true
                  case None => ;
                }
              }
            case None => ;
          }

          found
        }

        // Check this Building is on the lattice first
        zone.Lattice find this match {
          case Some(_) =>
            // todo: generator destruction state
            val subGraph = Zone.Lattice filter ((b: Building) => b.Faction == this.Faction && !b.CaptureConsoleIsHacked && b.NtuLevel > 0)

            var stackedBenefit = 0
            if (FindLatticeBenefit(GlobalDefinitions.amp_station, subGraph)) stackedBenefit |= 1
            if (FindLatticeBenefit(GlobalDefinitions.comm_station_dsp, subGraph)) stackedBenefit |= 2
            if (FindLatticeBenefit(GlobalDefinitions.cryo_facility, subGraph)) stackedBenefit |= 4
            if (FindLatticeBenefit(GlobalDefinitions.comm_station, subGraph)) stackedBenefit |= 8
            if (FindLatticeBenefit(GlobalDefinitions.tech_plant, subGraph)) stackedBenefit |= 16

            stackedBenefit
          case None => 0;
        }
      }
    }
    //out
    (
      ntuLevel,
      hacking,
      hackingFaction,
      hackTime,
      if(ntuLevel > 0) Faction else PlanetSideEmpire.NEUTRAL,
      0, //!! Field != 0 will cause malformed packet. See class def.
      None,
      generatorState,
      spawnTubesNormal,
      false, //force_dome_active
      latticeBenefit,
      0, //cavern_benefit; !! Field > 0 will cause malformed packet. See class def.
      Nil,
      0,
      false,
      8, //!! Field != 8 will cause malformed packet. See class def.
      None,
      boostSpawnPain, //boost_spawn_pain
      bootGeneratorPain //boost_generator_pain
    )
  }

  def BuildingType : StructureType.Value = buildingType

  override def Zone_=(zone : Zone) : Zone = Zone //building never leaves zone after being set in constructor

  override def Continent : String = Zone.Id

  override def Continent_=(zone : String) : String = Continent //building never leaves zone after being set in constructor

  def Definition: ObjectDefinition = buildingDefinition
}

object Building {
  final val NoBuilding : Building = new Building(name = "", building_guid = 0, map_id = 0, Zone.Nowhere, StructureType.Platform, GlobalDefinitions.building) {
    override def Faction_=(faction : PlanetSideEmpire.Value) : PlanetSideEmpire.Value = PlanetSideEmpire.NEUTRAL
    override def Amenities_=(obj : Amenity) : List[Amenity] = Nil
  }

  def apply(name : String, guid : Int, map_id : Int, zone : Zone, buildingType : StructureType.Value) : Building = {
    new Building(name, guid, map_id, zone, buildingType, GlobalDefinitions.building)
  }

  def Structure(buildingType : StructureType.Value, location : Vector3, definition: ObjectDefinition)(name : String, guid : Int, map_id : Int, zone : Zone, context : ActorContext) : Building = {
    import akka.actor.Props
    val obj = new Building(name, guid, map_id, zone, buildingType, definition)
    obj.Position = location
    obj.Actor = context.actorOf(Props(classOf[BuildingControl], obj), s"$map_id-$buildingType-building")
    obj
  }

  def Structure(buildingType : StructureType.Value, location : Vector3)(name : String, guid : Int, map_id : Int, zone : Zone, context : ActorContext) : Building = {
    import akka.actor.Props

    val obj = new Building(name, guid, map_id, zone, buildingType, GlobalDefinitions.building)
    obj.Position = location
    obj.Actor = context.actorOf(Props(classOf[BuildingControl], obj), s"$map_id-$buildingType-building")
    obj
  }

  def Structure(buildingType : StructureType.Value)(name : String, guid: Int, map_id : Int, zone : Zone, context : ActorContext) : Building = {
    import akka.actor.Props
    val obj = new Building(name, guid, map_id, zone, buildingType, GlobalDefinitions.building)
    obj.Actor = context.actorOf(Props(classOf[BuildingControl], obj), s"$map_id-$buildingType-building")
    obj
  }

  def Structure(buildingType : StructureType.Value, buildingDefinition : ObjectDefinition, location : Vector3)(name: String, guid: Int, id : Int, zone : Zone, context : ActorContext) : Building = {
    import akka.actor.Props
    val obj = new Building(name, guid, id, zone, buildingType, buildingDefinition)
    obj.Position = location
    obj.Actor = context.actorOf(Props(classOf[BuildingControl], obj), s"$id-$buildingType-building")
    obj
  }

  final case class SendMapUpdate(all_clients: Boolean)
  final case class TriggerZoneMapUpdate(zone_num: Int)
}
