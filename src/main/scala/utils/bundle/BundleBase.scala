package utils.bundle

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.util.BundleBuilder
import ca.uhn.hl7v2.model.v25.segment.{EVN, IN1, NK1, OBX, PID, ROL}
import org.hl7.fhir.r5.model.Bundle.{BundleEntryComponent, BundleEntryRequestComponent, BundleType, HTTPVerb}
import org.hl7.fhir.r5.model.{Bundle, Coverage, Organization, Patient, Practitioner, ResourceType}
import utils.bundle.CoverageResource.buildCoverage
import utils.bundle.OrganizationResource.buildOrganization
import utils.bundle.PatientResource.{buildPatient, extractPatientReferenceId}
import utils.bundle.PractitionerResource.buildPractitioner

import java.util.{Date, UUID}

object BundleBase:

  extension (bundle: Bundle) {

    def withBundleBasics(mshId: String): Bundle =
      bundle.withBundleTypeAndId(mshId.toLowerCase())
      bundle.setTimestamp(new Date())
      bundle

    private def withBundleTypeAndId(
                                     mshId: String,
                                     bundleType: BundleType = BundleType.TRANSACTION
                                   ): Bundle = {
      bundle.setType(bundleType)
      bundle.setId(mshId.toLowerCase())
      bundle
    }

    def withPatient(organization: String, env: EVN, pid: PID, obxList: List[OBX], rol: ROL, nk1List: List[NK1], method: HTTPVerb): Bundle = {
      val patient = buildPatient(organization, env, pid, obxList, rol, nk1List)
      bundle.addEntry(new BundleEntryComponent().setResource(patient).setRequest(request(patient, method)))
    }

    def withOrganization(code: String, name: Option[String], method: HTTPVerb): Bundle = {
      val organization = buildOrganization(code, name)
      bundle.addEntry(new BundleEntryComponent().setResource(organization).setRequest(request(organization, method)))
    }

    def withPractitioner(rol: ROL, method: HTTPVerb): Bundle = {
      val practitioner = buildPractitioner(rol)
      bundle.addEntry(new BundleEntryComponent().setResource(practitioner).setRequest(request(practitioner, method)))
    }

    def withCoverage(in1: IN1, method: HTTPVerb): Bundle ={
      val patientId = extractPatientReferenceId(bundle)
      val coverage = buildCoverage(in1, patientId)
      bundle.addEntry(new BundleEntryComponent().setResource(coverage).setRequest(request(coverage, method)))
    }

    private def request(resource: Any, method: HTTPVerb): BundleEntryRequestComponent = {
      val urlResource = resource match {
        case patient: Patient =>
          val resource = ResourceType.Patient.toString
          val condition = s"$resource?identifier=SONHO|${patient.getIdentifierFirstRep.getValue}"
          buildRequest(method, resource, condition)
        case organization: Organization =>
          val resource = ResourceType.Organization.toString
          val condition = s"$resource?identifier=SONHO|${organization.getIdentifierFirstRep.getValue}"
          buildRequest(method, resource, condition)
        case practitioner: Practitioner =>
          val resource = ResourceType.Practitioner.toString
          val condition = s"$resource?name=${practitioner.getNameFirstRep.getFamily}"
          buildRequest(method, resource, condition)
        case coverage: Coverage =>
          val resource = ResourceType.Coverage.toString
          val condition = s"$resource?name=${coverage.getBeneficiary.getReference.split("/").last}"
          buildRequest(method, resource, condition)
      }
      urlResource
    }

    private def buildRequest(method: HTTPVerb, resource: String, condition: String): BundleEntryRequestComponent =
      new Bundle.BundleEntryRequestComponent()
        .setMethod(method)
        .setUrl("Patient")
        .setIfNoneExist(condition)
  }

  def newBundle(using fhirCtx: FhirContext): Bundle =
    new BundleBuilder(fhirCtx).getBundle.asInstanceOf[Bundle]
    