package utils.bundle


import ca.uhn.hl7v2.model.v25.datatype.{CE, CX, ST, XAD, XPN, XTN}
import ca.uhn.hl7v2.model.v25.segment.{EVN, NK1, OBX, PID, ROL}
import org.apache.pekko.event.slf4j.Logger
import org.hl7.fhir.r4.model.codesystems.V3RoleCode
import org.hl7.fhir.r5.model.Identifier.IdentifierUse
import org.hl7.fhir.r5.model.Patient.ContactComponent
import org.hl7.fhir.r5.model.{Address, Annotation, BooleanType, CodeableConcept, Coding, ContactPoint, DateTimeType, Enumerations, Extension, HumanName, Meta, Patient, ResourceType}
import org.slf4j.Logger as SLFLogger
import utils.BaseFhirUtils.{buildCodeableConcept, buildCoding, buildExtension, buildHumanName, buildIdentifier, buildReference}
import utils.BaseUtils
import utils.DateFormatConverterUtils.getDateConverted25ToFhir
import utils.schemas.AddressType.{BIRTH_NOTSPECIFIED, CURRENT, MAILING, MAIN_ADDRESS, OFFICE, fromAddressTypeCode}
import utils.schemas.AssigningAuthority.SONHO
import utils.schemas.FhirExtension.{ADDRESS_TYPE, BIRTH_PLACE, COUNTY, EXTENSION_ADDRESS, MUNICIPALITY, PARISH}
import utils.schemas.IdentifierType.{BCFN, CZ, HC, PI, PPN, SS, TAX}
import utils.schemas.IdentifierTypeSonhoSystem.{B, C, NIF, NISS, NS, P, PRC, SNS}
import utils.schemas.{AdministrativeSex, ContactRoleV2, FhirExtension, IdentifierType, MaritalStatus, RelationshipCodeSystemV2, TelecomEquipmentType, TelecomUseCode}

import java.util.UUID
import scala.jdk.CollectionConverters.*

object PatientResource extends BaseUtils {

  val logger: SLFLogger = Logger("Patient Resource")
  private val locationSystem: String = "http://www.ine.pt"

  /**
   * Constrói um recurso FHIR do tipo Patient, combinando dados do paciente e dos seus contactos familiares a partir de múltiplos segmentos HL7.
   *
   * @param organization Nome ou identificador da organização associada ao paciente.
   * @param evn          Segmento EVN contendo informações sobre o evento.
   * @param pid          Segmento PID contendo os dados principais do paciente.
   * @param obxList      Lista de segmentos OBX que podem conter observações adicionais sobre o paciente.
   * @param rol          Segmento ROL com informações sobre o profissional responsável ou entidade associada ao paciente.
   * @param nk1List      Lista de segmentos NK1 representando contactos familiares ou próximos do paciente.
   * @return Um objeto Patient completo.
   */
  protected[bundle] def buildPatient(organization: String, evn: EVN, pid: PID, obxList: List[OBX], rol: ROL, nk1List: List[NK1]): Patient = {
    val patient: Patient = buildBasePatient(organization, evn, pid, obxList, rol)
    buildPatientWithRelatives(patient, nk1List)
    patient
  }

  /**
   * Constrói o objeto base do tipo Patient a partir de múltiplos segmentos HL7 (PID, EVN, ROL, OBX),
   * preenchendo todas as informações principais do paciente.
   */
  private def buildBasePatient(organization: String, evn: EVN, pid: PID, obxListOpt: List[OBX], rol: ROL): Patient = {
    val patient = new Patient
    val patientNumber: String = pid.getPid3_PatientIdentifierList.toList
      .find(patientId => patientId.getAssigningAuthority.getNamespaceID.getValue.equals(SONHO))
      .map(ns => UUID.nameUUIDFromBytes(ns.getIDNumber.getValue.getBytes).toString.toLowerCase())
      .getOrElse(UUID.randomUUID().toString.toLowerCase)
    patient.setId(patientNumber)
    patient.setMeta(buildPatientMeta(Some(evn)))

    buildPatientIdentifiersToPatientResource(patient, pid)
    buildMedicalProcessNumberPatientIdentifier(patient, pid)
    buildPatientHumanName(patient, pid)
    buildPatientBirthDate(patient, pid)
    buildPatientGender(patient, pid)
    buildPatientAddresses(patient, pid)
    buildPatientBirthPlaceExtension(patient, pid)
    buildNationalityExtension(patient, pid)
    buildPatientDeceasedStatus(patient, pid)
    buildPatientTelecomInfo(patient, pid)
    buildPatientMaritalStatus(patient, pid)
    buildGeneralPractitioner(patient, rol)
    buildManagingOrganization(patient, organization)
    buildPatientObservations(patient, obxListOpt)
    patient
  }

  /**
   * Adiciona ao Patient os contactos de familiares/entidades próximas (NK1), mapeando relação, nome, telefones e morada.
   *
   * @param patient Recurso FHIR Patient que será enriquecido com a lista de contactos.
   * @param nk1List Lista de segmentos HL7 NK1 contendo dados dos contactos (relação, nome, telefones e endereço).
   * @return O objeto Patient atualizado com os contactos e respetivas prioridades (rank).
   */
  private def buildPatientWithRelatives(patient: Patient, nk1List: List[NK1]) = {
    nk1List.foreach { nk1 =>
      val contactPerson = new ContactComponent()
      val relationshipCoding = mapRelationshipCode(nk1)
      contactPerson.addRelationship(buildCodeableConcept(List(relationshipCoding)))
      nk1.getNKName.foreach { name =>
        buildPatientHumanName(name).foreach(humanName => contactPerson.setName(humanName))
      }
      nk1.getNk15_PhoneNumber.map { contact =>
        val telecom = new ContactPoint
        setTelecomValue(contact, telecom)
        Option(telecom.getValue) match {
          case Some(_) =>
            getUseCode(contact.getTelecommunicationUseCode.getValue, contact.getTelecommunicationEquipmentType.getValue, telecom)
            getTelecomSystem(contact.getTelecommunicationEquipmentType.getValue, telecom)
          case None =>
        }
        contactPerson.addTelecom(telecom)
      }
      buildRelatedPersonAddress(contactPerson, nk1)
      patient.addContact(contactPerson)
    }
    buildPatientContactRank(patient)
    patient
  }

  /**
   * Define o endereço do contacto (RelatedPerson) a partir dos XAD do NK1, validando Cód. Postal PT e registando avisos quando inválido.
   *
   * @param contactPerson Componente de contacto do Patient que receberá o endereço mapeado.
   * @param nk1           Segmento HL7 NK1 contendo os endereços XAD do contacto.
   */
  private def buildRelatedPersonAddress(contactPerson: ContactComponent, nk1: NK1): Unit = {
    val addresses: Array[XAD] = nk1.getAddress
    addresses.foreach { address =>
      address.getCountry.getValue match {
        case "PRT" | "PT" =>
          val isValidPostalCode = Option(address.getZipOrPostalCode.getValue).exists(_.matches("\\d{4}-\\d{3}"))
          if (!isValidPostalCode) {
            logger.warn(s"Invalid postal code found: ${address.getZipOrPostalCode.getValue}")
          } else {
            val ad: Address = mapXadToAddress(address, isFromPid = false)
            contactPerson.setAddress(ad)
          }
        case _ => Option(address.getZipOrPostalCode.getValue).map {
          case "-" => logger.warn(s"Invalid postal code found: ${address.getZipOrPostalCode.getValue}")
          case _ =>
            val ad: Address = mapXadToAddress(address, isFromPid = false)
            contactPerson.setAddress(ad)
        }
      }
    }
  }

  /**
   * Mapeia o código de relação do NK1 para um Coding FHIR, registando aviso e usando "Other" quando inválido.
   *
   * @param nk1 Segmento HL7 NK1 contendo o campo Relationship (identifier) a ser convertido.
   * @return Coding FHIR correspondente à relação (ex.: FTH, MTH, SPO, EXF, EMC, OTH, SEL), ou "Other" por defeito.
   */
  private def mapRelationshipCode(nk1: NK1) = {
    nk1.getRelationship.getIdentifier.getValue match {
      case RelationshipCodeSystemV2.FTH.code =>
        buildCoding(V3RoleCode.FTH.toCode, V3RoleCode.FTH.getSystem, Some(V3RoleCode.FTH.getDisplay))
      case RelationshipCodeSystemV2.MTH.code =>
        buildCoding(V3RoleCode.MTH.toCode, V3RoleCode.MTH.getSystem, Some(V3RoleCode.MTH.getDisplay))
      case RelationshipCodeSystemV2.SPO.code =>
        buildCoding(V3RoleCode.SPS.toCode, V3RoleCode.SPS.getSystem, Some(V3RoleCode.SPS.getDisplay))
      case RelationshipCodeSystemV2.EXF.code =>
        buildCoding(V3RoleCode.FAMMEMB.toCode, V3RoleCode.FAMMEMB.getSystem, Some(V3RoleCode.FAMMEMB.getDisplay))
      case RelationshipCodeSystemV2.EMC.code =>
        buildCoding(ContactRoleV2.C.code, ContactRoleV2.C.system, Some(ContactRoleV2.C.display))
      case RelationshipCodeSystemV2.OTH.code =>
        buildCoding(ContactRoleV2.O.code, ContactRoleV2.O.system, Some(ContactRoleV2.O.display))
      case RelationshipCodeSystemV2.SEL.code =>
        buildCoding(ContactRoleV2.ONESELF.code, ContactRoleV2.ONESELF.system, Some(ContactRoleV2.ONESELF.display))
      case _ =>
        logger.warn(s"Relationship code not valid: ${nk1.getRelationship.getIdentifier.getValue}, going to process as other")
        buildCoding(ContactRoleV2.O.code, ContactRoleV2.O.system, Some(ContactRoleV2.O.display))
    }
  }

  /**
   * Define a prioridade (rank) do primeiro contacto telefónico do paciente caso nenhum rank esteja definido.
   *
   * @param patient Recurso FHIR Patient cujos contactos (telecom) serão verificados e atualizados.
   * @return O objeto Patient atualizado com o rank definido para o primeiro contacto disponível.
   */

  private def buildPatientContactRank(patient: Patient): Patient = {
    Option(patient.getTelecom)
      .filter(_.asScala.exists(_.hasRank))
      .orElse {
        Option(patient.getContactFirstRep)
          .filter(_.hasTelecom)
          .flatMap(contact => contact.getTelecom.asScala.headOption)
          .filter(telecom => telecom.hasValue)
          .map { firstTelecom =>
            firstTelecom.setRank(1)
            firstTelecom
          }
      }
    patient
  }

  /**
   * Constrói um objeto HumanName a partir dos dados do campo XPN do HL7, incluindo apelido, nome próprio e nomes intermédios.
   *
   * @param name Campo XPN (Extended Person Name) contendo os componentes do nome do paciente.
   * @return Um Option[HumanName] com o nome completo formatado e o tipo de uso (OFFICIAL, se aplicável), ou None se não houver dados válidos.
   */
  private def buildPatientHumanName(name: XPN): Option[HumanName] = {
    if (!name.getGivenName.isEmpty || !name.getFamilyName.isEmpty) {
      val familyName = Option(name.getFamilyName.getFn1_Surname.getValue)
      val givenName = Option(name.getGivenName.getValue)
      val middleNames = Option(name.getSecondAndFurtherGivenNamesOrInitialsThereof.getValue).map(_.split(" "))
      val nameUse = name.getXpn7_NameTypeCode.getValue match {
        case "L" => Some(HumanName.NameUse.OFFICIAL)
        case _ => None
      }
      Some(buildHumanName(familyName, givenName, middleNames, nameUse))
    }
    else None
  }

  /**
   * Define o valor do contacto (telecom) com base nas informações do XTN, priorizando o número de telefone e validando o e-mail.
   *
   * @param contact Campo XTN (Extended Telecommunication Number) contendo os dados de telefone e e-mail.
   * @param telecom Objeto ContactPoint FHIR que será preenchido com o valor correspondente.
   * @return O ContactPoint atualizado com o número de telefone ou endereço de e-mail válido.
   */
  private def setTelecomValue(contact: XTN, telecom: ContactPoint): ContactPoint = {
    if (!contact.getXtn12_UnformattedTelephoneNumber.isEmpty)
      telecom.setValue(contact.getXtn12_UnformattedTelephoneNumber.getValue)
    else if (!contact.getEmailAddress.isEmpty && emailIsValid(contact.getEmailAddress.getValue)) {
      telecom.setValue(contact.getEmailAddress.getValue)
    }
    telecom
  }

  /**
   * Define o tipo de sistema de contacto no objeto ContactPoint com base no código do equipamento.
   *
   * @param equipType Código do tipo de equipamento de telecomunicação.
   * @param telecom   Objeto ContactPoint FHIR que será atualizado com o sistema correspondente.
   * @return O ContactPoint atualizado com o tipo de sistema apropriado (PHONE, EMAIL, FAX ou OTHER).
   */

  private def getTelecomSystem(equipType: String, telecom: ContactPoint): ContactPoint = {
    equipType match {
      case TelecomEquipmentType.PH.code => telecom.setSystem(ContactPoint.ContactPointSystem.PHONE)
      case TelecomEquipmentType.CP.code => telecom.setSystem(ContactPoint.ContactPointSystem.PHONE)
      case TelecomEquipmentType.X400.code => telecom.setSystem(ContactPoint.ContactPointSystem.EMAIL)
      case TelecomEquipmentType.FX.code => telecom.setSystem(ContactPoint.ContactPointSystem.FAX)
      case _ => telecom.setSystem(ContactPoint.ContactPointSystem.OTHER)
    }
    telecom
  }

  /**
   * Define o tipo de uso (use) do contacto — por exemplo, pessoal, profissional ou móvel — com base nos códigos HL7 de uso e equipamento.
   *
   * @param useCode   Código de uso do contacto proveniente do HL7.
   * @param telSystem Código do tipo de equipamento de telecomunicação associado ao contacto.
   * @param telecom   Objeto ContactPoint FHIR que será atualizado com o tipo de uso apropriado.
   * @return O ContactPoint atualizado com o campo `use` definido conforme o tipo de contacto.
   */
  private def getUseCode(useCode: String, telSystem: String, telecom: ContactPoint): ContactPoint = {
    useCode match {
      case TelecomUseCode.PRN.code =>
        telSystem match {
          case TelecomEquipmentType.CP.code => telecom.setUse(ContactPoint.ContactPointUse.MOBILE)
          case _ => telecom.setUse(ContactPoint.ContactPointUse.HOME)
        }
      case TelecomUseCode.WPN.code => telecom.setUse(ContactPoint.ContactPointUse.WORK)
      case TelecomUseCode.EMR.code => telecom.setUse(ContactPoint.ContactPointUse.MOBILE)
      case _ =>
    }
    telecom
  }

  /**
   * Mapeia o campo XAD (HL7) para um objeto Address FHIR, convertendo e normalizando os campos de morada, código postal e país.
   *
   * @param address   Campo XAD contendo as informações de endereço do paciente ou contacto.
   * @param isFromPid Indica se o endereço provém do segmento PID (true) ou de outro segmento (false).
   * @return Objeto Address FHIR preenchido com os dados mapeados e normalizados.
   */
  private def mapXadToAddress(address: XAD, isFromPid: Boolean): Address = {
    val addressElement: Address = createAddress(address, isFromPid)

    Option(address.getStreetAddress.getStreetOrMailingAddress.getValue).foreach(addressElement.addLine)
    Option(address.getOtherDesignation.getValue).foreach(addressElement.addLine)
    Option(address.getCity.getValue).foreach(addressElement.setCity)
    Option(address.getStateOrProvince.getValue).foreach(addressElement.setDistrict)
    Option(address.getZipOrPostalCode.getValue).foreach(addressElement.setPostalCode)
    Option(address.getCountry.getValue).map {
      case "PRT" => "PT"
      case other => other
    }.foreach(addressElement.setCountry)
    Option(address.getAddressType.getValue).filter(_ == CURRENT.code).foreach(_ => addressElement.setType(Address.AddressType.POSTAL))

    addressElement
  }

  /**
   * Cria e configura um objeto Address FHIR a partir do campo XAD, definindo o tipo, uso e extensões conforme o código de endereço.
   *
   * @param address   Campo XAD (HL7) contendo as informações do tipo e localização do endereço.
   * @param isFromPid Indica se o endereço provém do segmento PID (true) ou de outro segmento (false), afetando o uso padrão atribuído.
   * @return Objeto Address configurado com tipo, uso e extensões geográficas apropriadas.
   */
  private def createAddress(address: XAD, isFromPid: Boolean): Address = {
    val addressElement = new Address()
    val addressType = address.getAddressType.getValue
    val geographic = address.getXad9_CountyParishCode.getValue
    addressType match {
      case CURRENT.code =>
        addressElement.setType(Address.AddressType.POSTAL)
        addExtensionToAddress(addressElement, geographic, CURRENT.code)
      case OFFICE.code =>
        addressElement.setUse(Address.AddressUse.WORK)
        addressElement.setType(Address.AddressType.BOTH)
        addExtensionToAddress(addressElement, geographic, OFFICE.code)
      case BIRTH_NOTSPECIFIED.code | MAIN_ADDRESS.code | MAILING.code =>
        addressElement.setUse(Address.AddressUse.HOME)
        addressElement.setType(Address.AddressType.BOTH)
        addExtensionToAddress(addressElement, geographic, MAIN_ADDRESS.code)
      case _ =>
        if (isFromPid) {
          addressElement.setUse(Address.AddressUse.HOME)
          addressElement.setType(Address.AddressType.BOTH)
          addExtensionToAddress(addressElement, geographic, MAIN_ADDRESS.code)
        }
    }
    addressElement
  }

  /**
   * Adiciona extensões FHIR ao endereço, incluindo códigos geográficos (município, freguesia, etc.) com base no tipo de endereço.
   *
   * @param addressElement Objeto Address FHIR ao qual as extensões serão adicionadas.
   * @param geographic     Código geográfico obtido do campo XAD.
   * @param typeAddress    Tipo de endereço utilizado para criar a extensão.
   */
  private def addExtensionToAddress(addressElement: Address, geographic: String, typeAddress: String): Unit = {
    Option(geographic).foreach { geo =>
      val addressExtension = createAddressExtension(typeAddress)
      addressExtension.setUrl(EXTENSION_ADDRESS.uri)
      addAddressCodesExtension(geo, addressExtension)
      addressElement.addExtension(addressExtension)
    }
  }

  /**
   * Cria uma extensão FHIR para o endereço, adicionando um subcampo ADDRESS_TYPE com o código e descrição correspondentes ao tipo de endereço.
   *
   * @param code Código do tipo de endereço utilizado para obter o mapeamento e construir a extensão.
   */
  private def createAddressExtension(code: String): Extension = {
    val addressExtension = new Extension()
    fromAddressTypeCode(code).foreach(
      addressTypeCode => addressExtension.addExtension(
        ADDRESS_TYPE.uri,
        new Coding()
          .setSystem(addressTypeCode.system)
          .setCode(addressTypeCode.code)
          .setDisplay(addressTypeCode.description))
    )
    addressExtension
  }

  /**
   * Adiciona extensões hierárquicas de localização (distrito, município e freguesia) ao endereço com base no código geográfico.
   *
   * @param geographic       Código geográfico completo (parish) utilizado para derivar os níveis administrativos.
   * @param addressExtension Extensão FHIR do endereço onde os códigos serão adicionados.
   */
  private def addAddressCodesExtension(geographic: String, addressExtension: Extension): Extension = {
    val parish: String = geographic
    val county: String = parish.take(2)
    val municipality: String = parish.take(4)
    addressExtension.addExtension(COUNTY.uri, buildCodeableConcept(List(buildCoding(county, locationSystem))))
    addressExtension.addExtension(MUNICIPALITY.uri, buildCodeableConcept(List(buildCoding(municipality, locationSystem))))
    addressExtension.addExtension(PARISH.uri, buildCodeableConcept(List(buildCoding(parish, locationSystem))))
    addressExtension
  }

  /**
   * Constrói o objeto Meta do recurso Patient, definindo a data de última atualização e, quando aplicável, o nível de confidencialidade.
   *
   * @param evn Segmento EVN opcional que contém informações sobre o evento e data de atualização do registo do paciente.
   */
  private def buildPatientMeta(evn: Option[EVN] = None): Meta = {
    val meta = new Meta
    evn match {
      case Some(event) =>
        //TODO: Validate dateTime existence in evn2
        val updateTime = evn.map(event => getDateConverted25ToFhir(event.getRecordedDateTime.getTime.getValue))
        updateTime.map(dateTime => meta.setLastUpdated(dateTime))
        Option(event.getEvn1_EventTypeCode.getValue).map {
          case "A40" | "A45" =>
            //TODO: define enum for confidenciality code system
            val securityCoding = buildCoding("NORMAL.system", "NORMAL.code", Some("NORMAL.description"))
            meta.addSecurity(securityCoding)
          case _ =>
        }
      case None =>
    }
    meta
  }

  /**
   * Adiciona ao recurso Patient os identificadores provenientes do segmento PID, mapeando código, autoridade emissora e tipo de identificador.
   *
   * @param patient Recurso FHIR Patient que receberá os identificadores.
   * @param pid     Segmento HL7 PID contendo a lista de identificadores (CX) do paciente.
   */
  private def buildPatientIdentifiersToPatientResource(patient: Patient, pid: PID): Patient = {
    val identifierList: Array[CX] = pid.getPatientIdentifierList
    identifierList.foreach { identifier =>
      patient.addIdentifier(
        buildIdentifier(identifier.getCx1_IDNumber.toString, Option(identifier.getCx4_AssigningAuthority.getNamespaceID.getValue),
          None, getIdentifierType(identifier.getCx5_IdentifierTypeCode.getValue)
        ))
    }
    patient
  }

  /**
   * Mapeia o código de identificador HL7 para o tipo de identificador FHIR correspondente.
   *
   * @param value Código do tipo de identificador proveniente do campo IdentifierTypeCode do HL7.
   */
  private def getIdentifierType(value: String): Option[CodeableConcept] = {
    value match {
      case NS.code => Some(buildCodeableConcept(List(buildCoding(PI.code, PI.system, Some(PI.display))), Some(PI.text)))
      case SNS.code => Some(buildCodeableConcept(List(buildCoding(HC.code, HC.system, Some(HC.display))), Some(HC.text)))
      case B.code => Some(buildCodeableConcept(List(buildCoding(CZ.code, CZ.system, Some(CZ.display))), Some(CZ.text)))
      case NIF.code => Some(buildCodeableConcept(List(buildCoding(TAX.code, TAX.system, Some(TAX.display))), Some(TAX.text)))
      case NISS.code => Some(buildCodeableConcept(List(buildCoding(SS.code, SS.system, Some(SS.display))), Some(SS.text)))
      case P.code => Some(buildCodeableConcept(List(buildCoding(PPN.code, PPN.system, Some(PPN.display))), Some(PPN.text)))
      case C.code => Some(buildCodeableConcept(List(buildCoding(BCFN.code, BCFN.system, Some(BCFN.display))), Some(BCFN.text)))
      case PRC.code => Some(buildCodeableConcept(List(buildCoding(PRC.code, PRC.system, Some(PRC.display))), Some(PRC.text)))
      case _ => None
    }
  }

  /**
   * Adiciona ao Patient o identificador correspondente ao número de processo clínico (Medical Record Number) presente no PID.
   *
   * @param patient Recurso FHIR Patient que receberá o identificador do processo clínico.
   * @param pid     Segmento HL7 PID contendo o número de conta ou processo do paciente (PatientAccountNumber).
   */
  private def buildMedicalProcessNumberPatientIdentifier(patient: Patient, pid: PID): Patient = {
    val processNumber: Option[String] = Option(pid.getPatientAccountNumber.getIDNumber.getValue)
    val system: Option[String] = Option(pid.getPatientAccountNumber.getAssigningAuthority.getNamespaceID.getValue)
    processNumber.foreach { (pn: String) =>
      val identifierTypeCode: Coding = buildCoding(IdentifierType.MR.code, IdentifierType.MR.system, Some(IdentifierType.MR.display))
      val identifierType: CodeableConcept = buildCodeableConcept(List(identifierTypeCode), Some(IdentifierType.MR.text))
      val processIdentifier = buildIdentifier(pn, system, Some(IdentifierUse.USUAL), Some(identifierType))
      patient.addIdentifier(processIdentifier)
    }
    patient
  }

  /**
   * Constrói e adiciona o nome completo (HumanName) ao recurso Patient a partir dos dados do segmento HL7.
   *
   * @param patient Recurso FHIR Patient que será preenchido com o(s) nome(s) do paciente.
   * @param segment Segmento HL7 (geralmente PID) que contém as informações de nome do paciente.
   * @tparam T Tipo genérico do segmento HL7 utilizado.
   */
  private def buildPatientHumanName[T](patient: Patient, segment: T): Patient = {
    val names = segment match {
      case pid: PID => pid.getPatientName()
      case _ => throw new IllegalArgumentException(s"Unsupported segment type: ${segment.getClass.getName}")
    }
    names.foreach(name => buildPatientHumanName(name).foreach(humanName => patient.addName(humanName)))
    patient
  }

  /**
   * Define a data de nascimento do paciente no recurso FHIR Patient.
   *
   * @param patient Recurso FHIR Patient que terá a data de nascimento definida.
   * @param pid     Segmento HL7 PID contendo o campo DateTimeOfBirth (TS) do paciente.
   */
  private def buildPatientBirthDate(patient: Patient, pid: PID): Patient = {
    val birthdateOpt: Option[String] = Option(pid.getDateTimeOfBirth.getTs1_Time.getValue)
    birthdateOpt.map((birthdate: String) =>
      if (birthdate.length.equals(8)) patient.setBirthDate(getDateConverted25ToFhir(birthdate.concat("000000")))
      else patient.setBirthDate(getDateConverted25ToFhir(birthdate))
    )
    patient
  }

  /**
   * Define o sexo administrativo do paciente no recurso FHIR Patient com base no valor do campo HL7 PID-8.
   *
   * @param patient Recurso FHIR Patient que terá o género definido.
   * @param pid     Segmento HL7 PID contendo o campo AdministrativeSex (PID-8) do paciente.
   */
  private def buildPatientGender(patient: Patient, pid: PID): Patient = {
    val genderOpt: Option[String] = Option(pid.getAdministrativeSex.getValue)
    genderOpt.foreach { (gender: String) =>
      val administrativeSex = AdministrativeSex.fromV2Code(gender)
        .getOrElse(throw new IllegalArgumentException(s"Unknown gender: $gender"))
      patient.setGender(Enumerations.AdministrativeGender.fromCode(administrativeSex.fhirCode))
    }
    patient
  }

  /**
   * Constrói e adiciona os endereços do paciente ao recurso FHIR Patient, validando o formato do código postal e mapeando o campo XAD.
   *
   * @param patient Recurso FHIR Patient que será preenchido com os endereços.
   * @param pid     Segmento HL7 PID contendo a lista de endereços (XAD) do paciente.
   */
  private def buildPatientAddresses(patient: Patient, pid: PID): Patient = {
    val addressesArray: Option[Array[XAD]] = Option(pid.getPatientAddress)
    addressesArray match {
      case Some(addresses) =>
        addresses.foreach { (xadAddress: XAD) =>
          xadAddress.getCountry.getValue match {
            case "PRT" | "PT" =>
              val isValidPostalCode = Option(xadAddress.getZipOrPostalCode.getValue).exists(_.matches("\\d{4}-\\d{3}"))
              if (!isValidPostalCode) {
                logger.warn(s"Invalid postal code found: ${xadAddress.getZipOrPostalCode.getValue}")
              } else
                patient.addAddress(mapXadToAddress(xadAddress, isFromPid = true))
            case _ => Option(xadAddress.getZipOrPostalCode.getValue).map {
              case "-" => logger.warn(s"Invalid postal code found: ${xadAddress.getZipOrPostalCode.getValue}")
              case _ => patient.addAddress(mapXadToAddress(xadAddress, isFromPid = true))
            }
          }
        }
      case None =>
    }
    patient
  }

  /**
   * Cria e adiciona a extensão FHIR de local de nascimento (BirthPlace) ao recurso Patient,
   * mapeando os códigos geográficos (país, distrito, município e freguesia) a partir do campo PID-23.
   *
   * @param patient Recurso FHIR Patient que receberá a extensão de local de nascimento.
   * @param pid     Segmento HL7 PID contendo o campo BirthPlace (PID-23) com a informação codificada do local de nascimento.
   */
  private def buildPatientBirthPlaceExtension(patient: Patient, pid: PID): Patient = {
    val birthPlaceOpt: Option[String] = Option(pid.getBirthPlace.getValue)
    birthPlaceOpt.foreach { (birthplace: String) =>
      val birthPlaceExtension = new Extension()
      val birthPlaceLocals: Array[String] = birthplace.split(" ")
      buildCountryExtension(birthPlaceLocals, birthPlaceExtension)
      birthPlaceLocals.length match {
        case 1 =>
        case 2 =>
          val birthPlaceCode: String = birthPlaceLocals(1)
          birthPlaceCode.length match {
            case 6 | 4 =>
              buildCountyExtension(birthPlaceCode, birthPlaceExtension)
              buildMunicipalityExtension(birthPlaceCode, birthPlaceExtension)
              patient.addExtension(birthPlaceExtension)
            case 2 =>
              buildCountyExtension(birthPlaceCode, birthPlaceExtension)
              patient.addExtension(birthPlaceExtension)
          }
        case _ =>
          val birthPlaceCode: String = birthPlaceLocals(2)
          birthPlaceCode.length match {
            case 6 =>
              buildCountyExtension(birthPlaceCode, birthPlaceExtension)
              buildMunicipalityExtension(birthPlaceCode, birthPlaceExtension)
              buildParishExtension(birthPlaceCode, birthPlaceExtension)
              patient.addExtension(birthPlaceExtension)
            case 4 =>
              buildCountyExtension(birthPlaceCode, birthPlaceExtension)
              buildMunicipalityExtension(birthPlaceCode, birthPlaceExtension)
              patient.addExtension(birthPlaceExtension)
            case 2 =>
              buildCountyExtension(birthPlaceCode, birthPlaceExtension)
              patient.addExtension(birthPlaceExtension)
          }
      }
      birthPlaceExtension.setUrl(BIRTH_PLACE.uri)
    }
    patient
  }

  private def buildCountryExtension(birthPlaceLocals: Array[String], birthPlaceExtension: Extension): Unit =
    birthPlaceExtension.addExtension(FhirExtension.COUNTRY.uri,
      buildCodeableConcept(List(buildCoding(birthPlaceLocals(0), locationSystem))))

  private def buildCountyExtension(birthPlaceCode: String, birthPlaceExtension: Extension): Unit =
    birthPlaceExtension
      .addExtension(FhirExtension.COUNTY.uri, buildCodeableConcept(List(buildCoding(birthPlaceCode.take(2), locationSystem))))

  private def buildMunicipalityExtension(birthPlaceCode: String, birthPlaceExtension: Extension): Unit =
    birthPlaceExtension
      .addExtension(FhirExtension.MUNICIPALITY.uri, buildCodeableConcept(List(buildCoding(birthPlaceCode.take(4), locationSystem))))

  private def buildParishExtension(birthPlaceCode: String, birthPlaceExtension: Extension): Unit =
    birthPlaceExtension
      .addExtension(FhirExtension.PARISH.uri, buildCodeableConcept(List(buildCoding(birthPlaceCode, locationSystem))))

  /**
   * Adiciona ao recurso Patient a extensão FHIR de nacionalidade,
   * convertendo os códigos de cidadania (PID-26) em CodeableConcepts FHIR.
   *
   * @param patient Recurso FHIR Patient que será enriquecido com a extensão de nacionalidade.
   * @param pid     Segmento HL7 PID contendo o campo Citizenship (PID-26) com os códigos de nacionalidade do paciente.
   */
  private def buildNationalityExtension(patient: Patient, pid: PID): Patient = {
    val nationality: Array[CE] = pid.getCitizenship
    nationality.toList.foreach { (nat: CE) =>
      val coding = buildCoding(nat.getCe1_Identifier.getValue, locationSystem, Option(nat.getCe2_Text.getValue))
      val nationalityExtension = buildExtension(FhirExtension.NATIONALITY.uri, buildCodeableConcept(List(coding)))
      patient.addExtension(nationalityExtension)
    }
    patient
  }

  /**
   * Define o estado de óbito do paciente no recurso FHIR Patient, com base nos campos PID-29 (data/hora do óbito) e PID-30 (indicador de óbito).
   *
   * @param patient Recurso FHIR Patient que terá o estado de óbito definido.
   * @param pid     Segmento HL7 PID contendo as informações de data e indicador de óbito do paciente.
   */
  private def buildPatientDeceasedStatus(patient: Patient, pid: PID): Patient = {
    val deceaseDate: Option[String] = Option(pid.getPatientDeathDateAndTime.getTime.getValue)
    deceaseDate.map(date => patient.setDeceased(new DateTimeType(getDateConverted25ToFhir(date))))
    val deathIndicator: Option[String] = Option(pid.getPatientDeathIndicator.getValue)
    deathIndicator match {
      case Some("Y") => patient.setDeceased(new BooleanType(true))
      case Some("N") => patient.setDeceased(new BooleanType(false))
      case _ => patient
    }
  }

  /**
   * Adiciona informações de contacto telefónico ao recurso FHIR Patient,
   * atribuindo prioridade (rank) ao primeiro contacto válido.
   *
   * @param patient Recurso FHIR Patient que será preenchido com os contactos telefónicos.
   * @param pid     Segmento HL7 PID contendo os campos de telefone residencial (PID-13) e profissional (PID-14).
   */
  private def buildPatientTelecomInfo(patient: Patient, pid: PID): Patient = {
    val homePhone = pid.getPhoneNumberHome
    homePhone.foreach(contact => addTelecomInfoToPatient(patient, contact))
    val businessPhone = pid.getPhoneNumberBusiness
    homePhone.foreach(contact => addTelecomInfoToPatient(patient, contact))
    Option(patient.getTelecomFirstRep)
      .filter(telecom => telecom.hasSystem || telecom.hasValue)
      .map(_.setRank(1))
    patient
  }

  /**
   * Cria e adiciona um contacto (telecom) ao recurso FHIR Patient,
   * definindo o valor, o sistema e o tipo de uso (pessoal, profissional, móvel).
   *
   * @param patient Recurso FHIR Patient ao qual o contacto será adicionado.
   * @param contact Campo XTN (HL7) contendo as informações de telefone e/ou e-mail do paciente.
   */
  private def addTelecomInfoToPatient(patient: Patient, contact: XTN): Patient = {
    val telecom = new ContactPoint
    if (!contact.getXtn12_UnformattedTelephoneNumber.isEmpty) {
      telecom.setValue(contact.getXtn12_UnformattedTelephoneNumber.getValue)
    } else if (!contact.getEmailAddress.isEmpty && emailIsValid(contact.getEmailAddress.getValue))
      telecom.setValue(contact.getEmailAddress.getValue)
    Option(telecom.getValue) match {
      case Some(_) =>
        getTelecomSystem(contact.getTelecommunicationEquipmentType.getValue, telecom)
        getUseCode(contact.getTelecommunicationUseCode.getValue, contact.getTelecommunicationEquipmentType.getValue, telecom)
      case None =>
    }
    patient
  }

  /**
   * Define o estado civil do paciente no recurso FHIR Patient, com base no código recebido no campo PID-16 (Marital Status).
   *
   * @param patient Recurso FHIR Patient que terá o estado civil definido.
   * @param pid     Segmento HL7 PID contendo o campo MaritalStatus (PID-16) do paciente.
   */
  private def buildPatientMaritalStatus(patient: Patient, pid: PID): Patient = {
    val maritalStatusCode: Option[MaritalStatus] = MaritalStatus.fromCode(pid.getPid16_MaritalStatus.getCe1_Identifier.getValue)
    maritalStatusCode.foreach { (code: MaritalStatus) =>
      val maritalStatusCoding: Coding = buildCoding(code.code, code.system, Some(code.display))
      val msCodeableConcept: CodeableConcept = buildCodeableConcept(List(maritalStatusCoding))
      patient.setMaritalStatus(msCodeableConcept)
    }
    patient
  }

  /**
   * Define o profissional de saúde responsável e a organização associada ao paciente no recurso FHIR Patient.
   *
   * @param patient Recurso FHIR Patient que receberá as referências ao Practitioner e à Organization.
   * @param rol     Segmento HL7 ROL contendo informações sobre o profissional responsável; se vazio, nenhuma referência é adicionada.
   */
  private def buildGeneralPractitioner(patient: Patient, rol: ROL): Patient = {
    if (!rol.isEmpty && rol.getRol3_RoleROL.getIdentifier.getValue.equals("FHCP")) {
      val id = buildIfFromIdentifier(rol).getOrElse(buildIdFromName(rol))
      val practitionerReference = s"${ResourceType.Practitioner.toString}/$id"
      patient.addGeneralPractitioner(buildReference(practitionerReference))
      buildCSPHealthcareOrganizationId(rol).map { id =>
        val organizationReference = s"${ResourceType.Organization.toString}/$id"
        patient.addGeneralPractitioner(buildReference(organizationReference))
      }
    }
    patient
  }

  private def buildManagingOrganization(patient: Patient, organization: String): Patient = {
    val organizationId = UUID.nameUUIDFromBytes(organization.getBytes).toString.toLowerCase
    patient.setManagingOrganization(buildReference(s"${ResourceType.Organization.toString}/$organizationId"))
  }

  private def buildPatientObservations(patient: Patient, obxList: List[OBX]): Patient = {
    obxList.foreach(patientNotes => buildPatientNotesExtension(patient, patientNotes))
    patient
  }

  private def buildCSPHealthcareOrganizationId(rol: ROL): Option[String] = {
    Option(rol.getOrganizationUnitType.getIdentifier.getValue).map(code =>
      UUID.nameUUIDFromBytes(code.getBytes()).toString.toLowerCase())
  }

  private def buildIfFromIdentifier(rol: ROL) = {
    rol.getRol4_RolePerson.toList.headOption
      .flatMap(person => Option(person.getIDNumber.getValue).map(code =>
        UUID.nameUUIDFromBytes(code.getBytes()).toString.toLowerCase)
      )
  }

  private def buildIdFromName(rol: ROL): String = {
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
   * Cria e adiciona ao recurso Patient a extensão FHIR de notas clínicas (PatientNotes),
   * convertendo o conteúdo do campo OBX-5 e a data da observação (OBX-14) em uma anotação FHIR.
   *
   * @param patient      Recurso FHIR Patient que receberá a extensão de notas clínicas.
   * @param patientNotes Segmento HL7 OBX contendo o texto e a data das observações clínicas associadas ao paciente.
   */
  private def buildPatientNotesExtension(patient: Patient, patientNotes: OBX): Patient = {
    if (patientNotes.getObx5_ObservationValue.length > 0) {
      val uri = FhirExtension.PATIENTNOTES.uri
      val annotation = new Annotation()
      annotation.setText(patientNotes.getObx5_ObservationValue.head.getData.asInstanceOf[ST].toString)
      val timeOpt = Option(patientNotes.getObx14_DateTimeOfTheObservation.getTs1_Time.getValue)
      timeOpt match {
        case Some(time) => annotation.setTimeElement(new DateTimeType(getDateConverted25ToFhir(time)))
        case None =>
      }
      val patientNotesExtension = buildExtension(uri, annotation)
      patient.addExtension(patientNotesExtension)
    }
    patient
  }

}
