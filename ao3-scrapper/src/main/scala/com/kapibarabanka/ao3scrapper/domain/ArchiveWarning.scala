package com.kapibarabanka.ao3scrapper.domain

case class ArchiveWarning(name: String) extends Tag:
  val category: TagCategory = TagCategory.Warning
