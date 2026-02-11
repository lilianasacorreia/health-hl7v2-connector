package utils.schemas
/**
 * Enumeração que define as extensões FHIR (Fast Healthcare Interoperability Resources)
 * utilizadas para enriquecer ou complementar os recursos padrão do modelo FHIR,
 * assegurando compatibilidade com requisitos específicos do sistema nacional de saúde português.
 *
 * @param uri URI único que identifica a extensão FHIR (ex: "http://spms.min-saude.pt/fhir/iop/extensions/nationality").
 * @param description Descrição da finalidade ou do conteúdo da extensão.
 *
 * Casos definidos:
 * - EXTENSION_ADDRESS → Extensões adicionais aplicadas ao recurso de morada (v1-1-2)
 * - NATIONALITY → Extensão que define a nacionalidade do utente
 * - ADDRESS_TYPE → Categoria ou tipo da morada (ex: residência, correspondência)
 * - COUNTRY → Código do país (ISO 3166-1 alpha-2)
 * - COUNTY → Código do distrito (nível administrativo NUTS-3)
 * - MUNICIPALITY → Código do concelho
 * - PARISH → Código da freguesia
 * - BIRTH_PLACE → Local de nascimento ou naturalidade do utente
 * - INDICATIVE → Indicativo telefónico (ex: +351 para Portugal)
 * - PATIENTNOTES → Observações clínicas ou administrativas associadas à ficha do utente
 * - ACTIVITYAREA → Área de atividade ou setor profissional
 */
enum FhirExtension (val uri: String,
                    val description: String):
  case  EXTENSION_ADDRESS extends FhirExtension("http://exemplo.pt/fhir/extensions/extension-address-v1-1-2", "Extensões à morada v1-1-2")
  case NATIONALITY extends FhirExtension("http://spms.min-saude.pt/fhir/iop/extensions/nationality", "Nacionalidade")
  case ADDRESS_TYPE extends FhirExtension("address-type", "Categoria da morada")
  case COUNTRY extends FhirExtension("country", "Código país")
  case COUNTY extends FhirExtension("county", "Código distrito")
  case MUNICIPALITY extends FhirExtension("municipality", "Código concelho")
  case PARISH extends FhirExtension("parish", "Código freguesia")
  case BIRTH_PLACE extends FhirExtension("http://exemplo.pt/fhir/extensions/birthplace", "Local de nascimento / naturalidade")
  case INDICATIVE extends FhirExtension("http://exemplo.pt/fhir/extensions/indicative", "Telecom indicative")
  case PATIENTNOTES extends FhirExtension("notes", "Observações na ficha do utente")
  case ACTIVITYAREA extends FhirExtension("activity-area", "Area de atividade")

end FhirExtension
