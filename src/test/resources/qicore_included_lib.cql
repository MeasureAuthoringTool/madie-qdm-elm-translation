library HelperLibrary version '0.0.000'

using QICore version '4.1.1'

valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'

parameter "Measurement Period" Interval<DateTime>
parameter "test date" Interval<DateTime>

context Patient

define "Inpatient Encounter":
  [Encounter: "Encounter Inpatient"] EncounterInpatient
		where EncounterInpatient.status = 'finished'
		and EncounterInpatient.period ends during day of "Measurement Period"
		
define fluent function "Null Abatement"(Conditions List<Condition>):
  Conditions C where C.abatement is null
