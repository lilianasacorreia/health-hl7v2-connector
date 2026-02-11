package utils.schemas

/**
 * Enumeration representing the administrative sex (gender) of a patient according to multiple coding systems:
 * <ul>
 * <li>HL7 v2 (e.g., "M", "F", "A", "U")</li>
 * <li>FHIR administrative gender codes (e.g., "male", "female", "other", "unknown")</li>
 * <li>S3 (SPMS) internal code system</li>
 * <li>Solicitation display text (Portuguese human-readable description)</li>
 * </ul>
 *
 * <p>This enumeration aligns the different code systems used in healthcare standards
 * to represent a patient's administrative sex or gender.</p>
 * MALE - Masculino
 * FEMALE - Feminino
 * UNDEFINED - Indeterminado
 */
enum AdministrativeSex(val v2Code: String, val fhirCode: String, val sctCode: String, val display: String):

  case MALE extends AdministrativeSex("M", "male", "248152002", "Masculino")
  case FEMALE extends AdministrativeSex("F", "female", "248153007", "Feminino")
  case UNDEFINED extends AdministrativeSex("A", "other", "32570681000036106", "Indeterminado")
  case UNKNOWN extends AdministrativeSex("U", "unknown", "184115007", "Desconhecido")

end AdministrativeSex

/**
 * Companion object for {@link AdministrativeSex}, providing lookup methods for code-based access.
 */
object AdministrativeSex:
  /**
   * Retrieves an {@link AdministrativeSex} by its HL7 v2 code.
   *
   * @param code the v2 code (e.g., "M", "F", "A", "U")
   * @return an {@code Option[AdministrativeSex]} containing the matching value, or None if not found.
   */
  def fromV2Code(code: String): Option[AdministrativeSex] =
    values.find(_.v2Code == code)

  /**
   * Retrieves an {@link AdministrativeSex} by its FHIR code.
   *
   * @param code the FHIR administrative gender code (e.g., "male", "female")
   * @return an {@code Option[AdministrativeSex]} containing the matching value, or None if not found.
   */
  def fromFhirCode(code: String): Option[AdministrativeSex] =
    values.find(_.fhirCode == code)

  /**
   * Retrieves an {@link AdministrativeSex} by its internal S3 code.
   *
   * @param code the Snomed CT system code
   * @return an {@code Option[AdministrativeSex]} containing the matching value, or None if not found.
   */
  def fromSnomedCode(code: String): Option[AdministrativeSex] =
    values.find(_.sctCode == code)

  /**
   * Retrieves an {@link AdministrativeSex} by its Portuguese solicitation display value.
   *
   * @param display the display string (e.g., "Masculino", "Feminino")
   * @return an {@code Option[AdministrativeSex]} containing the matching value, or None if not found.
   */
  def fromDisplay(display: String): Option[AdministrativeSex] =
    values.find(_.display == display)

