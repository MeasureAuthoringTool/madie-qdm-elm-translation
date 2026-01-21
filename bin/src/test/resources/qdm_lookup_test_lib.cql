library LookupTestLib version '0.0.000'

using QDM version '5.6'

codesystem "RXNORM": 'urn:oid:2.16.840.1.113883.6.88'

valueset "Digoxin Medications": 'urn:oid:2.16.840.1.113883.3.464.1003.1065'
valueset "Race": 'urn:oid:2.16.840.1.114222.4.11.836'

code "1 ML digoxin 0.1 MG/ML Injection": '204504' from "RXNORM" display '1 ML digoxin 0.1 MG/ML Injection'
code "digoxin 0.125 MG Oral Tablet": '197604' from "RXNORM" display 'digoxin 0.125 MG Oral Tablet'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
exists "MedicationOrderInjection" or
  exists "test when then case"

define "MedicationOrderInjection":
["Medication, Order": "1 ML digoxin 0.1 MG/ML Injection"]

define "test when then case":
  ( "More Than One Order"(["Medication, Order": "Digoxin Medications"]) ) DigoxinOrdered
    return DigoxinOrdered Order
      let MedicationStrength: (
        case
          when Order.code ~ "1 ML digoxin 0.1 MG/ML Injection" then 0.125 'mg'
           when Order.code ~ "digoxin 0.125 MG Oral Tablet" then 0.5 'mg'
          else 1 end
      )
      return MedicationStrength

define function "More Than One Order"(Medication List<"Medication, Order"> ):
  "Medication" OrderMedication1
    with "Medication" OrderMedication2
      such that ( OrderMedication1.authorDatetime during "Measurement Period"
          and OrderMedication1.refills >= 1
      )
        or ( date from OrderMedication1.authorDatetime !~ date from OrderMedication2.authorDatetime
            and OrderMedication1.authorDatetime during "Measurement Period"
            and OrderMedication2.authorDatetime during "Measurement Period"
        )
        or ( date from OrderMedication1.authorDatetime ~ date from OrderMedication2.authorDatetime and OrderMedication1.authorDatetime during "Measurement Period"  and date from start of OrderMedication1.relevantPeriod !~ date from start of OrderMedication2.relevantPeriod  and start of OrderMedication1.relevantPeriod during "Measurement Period"
            and start of OrderMedication2.relevantPeriod during "Measurement Period"
        )
    return OrderMedication1
