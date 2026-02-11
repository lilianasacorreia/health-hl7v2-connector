package utils.schemas

/**
 * Enumeration representing the HL7 v2 Telecommunication Use Code system.
 * These codes specify the use or purpose of a telecommunication address (e.g., home, work, email),
 * typically used in HL7 segments such as PID, NK1, or CTD to describe how a contact point is used.
 * PRN - Primary Residence Number: The primary home telephone number of the individual.
 * ORN - Other Residence Number: An additional or secondary home telephone number.
 * WPN - Work Number: A work or office telephone number.
 * VHN - Vacation Home Number: A telephone number at a vacation or temporary residence.
 * ASN - Answering Service Number: A number associated with an answering service.
 * EMR - Emergency Number: A telephone number used for emergencies.
 * NET - Network (email) Address: An electronic mail address or internet contact.
 * BPN - Beeper Number: A pager or beeper contact number.
 */

enum TelecomUseCode(val code: String):
  case PRN extends TelecomUseCode("PRN")
  case ORN extends TelecomUseCode("ORN")
  case WPN extends TelecomUseCode("WPN")
  case VHN extends TelecomUseCode("VHN")
  case ASN extends TelecomUseCode("ASN")
  case EMR extends TelecomUseCode("EMR")
  case NET extends TelecomUseCode("NET")
  case BPN extends TelecomUseCode("BPN")
end TelecomUseCode
