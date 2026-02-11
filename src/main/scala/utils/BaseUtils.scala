package utils

import ca.uhn.fhir.context.FhirContext
import org.hl7.fhir.r5.model.Bundle.BundleEntryComponent
import org.hl7.fhir.r5.model.{Bundle, Patient, ResourceType}

import java.nio.charset.Charset
import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.matching.Regex

trait BaseUtils:
  /**
   * Substitui sequências hexadecimais escapadas (\X...\) em uma mensagem HL7 pelos seus caracteres correspondentes.
   *
   * @param message Mensagem HL7 original que pode conter caracteres escapados em formato hexadecimal.
   * @param charset Conjunto de caracteres (charset) utilizado para decodificar os bytes convertidos do valor hexadecimal.
   */

  def replaceEscapedHexChars(message: String, charset: String): String = {
    val hexPattern = """\\X([0-9A-Fa-f]{2,})\\""".r
    hexPattern.replaceAllIn(message, m => {
      val hex = m.group(1)
      val bytes = hex.grouped(2).map(h => Integer.parseInt(h, 16).toByte).toArray
      new String(bytes, Charset.forName(charset))
    })
  }

  /**
   * Verifica se a mensagem HL7 recebida é do tipo ACK (Acknowledgment Message).
   *
   * @param message Mensagem HL7 completa representada como uma string, separada por delimitadores de campo ("|").
   */

  def isAckMessage(message: String): Boolean = message.split("\\|")(8).split("\\^")(0).equals("ACK")

  /**
   * Converte um objeto FHIR Bundle em sua representação textual no formato JSON.
   *
   * @param bundle  Objeto Bundle FHIR que será convertido em string.
   * @param fhirCtx Contexto FHIR utilizado para criar o parser responsável pela serialização em JSON.
   */

  def parseFhirToString(bundle: Bundle)(using fhirCtx: FhirContext): String = {
    val parser = fhirCtx.newJsonParser()
    parser.encodeToString(bundle)
  }

  /**
   * Verifica se uma string fornecida corresponde a um formato de e-mail válido.
   *
   * @param getValue String a ser validada como endereço de e-mail.
   */

  def emailIsValid(getValue: String): Boolean = {
    val emailRegex: Regex = "^[\\w\\.-]+@[\\w\\.-]+\\.[a-zA-Z]{2,}$".r
    Option(getValue)
      .map(_.trim)
      .filter(_.nonEmpty)
      .exists(emailRegex.pattern.matcher(_).matches())
  }

  def extractPatientReferenceId(bundle: Bundle): String = {
    extractPatientResource(bundle).getId
  }

  private def extractPatientResource(bundle: Bundle): Patient = {
    extractResourceFromBundle(bundle, ResourceType.Patient) match {
      case Some(entry) => entry.getResource.asInstanceOf[Patient]
      case None => throw new Exception(s"Patient resource not exists in fhir message ${bundle.getId}")
    }
  }

  private def extractResourceFromBundle(fhirMsg: Bundle, resourceType: ResourceType): Option[BundleEntryComponent] = {
    extractResourceList(fhirMsg, resourceType.toString).headOption
  }

  private def extractResourceList(fhirMsg: Bundle, resourceType: String): List[BundleEntryComponent] = {
    fhirMsg.getEntry.asScala.toList
      .filter(entry => entry.getResource.getResourceType.toString.equals(resourceType))
  }

object BaseUtils:
  /**
   * Remove quebras de linha e espaços desnecessários de uma string.
   *
   * @param result String de entrada que pode conter caracteres de nova linha (\n, \r\n, \r).
   */
  def removeSpecialCharacters(result: String): String =
    result.replaceAll("\\n|\\r\\n|\\r", "").trim

  /**
   * Remove barras invertidas de escape (\") de uma string, substituindo-as por aspas normais (").
   *
   * @param result String de entrada que contém caracteres de escape.
   */
  def removeBackSlashCharacter(result: String): String =
    result.replace("\\\"", "\"")
