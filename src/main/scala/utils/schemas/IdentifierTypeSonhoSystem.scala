package utils.schemas
/**
 * Enumeração que representa os diferentes tipos de identificadores utilizados no sistema SONHO (Sistema de Informação Hospitalar),
 * mapeando códigos internos a descrições, autoridades emissoras e sistemas de referência.
 *
 * Cada caso define um tipo de identificador único, com os seguintes parâmetros:
 *
 * @param code Código curto associado ao identificador (ex: "SNS", "NIF", "P").
 * @param display Descrição em inglês do identificador (ex: "Health Card Number").
 * @param text Descrição em português do identificador (ex: "Número nacional de utente").
 * @param assigningAuthority Autoridade emissora ou entidade responsável pelo número (ex: "IRN", "AIMA", "AT").
 * @param system URL base do sistema de identificação associado (por padrão: 
 *               "https://spmspt.atlassian.net/wiki/spaces/DIS/pages/1291681847/N+mero+de+Identifica+o").
 *
 * Casos definidos:
 * - NS   → Identificador interno do SONHO (autoridade: SONHO)
 * - SNS  → Número nacional de utente (autoridade: RNU)
 * - B    → Número de identificação civil / Cartão de Cidadão (autoridade: IRN)
 * - C    → Cédula Pessoal (autoridade: IRN)
 * - P    → Número de Passaporte (autoridade: AIMA)
 * - NIF  → Número de identificação fiscal (autoridade: AT)
 * - NISS → Número de identificação da segurança social (autoridade: SS)
 * - PRC  → Número de visto de residência (autoridade: AIMA)
 *
 * Esta enumeração é usada para garantir consistência e interoperabilidade nos identificadores de pacientes
 * entre diferentes sistemas dentro do ecossistema SONHO, RNU, IRN, AT, SS e AIMA.
 */
enum IdentifierTypeSonhoSystem(val code: String,
                               val display: String,
                               val text: String,
                               val assigningAuthority: String,
                               val system: String = "https://spmspt.atlassian.net/wiki/spaces/DIS/pages/1291681847/N+mero+de+Identifica+o"):
  

  case NS extends IdentifierTypeSonhoSystem("NS", "Patient internal identifier", "Identificador interno do Sonho", "SONHO")
  case SNS extends IdentifierTypeSonhoSystem("SNS", "Health Card Number", "Número nacional de utente", "RNU")
  case B extends IdentifierTypeSonhoSystem("B", "Citizenship Card", "Número de identificação civil", "IRN") 
  case C extends IdentifierTypeSonhoSystem("C", "Birth Certificate", "Cédula Pessoal", "IRN")
  case P extends IdentifierTypeSonhoSystem("P", "Passport Number", "Passport number", "AIMA")
  case NIF extends IdentifierTypeSonhoSystem("NIF", "Tax ID number", "Número de identificação fiscal", "AT")
  case NISS extends IdentifierTypeSonhoSystem("NISS", "Social Security Number", "Número de identificação da segurança Social", "SS")
  case PRC extends IdentifierTypeSonhoSystem("PRC", "Permanent Resident Card Number", "Visto de residência", "AIMA")
  
end IdentifierTypeSonhoSystem
