library definitionCallstack version '0.0.000'

using QICore version '4.1.1'

include HelperLibrary version '0.0.000' called Helper

parameter "Measurement Period" Interval<DateTime>

context Patient

define "measure 1":
    true

define "measure 2":
    "measure 1"

define "measure 3":
    "measure 1" and "measure 2"

define "measure 4":
    exists Helper."Inpatient Encounter" and "measure function"()

define function "measure function"():
    true
