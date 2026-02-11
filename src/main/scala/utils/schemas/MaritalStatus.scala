package utils.schemas

/**
 * Enumeration representing a patient's marital status according to the
 * HL7 v3 Marital Status code system.
 *
 * <p>Each {@code MaritalStatus} includes multiple code mappings:
 * <ul>
 * <li>{@code code} – the HL7 v3 code (e.g., "M", "S", "D")</li>
 * <li>{@code display} – the Portuguese label used in national systems</li>
 * <li>{@code domainCode} – the local domain or database code (e.g., "1", "2", "3")</li>
 * <li>{@code sv2Code} – the SV2 compatibility code</li>
 * <li>{@code text} – the human-readable definition or explanation</li>
 * <li>{@code system} – the coding system URI (default: HL7 v3 Marital Status)</li>
 * </ul>
 *
 * <p>This enumeration maps HL7, FHIR, and SPMS local codes for marital status,
 * ensuring consistent representation of values such as "Married", "Divorced", or "Widowed".
 * DIVORCED - O contrato de casamento foi declarado dissolvido e inativo.
 * LEGALLY_SEPARATED - Separado legalmente
 * MARRIED - Um contrato de casamento atual está ativo
 * COMMON_LAW - União de facto reconhecida em algumas jurisdições.
 * NEVER_MARRIED - Nenhum contrato de casamento foi celebrado.
 * WIDOWED - O cônjuge faleceu.
 * UNKNOWN - Estado civil desconhecido.
 *
 */
enum MaritalStatus(val code: String,
                   val display: String,
                   val sv2Code: String,
                   val text: String

                  ):
  def system: String = MaritalStatus.defaultSystem

  case DIVORCED extends MaritalStatus("D", "Divorciado", "D", "Contrato de casamento foi declarado dissolvido e inativo")
  case LEGALLY_SEPARATED extends MaritalStatus("L", "Separado", "F", "Separado legalmente")
  case MARRIED extends MaritalStatus("M", "Casado", "M", "Um contrato de casamento atual está ativo")
  case COMMON_LAW extends MaritalStatus("C", "União de facto", "C",
    "Um casamento reconhecido em algumas jurisdições e baseado no acordo das partes em se considerarem casados, podendo também ser baseado em documentação de coabitação")
  case NEVER_MARRIED extends MaritalStatus("S", "Solteiro", "S", "Nenhum contrato de casamento foi celebrado")
  case WIDOWED extends MaritalStatus("W", "Viúvo", "W", "O cônjuge faleceu")
  case UNKNOWN extends MaritalStatus("UNK", "Desconhecido", "U", "Não se conhece o estado civil")

end MaritalStatus

object MaritalStatus:
  val defaultSystem: String = "http://terminology.hl7.org/CodeSystem/v3-MaritalStatus"

  /**
   * Retrieves a {@link MaritalStatus} by its HL7 v3 code.
   *
   * @param code the HL7 v3 code (e.g., "M", "S", "D").
   * @return an {@code Option[MaritalStatus]} containing the matching status, or None if not found.
   */
  def fromCode(code: String): Option[MaritalStatus] =
    values.find(_.code == code)

  /**
   * Retrieves all HL7 v3 marital status codes for this code system.
   *
   * @return list of all codes defined within the HL7 v3 Marital Status system.
   */
  def allCodes: List[String] =
    values.filter(_.system == defaultSystem).map(_.code).toList
