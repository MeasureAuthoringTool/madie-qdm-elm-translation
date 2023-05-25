library DataCriteriaRetrivalTest version '0.0.000'

using QDM version '5.6'

codesystem "LOINC": 'urn:oid:2.16.840.1.113883.6.1'
codesystem "CPT": 'urn:oid:2.16.840.1.113883.6.12'

valueset "Encounter Inpatient": 'urn:oid:2.16.840.1.113883.3.666.5.307'

code "Birth date": '21112-8' from "LOINC" display 'Birth date'
code "Clinical Examples": '1021859' from "CPT" display 'Clinical Examples'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Qualifying Encounters":
  ( ["Encounter, Performed": "Encounter Inpatient"]
    union ["Encounter, Performed": "Clinical Examples"] ) Encounter
    where Encounter.relevantPeriod ends during "Measurement Period"