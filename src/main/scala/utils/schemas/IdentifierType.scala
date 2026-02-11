package utils.schemas

/**
 * Enumeration representing various types of personal, professional, and administrative identifiers
 * according to the HL7 v2-0203 code system and national extensions.
 * Each {@code IdentifierType} value provides:
 * <ul>
 * <li>{@code code} – the identifier code (e.g., "CZ", "TAX")</li>
 * <li>{@code display} – the short English label of the identifier</li>
 * <li>{@code text} – a Portuguese human-readable description</li>
 * <li>{@code system} – the coding system URI, defaulting to
 * {@code http://terminology.hl7.org/CodeSystem/v2-0203}</li>
 * </ul>
 * <p>These identifiers are used to classify IDs such as national ID, tax number,professional licenses, 
 * and healthcare identifiers within FHIR and HL7 messages.
 * 
 * CZ - Citizenship Card: Número de identificação civil.
 * TAX - Tax ID Number: Número de identificação fiscal.
 * SS - Social Security Number: Número de identificação da segurança social.
 * HC - Health Card Number: Número nacional de utente.
 * PPN - Passport Number: Número do passaporte.
 * MI - Military ID Number: Número da cédula militar.
 * PT - Patient External Identifier: Número externo de saúde.
 * DL - Driver’s License Number: Número da carta de condução.
 * ANON - Anonymous Identifier: Chave referente à identificação anónima da pessoa.
 * MR - Medical Record Number: Número do processo clínico.
 * PI - Patient Internal Identifier: Identificador sequencial do utente na entidade.
 * PRC - Permanent Resident Card Number: Visto de residência.
 * EI - Employee Number: Número interno do profissional.
 * MEI - Employee Number (Mecanicográfico): Número mecanográfico do profissional.
 * MD - Medical License Number: Número da Ordem dos Médicos.
 * NP - Nurse Professional Number: Número da Ordem dos Enfermeiros.
 * U - Other Identifier: Outro identificador (ex: sistema S3).
 * BCFN - Birth Certificate Number: Certificado de nascimento.
 */

enum IdentifierType(val code: String, 
                    val display: String, 
                    val text: String, 
                    val system: String = "http://terminology.hl7.org/CodeSystem/v2-0203"):
  
  case CZ extends IdentifierType("CZ", "Citizenship Card", "Número de identificação civil")
  case TAX extends IdentifierType("TAX", "Tax ID number", "Número de identificação fiscal")
  case SS extends IdentifierType("SS", "Social Security Number", "Número de identificação da segurança social")
  case HC extends IdentifierType("HC", "Health Card Number", "Número nacional de utente")
  case PPN extends IdentifierType("PPN", "Passport Number", "Passport number")
  case MI extends IdentifierType("MI", "Military ID number", "Número da cédula militar")
  case PT extends IdentifierType("PT", "Patient External Identifier", "Número externo de saúde")
  case DL extends IdentifierType("DL", "Driver's license number", "Número da carta de condução")
  case ANON extends IdentifierType("ANON", "Anonymous identifier", "Chave referente à identificação da pessoa")
  case MR extends IdentifierType("MR", "Medical record number", "Número do processo")
  case PI extends IdentifierType("PI", "Patient internal identifier", "Identificador sequencial do utente na entidade")
  case PRC extends IdentifierType("PRC", "Permanent Resident Card Number", "Visto de residência")
  case EI extends IdentifierType("EI", "Employee number", "Número interno do profissional")
  case MEI extends IdentifierType("EI", "Employee number", "Número mecanográfico do profissional")
  case MD extends IdentifierType("MD", "Medical License number", "Número da ordem dos médicos",
    "http://terminology.hl7.org/CodeSystem/practitioner-role")
  case NP extends IdentifierType("NP", "Nurse Professional Number", "Número da ordem dos enfermeiros",
    "http://terminology.hl7.org/CodeSystem/practitioner-role")
  case BCFN extends IdentifierType("BCFN", "Birth Certificate", "Certificado de Nascimento")

end IdentifierType

/**
 * Companion object providing helper methods for the {@link IdentifierType} enum.
 */
object IdentifierType:
  
  /**
   * Retrieves an {@link IdentifierType} by its code.
   *
   * @param code the short code (e.g., "CZ", "TAX", "HC")
   * @return an {@code Option[IdentifierType]} containing the matching entry, or None if not found.
   */
  def fromCode(code: String): Option[IdentifierType] =
    values.find(_.code == code)
