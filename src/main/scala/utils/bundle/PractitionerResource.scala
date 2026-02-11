package utils.bundle


import ca.uhn.hl7v2.model.v25.datatype.XCN
import ca.uhn.hl7v2.model.v25.segment.ROL
import org.apache.pekko.event.slf4j.Logger
import org.hl7.fhir.r5.model.HumanName.NameUse
import org.hl7.fhir.r5.model.Identifier.IdentifierUse
import org.hl7.fhir.r5.model.{CodeableConcept, HumanName, Practitioner}
import org.slf4j.Logger as SLFLogger
import utils.BaseFhirUtils.{buildCodeableConcept, buildCoding, buildHumanName, buildIdentifier}
import utils.BaseUtils
import utils.schemas.AssigningAuthority.{RHV, SONHO}
import utils.schemas.IdentifierType.{EI, MD, MEI, NP}

import java.util.UUID

object PractitionerResource extends BaseUtils {
  val logger: SLFLogger = Logger("Practitioner Resource")
  lazy val meiCode = "N.Mecanográfico"

  protected[bundle] def buildPractitioner(rol: ROL): Practitioner = {
    val practitioner: Practitioner = buildBasePractitioner(rol)
    practitioner
  }

  /**
   * Constrói um objeto base do tipo Practitioner.
   *
   * @param rol Segmento ROL.
   * @return Um objeto Practitioner inicializado com ID, nome e identificadores do profissional.
   */

  private def buildBasePractitioner(rol: ROL): Practitioner = {
    val practitioner = new Practitioner
    val id = buildIfFromIdentifier(rol).getOrElse(buildIdFromName(rol))

    UUID.randomUUID().toString.toLowerCase
    practitioner.setId(id)
    buildPractitionerHumanName(practitioner, rol)
    buildPractitionerIdentifierToPractitionerResource(practitioner, rol)
  }

  private def buildCSPHealthcareOrganizationId(rol: ROL): Option[String] = {
    Option(rol.getOrganizationUnitType.getIdentifier.getValue).map(code =>
      UUID.nameUUIDFromBytes(code.getBytes()).toString.toLowerCase())
  }

  private def buildCSPPractitionerId(rol: ROL): Option[String] = {
    rol.getRol4_RolePerson.toList.headOption
      .flatMap(person => Option(person.getIDNumber.getValue)
        .map(code =>
          UUID.nameUUIDFromBytes(code.getBytes()).toString.toLowerCase)
      )
  }

  private def buildIfFromIdentifier(rol: ROL) = {
    rol.getRol4_RolePerson.toList.headOption
      .flatMap(person => Option(person.getIDNumber.getValue).map(code =>
        UUID.nameUUIDFromBytes(code.getBytes()).toString.toLowerCase)
      )
  }

  private def buildIdFromName(rol: ROL) = {
    val practitionerName: Option[String] = rol.getRol4_RolePerson.toList.headOption
      .flatMap { person =>
        val familyNameOpt = Option(person.getFamilyName).map(_.getSurname.getValue)
        val givenNameOpt = Option(person.getGivenName).map(_.getValue)

        (familyNameOpt, givenNameOpt) match {
          case (Some(familyName), Some(givenName)) => Some(s"${Some(familyName)}${Some(givenName)}")
          case (Some(familyName), None) => Some(familyName)
          case (None, Some(givenName)) => Some(givenName)
          case _ => None
        }
      }
    practitionerName.map(name => UUID.nameUUIDFromBytes(name.getBytes()).toString.toLowerCase())
      .getOrElse(UUID.randomUUID.toString.toLowerCase)
  }

  /**
   * Adiciona identificadores ao recurso Practitioner com base nas informações do segmento ROL.
   *
   * @param practitioner Objeto Practitioner ao qual os identificadores serão adicionados.
   * @param rol          Segmento ROL contendo os dados de identificação do profissional (XCN - Extended Composite ID Number).
   * @return O objeto Practitioner atualizado com os identificadores construídos.
   */

  private def buildPractitionerIdentifierToPractitionerResource(practitioner: Practitioner, rol: ROL): Practitioner = {
    val identifierList: Array[XCN] = rol.getRolePerson()
    identifierList.foreach { identifier =>
      Option(identifier.getIDNumber.getValue).foreach { id =>
        val system = identifier.getAssigningAuthority.getNamespaceID.getValue match {
          case meiCode => Some(RHV.system)
          case _ => Option(identifier.getAssigningAuthority.getNamespaceID.getValue)
        }
        val identifierTypeCode = identifier.getIdentifierTypeCode.getValue
        val assigningAuthority = identifier.getAssigningAuthority.getNamespaceID.getValue
        practitioner.addIdentifier(
          buildIdentifier(id, system, Some(IdentifierUse.USUAL), getIdentifierType(identifierTypeCode, assigningAuthority)))
      }
    }
    practitioner
  }

  /**
   * Determina o tipo de identificador (IdentifierType) do Practitioner com base no código e na autoridade emissora.
   *
   * @param value         Código do identificador (ex: EI, MD, NP) proveniente do campo IdentifierTypeCode do HL7.
   * @param assigningAuth Autoridade emissora (Assigning Authority) associada ao identificador.
   * @return Um Option[CodeableConcept] que representa o tipo de identificador correspondente, ou None se não houver correspondência.
   */

  private def getIdentifierType(value: String, assigningAuth: String): Option[CodeableConcept] = {
    value match {
      case EI.code if assigningAuth.equals(SONHO.system) => Some(buildCodeableConcept(List(buildCoding(EI.code, EI.system, Some(EI.display))), Some(EI.text)))
      case EI.code if assigningAuth.equals(meiCode) => Some(buildCodeableConcept(List(buildCoding(MEI.code, MEI.system, Some(MEI.display))), Some(MEI.text)))
      case MD.code => Some(buildCodeableConcept(List(buildCoding(MD.code, MD.system, Some(MD.display))), Some(MD.text)))
      case NP.code => Some(buildCodeableConcept(List(buildCoding(NP.code, NP.system, Some(NP.display))), Some(NP.text)))
      case _ => None
    }
  }


  private def buildPractitionerID(practitioner: Practitioner, rol: ROL): Practitioner = {
    val identifierList: Array[XCN] = rol.getRolePerson()
    val foundIdentifier = identifierList.find { xcn =>
      val assigningAuth = xcn.getAssigningAuthority
      assigningAuth != null &&
        assigningAuth.getNamespaceID != null &&
        assigningAuth.getNamespaceID.getValue == meiCode
    }
    val id = foundIdentifier match {
      case Some(identifier) =>
        val idNumber = identifier.getIDNumber.getValue
        UUID.nameUUIDFromBytes(idNumber.getBytes).toString.toLowerCase
      case None =>
        UUID.randomUUID().toString.toLowerCase
    }
    practitioner.setId(id)
    practitioner
  }

  //  private def buildPractitionerID(practitioner: Practitioner, rol: ROL): Practitioner = {
  //    val identifierList: Array[XCN] = rol.getRolePerson()
  //    val identifier = identifierList.head
  //    //find --- if AssigningAuth == meiCode
  //    val idNumber = identifier.getIDNumber.getValue
  //    val nameSpace = identifier.getAssigningAuthority.getNamespaceID.getValue
  //
  //    val id =
  //      if (nameSpace == meiCode){
  //        UUID.nameUUIDFromBytes(idNumber.getBytes).toString.toLowerCase
  //      } else {
  //        UUID.randomUUID().toString.toLowerCase()
  //      }
  //    practitioner.setId(id)
  //    practitioner
  //  }


  /**
   * Constrói e adiciona o nome completo (HumanName) ao recurso Practitioner com base nas informações do segmento HL7.
   *
   * @param practitioner Objeto Practitioner ao qual o nome será adicionado.
   * @param segment      Segmento HL7 (ex: ROL) que contém os dados de nome do profissional.
   * @tparam T Tipo genérico do segmento HL7 (neste caso, geralmente ROL).
   * @return O objeto Practitioner atualizado com o(s) nome(s) do profissional.
   */
  private def buildPractitionerHumanName[T](practitioner: Practitioner, segment: T): Practitioner = {
    val names = segment match {
      case rol: ROL => rol.getRolePerson
      case _ => throw new IllegalArgumentException(s"Unsupported segment type: ${segment.getClass.getName}")
    }
    names.foreach(name => buildPractitionerHumanName(name).foreach(humanName => practitioner.addName(humanName)))
    practitioner
  }

  /**
   * Constrói um objeto HumanName a partir das informações de nome presentes no campo XCN do HL7.
   *
   * @param name Objeto XCN (Extended Composite ID Number) contendo os componentes do nome do profissional (apelido, primeiro nome, nomes intermédios, etc.).
   * @return Um Option[HumanName] com o nome formatado do Practitioner, ou None caso os valores estejam ausentes.
   */
  private def buildPractitionerHumanName(name: XCN): Option[HumanName] = {
    val familyName = Option(name.getXcn2_FamilyName.getFn1_Surname.getValue)
    val givenName = Option(name.getGivenName.getValue)
    val middleName = Option(name.getSecondAndFurtherGivenNamesOrInitialsThereof.getValue).map(_.split(" "))
    Option(buildHumanName(familyName, givenName, middleName, Some(NameUse.USUAL)))

  }
}
