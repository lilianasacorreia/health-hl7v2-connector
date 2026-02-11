package utils.schemas
/**
 * Enumeração que representa os diferentes papéis (roles) de contacto de um indivíduo
 * segundo o padrão HL7 V2 (CodeSystem v2-0131) e, em um caso específico, HL7 V3 (RoleCode).
 *
 * @param code Código abreviado do papel de contacto (ex: "E", "N", "CP").
 * @param display Descrição textual do papel de contacto (ex: "Emergency Contact", "Next-of-Kin").
 * @param system Sistema de codificação HL7 associado. Por padrão, utiliza:
 *               "http://terminology.hl7.org/CodeSystem/v2-0131", podendo ser sobrescrito
 *               quando aplicável (ex: "http://terminology.hl7.org/CodeSystem/v3-RoleCode").
 *
 * Casos definidos:
 * - BP → Billing contact person — Pessoa responsável pela faturação
 * - CP → Contact person — Pessoa de contacto geral
 * - PR → Person preparing referral — Pessoa responsável pela preparação da referenciação
 * - E → Employer — Empregador do paciente
 * - C → Emergency Contact — Contacto de emergência
 * - F → Federal Agency — Agência governamental (nível federal)
 * - I → Insurance Company — Companhia de seguros
 * - N → Next-of-Kin — Parente próximo ou contacto familiar direto
 * - S → State Agency — Agência governamental (nível estadual)
 * - O → Other — Outro tipo de contacto
 * - U → Unknown — Contacto desconhecido ou não especificado
 * - ONESELF → Self — O próprio indivíduo (utiliza o sistema HL7 V3 RoleCode)
 *
 */
enum ContactRoleV2(val code: String,
                   val display: String,
                   val system: String = "http://terminology.hl7.org/CodeSystem/v2-0131"):

  case BP extends ContactRoleV2("BP", "Billing contact person")
  case CP extends ContactRoleV2("CP", "Contact person")
  case PR extends ContactRoleV2("PR", "Person preparing referral")
  case E extends ContactRoleV2("E", "Employer")
  case C extends ContactRoleV2("C", "Emergency Contact")
  case F extends ContactRoleV2("F", "Federal Agency")
  case I extends ContactRoleV2("I", "Insurance Company")
  case N extends ContactRoleV2("N", "Next-of-Kin")
  case S extends ContactRoleV2("S", "State Agency")
  case O extends ContactRoleV2("O", "Other")
  case U extends ContactRoleV2("U", "Unknown")
  case ONESELF extends ContactRoleV2("ONESELF", "Self", "http://terminology.hl7.org/CodeSystem/v3-RoleCode")

end ContactRoleV2