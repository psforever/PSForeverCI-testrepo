// Copyright (c) 2019 PSForever
package net.psforever.objects.teamwork

import net.psforever.types.{CertificationType, Vector3}

class Member {
  //about the position to be filled
  private var role : String = ""
  private var orders : String = ""
  private var restrictions : Set[CertificationType.Value] = Set()
  //about the individual filling the position
  private var name : String = ""
  private var health : Int = 0
  private var armor : Int = 0
  private var zoneId : Int = 0
  private var position : Vector3 = Vector3.Zero

  def Role : String = role

  def Role_=(title : String) : String = {
    role = title
    Role
  }

  def Orders : String = orders

  def Orders_=(text : String) : String = {
    orders = text
    Orders
  }

  def Restrictions : Set[CertificationType.Value] = restrictions

  def Restrictions_=(requirements : Set[CertificationType.Value]) = {
    restrictions = requirements
    Restrictions
  }

  def Name : String = name

  def Name_=(moniker : String) : String = {
    name = moniker
    Name
  }

  def Health : Int = health

  def Health_=(red : Int) : Int = {
    health = red
    Health
  }

  def Armor : Int = armor

  def Armor_=(blue : Int) : Int = {
    armor = blue
    Armor
  }

  def ZoneId : Int = zoneId

  def ZoneId_=(id : Int) : Int = {
    zoneId = id
    ZoneId
  }

  def Position : Vector3 = position

  def Position_=(pos : Vector3) : Vector3 = {
    position = pos
    Position
  }

  def Close() : Unit = {
    role = ""
    restrictions = Set()
    //about the individual filling the position
    name = ""
    health = 0
    armor = 0
    zoneId = 0
    position = Vector3.Zero
  }
}
