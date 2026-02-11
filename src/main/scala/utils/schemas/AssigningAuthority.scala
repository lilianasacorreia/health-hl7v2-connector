package utils.schemas
/**
 * Enumeração que representa as diferentes autoridades emissoras ou sistemas responsáveis
 * pela atribuição de identificadores, números ou registos dentro do ecossistema de saúde português.
 *
 * Cada caso define uma entidade ou sistema que atua como "Assigning Authority" — isto é,
 * a fonte oficial que emite, valida ou gere o identificador de um determinado indivíduo, profissional
 * ou registo clínico.
 *
 * @param system Nome do sistema ou entidade responsável pela emissão do identificador.
 *
 * Casos definidos:
 * - SONHO → Sistema de Informação Hospitalar (identificadores internos hospitalares)
 * - EAGENDA → Sistema de marcação de consultas e atos médicos
 * - S3 → Sistema de informação clínica (SClínico/Sonho 3)
 * - SNS24 → Serviço Nacional de Saúde 24h (contactos e triagem clínica)
 * - RNU → Registo Nacional de Utentes (identificação única de utentes)
 * - SS → Segurança Social (números de identificação social)
 * - AT → Autoridade Tributária (números fiscais)
 * - IRN → Instituto dos Registos e Notariado (cartões de cidadão, cédulas pessoais)
 * - IMTT → Instituto da Mobilidade e dos Transportes Terrestres (licenças de condução, etc.)
 * - RHV → Registo de História de Vacinação
 * - SINUS → Sistema Nacional de Utilização de Serviços (plataforma de gestão de utilização de serviços)
 * - DOCTOR_NUMBER → Ordem dos Médicos (número de inscrição profissional)
 * - NURSING_NUMBER → Ordem dos Enfermeiros (número de inscrição profissional)
 * - HIP → Health Information Platform (plataforma de dados de saúde)
 * - SIGA → Sistema de Gestão de Acesso (controlo e gestão de utilizadores)
 * - SCLINICOH → Sistema SClínico Hospitalar
 * - SCLINICOCSP → Sistema SClínico Cuidados de Saúde Primários
 *
 * Esta enumeração assegura consistência e interoperabilidade ao identificar a origem
 * dos códigos, números e registos utilizados em diferentes sistemas de informação
 * no contexto da saúde em Portugal.
 */
enum AssigningAuthority(val system: String):
  case SONHO extends AssigningAuthority("SONHO")
  case EAGENDA extends AssigningAuthority("eAgenda")
  case S3 extends AssigningAuthority("S3")
  case SNS24 extends AssigningAuthority("SNS24")
  case RNU extends AssigningAuthority("RNU")
  case SS extends AssigningAuthority("SS")
  case AT extends AssigningAuthority("AT")
  case IRN extends AssigningAuthority("IRN")
  case IMTT extends AssigningAuthority("IMTT")
  case RHV extends AssigningAuthority("RHV")
  case SINUS extends AssigningAuthority("SINUS")
  case DOCTOR_NUMBER extends AssigningAuthority("Ordem dos Medicos")
  case NURSING_NUMBER extends AssigningAuthority("Ordem dos Enfermeiros")
  case HIP extends AssigningAuthority("HIP")
  case SIGA extends AssigningAuthority("SIGA")
  case SCLINICOH extends AssigningAuthority("SCLINICOH")
  case SCLINICOCSP extends AssigningAuthority("SCLINICOCSP")
end AssigningAuthority


