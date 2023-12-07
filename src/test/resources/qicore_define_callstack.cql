library definitionCallstack version '0.0.000'

using QICore version '4.1.1'

include HelperLibrary version '0.0.000' called Helper

parameter "Measurement Period" Interval<DateTime>

context Patient

define "define 1":
    true

define "define 2":
    "define 1"

define "define 3":
    "define 1" and "define 2"

define "define 4":
    exists Helper."Inpatient Encounter" and "func"()

define function "func"():
    true
