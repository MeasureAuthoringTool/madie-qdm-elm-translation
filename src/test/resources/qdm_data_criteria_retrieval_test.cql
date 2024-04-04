library DataCriteriaRetrivalTest version '0.0.000'

using QDM version '5.6'

include MATGlobalCommonFunctions version '7.0.000' called Global

codesystem "LOINC": 'urn:oid:2.16.840.1.113883.6.1'
codesystem "CPT": 'urn:oid:2.16.840.1.113883.6.12'

valueset "Encounter Inpatient": 'urn:oid:2.16.840.1.113883.3.666.5.307'
valueset "Nonelective Inpatient Encounter": 'urn:oid:2.16.840.1.113883.3.117.1.7.1.424'

code "Birth date": '21112-8' from "LOINC" display 'Birth date'
code "Clinical Examples": '1021859' from "CPT" display 'Clinical Examples'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Qualifying Encounters":
  ( ["Encounter, Performed": "Encounter Inpatient"]
    union ["Encounter, Performed": "Clinical Examples"] ) Encounter
    where Encounter.relevantPeriod ends during "Measurement Period"

define "Non Elective Inpatient Encounter":
  ["Encounter, Performed": "Nonelective Inpatient Encounter"] NonElectiveEncounter
    where Global."LengthInDays" ( NonElectiveEncounter.relevantPeriod ) <= 120
      and NonElectiveEncounter.relevantPeriod ends during day of "Measurement Period"

define "test stratification":
    "Qualifying Encounters"

define function "Test Observation"(Encounter "Encounter, Performed" ):
  duration in seconds of Encounter.relevantPeriod