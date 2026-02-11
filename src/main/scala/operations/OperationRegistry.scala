package operations

import ca.uhn.fhir.context.FhirContext
import org.apache.pekko.actor.ActorSystem
import org.slf4j.Logger as SLFLogger

class OperationRegistry(using fhirCtx: FhirContext, val hl7System: ActorSystem, val logger: SLFLogger) {
  val patientDemographicsOperations = new PatientDemographicsOperations()
}
