package utils

import org.hl7.fhir.r5.model.Coverage.CoveragePaymentByComponent
import org.hl7.fhir.r5.model.HumanName.NameUse
import org.hl7.fhir.r5.model.Identifier.IdentifierUse
import org.hl7.fhir.r5.model.{Annotation, CodeableConcept, Coding, Extension, HumanName, Identifier, Reference}

object BaseFhirUtils {
  /**
   * Constrói um objeto CodeableConcept adicionando uma lista de codificações e, opcionalmente, um texto descritivo.
   *
   * @param codingsList Lista de objetos Coding que representam os códigos a serem adicionados ao CodeableConcept.
   * @param textOpt     Texto opcional que descreve o conceito de forma legível.
   */
  def buildCodeableConcept(codingsList: List[Coding], textOpt: Option[String] = None): CodeableConcept = {
    val codeableConcept = new CodeableConcept
    codingsList.foreach(coding => codeableConcept.addCoding(coding))
    textOpt.foreach(text => codeableConcept.setText(text))
    codeableConcept
  }

  /**
   * Constrói um objeto Coding que representa uma codificação FHIR com código, sistema e opcionalmente um texto descritivo.
   *
   * @param code       Código do conceito a ser representado.
   * @param system     Sistema de codificação ao qual o código pertence.
   * @param displayOpt Texto opcional que descreve o código de forma legível.
   */
  def buildCoding(code: String, system: String, displayOpt: Option[String] = None): Coding = {
    val coding = new Coding().setCode(code)
      .setSystem(system)
    displayOpt.foreach(display => coding.setDisplay(display))
    coding
  }

  /**
   * Constrói um objeto Identifier que representa um identificador FHIR com valor, sistema, tipo e uso opcionais.
   *
   * @param value             Valor do identificador.
   * @param systemOpt         Sistema opcional que emite o identificador.
   * @param useOpt            Uso opcional que define o propósito do identificador.
   * @param identifierTypeOpt Tipo opcional do identificador, representado por um CodeableConcept.
   */
  def buildIdentifier(value: String, systemOpt: Option[String], useOpt: Option[IdentifierUse], identifierTypeOpt: Option[CodeableConcept] = None): Identifier = {
    val identifier: Identifier = new Identifier
    identifier.setValue(value)
    systemOpt.foreach(system => identifier.setSystem(system))
    identifierTypeOpt.foreach(identifierType => identifier.setType(identifierType))
    useOpt.foreach(use => identifier.setUse(use))
    identifier
  }

  /**
   * Constrói um objeto Reference que aponta para outro recurso FHIR através do seu identificador ou caminho.
   *
   * @param reference String que identifica o recurso referenciado.
   */

  def buildReference(reference: String): Reference =
    new Reference(reference)

  /**
   * Constrói um objeto Extension que adiciona informações personalizadas a um recurso FHIR.
   *
   * @param uri   URI que define o significado e a estrutura da extensão.
   * @param value Valor da extensão.
   */

  def buildExtension(uri: String, value: Any): Extension = {
    val extension = new Extension()
    extension.setUrl(uri)
    value match {
      case annotation: Annotation => extension.setValue(annotation)
      case codeableConcept: CodeableConcept => extension.setValue(codeableConcept)
      case _ =>
    }
    extension
  }

  /**
   * Constrói um objeto HumanName que representa o nome de uma pessoa segundo o padrão FHIR.
   *
   * @param familyName  Sobrenome ou apelido da pessoa.
   * @param givenName   Primeiro nome da pessoa.
   * @param middleNames Nomes intermédios opcionais a serem adicionados ao nome completo.
   * @param nameUse     Uso opcional do nome, indicando o seu contexto.
   */

  def buildHumanName(familyName: Option[String], givenName: Option[String], middleNames: Option[Array[String]], nameUse: Option[NameUse]): HumanName = {
    val humanName = new HumanName()
    familyName.map(name => humanName.setFamily(name))
    givenName.map(name => humanName.addGiven(name))
    middleNames.foreach(names => names.foreach((name: String) => humanName.addGiven(name)))
    nameUse.map(use => humanName.setUse(use))
    humanName
  }

  def buildCoveragePaymentByComponent(reference: Reference): CoveragePaymentByComponent =
    new CoveragePaymentByComponent().setParty(reference)

}
