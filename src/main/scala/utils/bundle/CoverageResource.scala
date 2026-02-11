package utils.bundle

import ca.uhn.hl7v2.model.v25.segment.IN1
import org.apache.pekko.event.slf4j.Logger
import org.hl7.fhir.r5.model.Coverage.CoveragePaymentByComponent
import org.hl7.fhir.r5.model.Identifier.IdentifierUse
import org.hl7.fhir.r5.model.{Coverage, Organization, ResourceType}
import org.slf4j.Logger as SLFLogger
import utils.BaseFhirUtils.{buildCoveragePaymentByComponent, buildIdentifier, buildReference}
import utils.BaseUtils

import java.util.UUID

object CoverageResource extends BaseUtils:
  val logger: SLFLogger = Logger("Coverage Resource")

  protected[bundle] def buildCoverage(in1: IN1, patientId: String): Coverage = {
    buildBaseCoverage(in1, patientId)
  }

  /**
   * Constrói um objeto base do tipo Coverage.
   *
   * @param in1 segmento Insurance da mensagem HL7 v2.
   * @return Um objeto Coverage inicializado com ID, nome, estado ativo e identificador.
   */
  private def buildBaseCoverage(in1: IN1, patientId: String): Coverage = {
    val coverage = new Coverage()
    Option(in1.getIn12_InsurancePlanID.getIdentifier.getValue).map(planId =>
      coverage.addPaymentBy(buildPaymentByPartyComponent(planId))
    )
    coverage.setBeneficiary(buildReference(s"${ResourceType.Patient.toString}/$patientId"))
    coverage
  }

  private def buildPaymentByPartyComponent(id: String): CoveragePaymentByComponent = {
    val organizationId = UUID.nameUUIDFromBytes(id.getBytes).toString.toLowerCase
    val partyReference = buildReference(s"${ResourceType.Organization.toString}/$organizationId")
    buildCoveragePaymentByComponent(partyReference)
  }

