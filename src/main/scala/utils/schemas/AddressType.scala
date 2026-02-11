package utils.schemas

/**
 * Enumeration representing different types of addresses used in HL7 v2 messages and 
 * national health extensions (such as SPMS extensions for Portugal).
 * Each {@code AddressType} value includes:
 * <li>{@code system} – the coding system URI (e.g., HL7 v2 or SPMS extension)</li>
 * <li>{@code code} – the code representing the address type</li>
 * <li>{@code description} – a human-readable explanation of the address type</li>
 * </ul>
 * <p>These codes are typically used in HL7 segments such as PID or NK1 to identify
 * the nature or purpose of a given address (mailing, office, birth, etc.).
 * MAILING (M) - Mailing address used for correspondence purposes. 
 * BIRTH_NOTSPECIFIED (N) - Birth (née) address, not otherwise specified.
 * OFFICE (O) - Office or business address.
 * CURRENT (C) - Current or temporary residential address.
 * MAIN_ADDRESS (MA) - Main address according to SPMS national extension.
 * OTHER (OTH) - Other address type not covered by the predefined categories.
 * 
 */
enum AddressType( val system: String,
                  val code: String,
                  val description: String):
  case MAILING extends AddressType("https://spmspt.atlassian.net/wiki/spaces/DIS/pages/4269637679/Tabela+0190+-+Address+type", "M", "Mailing")
  case BIRTH_NOTSPECIFIED extends AddressType("https://spmspt.atlassian.net/wiki/spaces/DIS/pages/4269637679/Tabela+0190+-+Address+type", "N", "Birth (nee) (birth address, not otherwise specified)")
  case OFFICE extends AddressType("https://spmspt.atlassian.net/wiki/spaces/DIS/pages/4269637679/Tabela+0190+-+Address+type", "O", "Office")
  case CURRENT extends AddressType("https://spmspt.atlassian.net/wiki/spaces/DIS/pages/4269637679/Tabela+0190+-+Address+type", "C", "Current Or Temporary")
  case MAIN_ADDRESS extends AddressType("http://spms.min-saude.pt/rnu/extensions/address-types", "MA", "Main Address")
  case OTHER extends AddressType("http://spms.min-saude.pt/rnu/extensions/address-types", "OTH", "Other")


end AddressType

object AddressType:
  
  /**
   * Case class representing a single AddressType entry.
   *
   * @param system      coding system URI (e.g., HL7 v2 or SPMS extension)
   * @param code        address type code (e.g., "M", "O", "MA")
   * @param description human-readable description of the address type
   */
  
  def fromAddressTypeCode(code: String): Option[AddressType] =
    values.find(_.code == code)
