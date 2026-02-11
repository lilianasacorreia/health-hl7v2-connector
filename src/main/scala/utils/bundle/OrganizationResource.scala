package utils.bundle

import utils.BaseUtils
import org.apache.pekko.event.slf4j.Logger
import org.hl7.fhir.r5.model.Identifier.IdentifierUse
import org.hl7.fhir.r5.model.Organization
import org.slf4j.Logger as SLFLogger
import utils.BaseFhirUtils.buildIdentifier

import java.util.UUID 

object OrganizationResource extends BaseUtils {
  val logger: SLFLogger = Logger("Organization Resource")

  protected[bundle] def buildOrganization(code: String, name: Option[String]): Organization = {
    buildBaseOrganization(code, name)
  }

  /**
   * Constrói um objeto base do tipo Organization a partir do código e do nome fornecidos.
   *
   * @param code Código único utilizado para gerar o identificador (ID) da organização.
   * @param name Nome oficial da organização a ser definido no recurso FHIR.
   * @return Um objeto Organization inicializado com ID, nome, estado ativo e identificador.
   */
  private def buildBaseOrganization(code: String, name: Option[String]): Organization = {
    val organization = new Organization
    val id = UUID.nameUUIDFromBytes(code.getBytes).toString.toLowerCase
    organization.setId(id)
    organization.setActive(true)
    name.map(_ => organization.setName(_))
    buildOrganizationIdentifier(code, organization)
    organization
  }

  /**
   * Adiciona um identificador ao recurso Organization com base no código fornecido.
   *
   * @param code         Código da organização usado para criar o identificador principal.
   * @param organization Objeto Organization ao qual o identificador será adicionado.
   * @return O objeto Organization atualizado com o identificador definido.
   */
  private def buildOrganizationIdentifier(code: String, organization: Organization): Organization = {
    val identifier = buildIdentifier(code, Some("ACSS"), Some(IdentifierUse.USUAL))
    organization.addIdentifier(identifier)
  }
}
