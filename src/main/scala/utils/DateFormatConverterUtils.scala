package utils

import java.text.SimpleDateFormat
import java.time.{LocalDate, ZonedDateTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.util.Date

object DateFormatConverterUtils {
  private val dateFormat = "yyyy-MM-dd"
  private val hl7DateTimeFormat = "yyyyMMddHHmmss"
  private val hl7DateFormat = "yyyyMMdd"
  private val zonedDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss"

  /**
   * Cria um formatador de datas com o padrão especificado.
   *
   * @param format String que define o padrão de formatação da data (por exemplo, "yyyy-MM-dd" ou "dd/MM/yyyy").
   */
  
  private def dateFormatter(format: String): DateTimeFormatter = DateTimeFormatter.ofPattern(format)

  /**
   * Converte uma data representada como string em um objeto LocalDate, utilizando o formato de data definido.
   *
   * @param date String que representa a data a ser convertida.
   * @throws RuntimeException Se ocorrer um erro ao tentar fazer o parsing da string.
   */
  
  def stringToLocalDate(date: String): LocalDate = {
    try {
      LocalDate.parse(date, dateFormatter(dateFormat))
    } catch {
      case ex: DateTimeParseException =>
        throw new RuntimeException("Error on converting a string to LocalDate: " + ex.getMessage)
    }
  }

  /**
   * Converte uma string em um objeto ZonedDateTime.
   *
   * @param date String que representa a data e hora com fuso horário.
   * @throws RuntimeException Se ocorrer um erro ao tentar converter a string para ZonedDateTime.
   */

  def stringToZonedDateTime(date: String): ZonedDateTime = {
    try {
      ZonedDateTime.parse(date)
    } catch {
      case ex: DateTimeParseException =>
        throw new RuntimeException("Error on converting a string to LocalDate: " + ex.getMessage)
    }
  }

  /**
   * Converte um objeto Date em uma string formatada de acordo com o padrão de data definido.
   *
   * @param date Objeto Date que será convertido em string.
   */

  def dateToString(date: Date): String = {
    val format = new SimpleDateFormat(dateFormat)
    format.format(date)
  }

  /**
   * Converte um objeto LocalDate em uma string formatada de acordo com o padrão de data definido.
   *
   * @param date Objeto LocalDate que será convertido em string.
   */
  
  def localDateToString(date: LocalDate): String =
    dateFormatter(dateFormat).format(date)

  /**
   * Converte um objeto ZonedDateTime em uma string formatada de acordo com o padrão de data e hora definido.
   *
   * @param date Objeto ZonedDateTime que será convertido em string.
   */

  def zonedDateTimeToString(date: ZonedDateTime): String =
    date.format(dateFormatter(zonedDateTimeFormat))

  /**
   * Converte uma data no formato HL7 v2 para o formato FHIR.
   *
   * @param dateString String que representa a data no formato HL7 v2.
   */
  
  def getDateConverted25ToFhir(dateString: String): Date = {
    val hl7v2DateFormat = new SimpleDateFormat(hl7DateTimeFormat)
    val fhirDateFormat = new SimpleDateFormat(dateFormat)
    try {
      val date: Date = hl7v2DateFormat.parse(dateString)
      val fhirDateStr: String = fhirDateFormat.format(date)
      fhirDateFormat.parse(fhirDateStr)
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Error on parse date ${e.getMessage}.")
    }
  }

}
