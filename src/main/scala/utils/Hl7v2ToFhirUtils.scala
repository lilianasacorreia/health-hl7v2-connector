package utils

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.v25.group.ADT_A05_INSURANCE
import ca.uhn.hl7v2.model.v25.message.ADT_A05
import ca.uhn.hl7v2.model.v25.segment.{EVN, MSH, PID, ROL}
import org.apache.pekko.actor.ActorSystem
import org.hl7.fhir.r5.model.Bundle
import utils.bundle.BundleBase.{newBundle, withBundleBasics, withCoverage, withOrganization, withPatient, withPractitioner}

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait Hl7v2ToFhirUtils:
  /**
   * Converte uma mensagem HL7 em um Bundle FHIR contendo os recursos Patient, Organization e Practitioner.
   *
   * @param msg       Mensagem HL7 recebida.
   * @param ctx       Contexto FHIR utilizado para serialização e manipulação dos recursos (FhirContext).
   * @param hl7System Sistema de atores do Pekko que fornece acesso às configurações, incluindo códigos e nomes da organização.
   * @return Um objeto Bundle FHIR completo, com os recursos e metadados construídos a partir da mensagem HL7.
   */

  def handlePatientNew(msg: ADT_A05)(using ctx: FhirContext, hl7System: ActorSystem): Bundle = {
    val msh: MSH = msg.getMSH
    val evn: EVN = msg.getEVN
    val pid: PID = msg.getPID
    val rol = msg.getROL
    val nk1List = msg.getNK1All.asScala.toList
    val obx = msg.getOBXAll.asScala.toList
    val inGroupList: List[ADT_A05_INSURANCE] = msg.getINSURANCEAll.asScala.toList
    val managingOrganizationCode: String = hl7System.settings.config.getString("healthcareOrganization.code")
    val managingOrganizationName: String = hl7System.settings.config.getString("healthcareOrganization.name")

    val bundle: Bundle = newBundle
      .withBundleBasics(msh.getMessageControlID.toString)
      .withPatient(managingOrganizationCode, evn, pid, obx, rol, nk1List, Bundle.HTTPVerb.POST)
      .withPractitioner(rol, Bundle.HTTPVerb.POST)
      .withOrganization(managingOrganizationCode, Some(managingOrganizationName), Bundle.HTTPVerb.POST)

    buildCSPHealthcareOrganization(bundle, rol)
    buildPatientResponsibleEntity(bundle, inGroupList)

    bundle
  }

  /**
   * Extrai o número sequencial do paciente a partir do segmento PID, filtrando pelo sistema de autoridade "SONHO".
   *
   * @param pid Segmento PID da mensagem HL7 que contém a lista de identificadores do paciente.
   * @return String com o número sequencial (ID) atribuído pelo sistema SONHO.
   */

  private def extractSequentialNumber(pid: PID): String = {
    pid.getPatientIdentifierList.toList
      .filter(_.getAssigningAuthority.getNamespaceID.getValue.equals("SONHO")).head.getIDNumber.getValue
  }

  /**
   * Valida a existencia de entidade dos CSP ao qual o utente pertence.
   *
   * @param pid Segmento PID da mensagem HL7 que contém a lista de identificadores do paciente.
   * @return String com o número sequencial (ID) atribuído pelo sistema SONHO.
   */
  private def buildCSPHealthcareOrganization(bundle: Bundle, rol: ROL): Bundle = {
    val organizationCSPCode: Option[String] = Option(rol.getOrganizationUnitType.getIdentifier.getValue)
    val organizationCSPName: Option[String] = Option(rol.getOrganizationUnitType.getText.getValue)
    organizationCSPCode.foreach(code => bundle.withOrganization(code, organizationCSPName, Bundle.HTTPVerb.POST))
    bundle
  }

  private def buildPatientResponsibleEntity(bundle: Bundle, inGroupList: List[ADT_A05_INSURANCE]): Bundle = {
    inGroupList.foreach { insuranse =>
      val in1Opt = Option(insuranse.getIN1)
      in1Opt.map { in1 =>
        Option(in1.getIn12_InsurancePlanID.getIdentifier.getValue).map { organizationId =>
          val organizationName = Option(in1.getIn12_InsurancePlanID.getText.getValue)
          bundle.withOrganization(organizationId,organizationName, Bundle.HTTPVerb.POST)
          bundle.withCoverage(in1, Bundle.HTTPVerb.POST)
        }
      }
    }
    bundle
  }
