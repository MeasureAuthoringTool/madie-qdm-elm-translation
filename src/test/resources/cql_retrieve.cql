library TestRetriveValidation version '1.0.000'

using QDM version '5.6'

codesystem "LOINC": 'urn:oid:2.16.840.1.113883.6.1'
codesystem "SNOMEDCT": 'urn:oid:2.16.840.1.113883.6.96'


valueset "Office Visit": 'urn:oid:2.16.840.1.113883.3.464.1003.101.12.1001'
valueset "Annual Wellness Visit": 'urn:oid:2.16.840.1.113883.3.526.3.1240'

code "Acute Pharyngitis Code":
  '363746003' from "SNOMEDCT" display 'Acute pharyngitis (disorder)'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
                 AgeInYearsAt(date from
                     end of "Measurement Period"
                  )in Interval[52, 74]

define "Get Encounter from valueset Retrieve":
  ["Encounter, Performed": "Office Visit"]

define "Get Encounter From Code Retrieve":
 ["Encounter, Performed": "Acute Pharyngitis Code"]

define "Get Encounter from Code Declaration":
  ["Encounter, Performed": code ~ "Acute Pharyngitis Code"]

define "Get Encounter from CodeSystem Declaration":
  ["Encounter, Performed": code in "SNOMEDCT"]

define "Get Diagnosis from CodeSystem Declaration":
  ["Diagnosis": "SNOMEDCT"]

define "Retrieve with no filter":
 ["Encounter, Performed"] ValidEncounter
    where ValidEncounter.relevantPeriod during "Measurement Period"
