package utils.schemas

/**
 * Enumeration representing telecommunication equipment types according to the HL7 v2 standard.
 * These codes identify the type of telecommunication device or address used for contact information,
 * commonly found in HL7 segments such as PID, NK1, or CTD.
 * PH - Telephone: Standard voice telephone number.
 * FX - Fax: Facsimile machine number.
 * MD - Modem: Modem connection line (used for data transmission).
 * CP - Cellular Phone: Mobile or cellular telephone number.
 * BP - Visto de Residência (Residence Visa): Administrative or identification code related to residence.
 * Internet - Internet Address: Use only if the Telecommunication Use Code is NET.
 * X400 - X.400 Email Address: Use only if the Telecommunication Use Code is NET.
 * TDD - Telecommunications Device for the Deaf: Device enabling text communication over phone lines for deaf users.
 * TTY - Teletypewriter: Text telephone for communication with hearing-impaired users.
 */

enum TelecomEquipmentType (val code: String):
  case PH extends TelecomEquipmentType("PH")
  case FX extends TelecomEquipmentType("FX")
  case MD extends TelecomEquipmentType("MD")
  case CP extends TelecomEquipmentType("CP")
  case BP extends TelecomEquipmentType("BP")
  case Internet extends TelecomEquipmentType("Internet")
  case X400 extends TelecomEquipmentType("X.400")
  case TDD extends TelecomEquipmentType("TDD")
  case TTY extends TelecomEquipmentType("TTY")

end TelecomEquipmentType
