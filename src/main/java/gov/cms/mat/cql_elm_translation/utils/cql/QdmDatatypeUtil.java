package gov.cms.mat.cql_elm_translation.utils.cql;

import java.util.Map;

public class QdmDatatypeUtil {

  private static Map<String, String> negationRationale;

  static {
    negationRationale = Map.ofEntries(
        Map.entry("Assessment, Not Ordered", "AssessmentOrder"),
        Map.entry("Assessment, Not Recommended", "AssessmentRecommended"), // Assessment, Recommended
        Map.entry("Assessment, Not Performed", "AssessmentPerformed"),
        Map.entry("Communication, Not Performed", "CommunicationPerformed"),
        Map.entry("Device, Not Ordered", "DeviceOrder"),
        Map.entry("Device, Not Recommended", "DeviceRecommended"),
        Map.entry("Diagnostic Study, Not Ordered", "DiagnosticStudyOrder"),
        Map.entry("Diagnostic Study, Not Recommended", "DiagnosticStudyRecommended"),
        Map.entry("Diagnostic Study, Not Performed", "DiagnosticStudyPerformed"),
        Map.entry("Encounter, Not Ordered", "EncounterOrder"),
        Map.entry("Encounter, Not Recommended", "EncounterRecommended"),
        Map.entry("Intervention, Not Ordered", "InterventionOrder"),
        Map.entry("Intervention, Not Recommended", "InterventionRecommended"),
        Map.entry("Intervention, Not Performed", "InterventionPerformed"),
        Map.entry("Immunization, Not Ordered", "ImmunizationOrder"),
        Map.entry("Immunization, Not Administered", "ImmunizationAdministered"),
        Map.entry("Medication, Not Administered", "MedicationAdministered"),
        Map.entry("Medication, Not Dispensed", "MedicationDispensed"),
        Map.entry("Medication, Not Discharged", "MedicationDischarge"),
        Map.entry("Medication, Not Ordered", "MedicationOrder"),
        Map.entry("Physical Exam, Not Ordered", "PhysicalExamOrder"),
        Map.entry("Physical Exam, Not Recommended", "PhysicalExamRecommended"),
        Map.entry("Physical Exam, Not Performed", "PhysicalExamPerformed"),
        Map.entry("Procedure, Not Ordered", "ProcedureOrder"),
        Map.entry("Procedure, Not Recommended", "ProcedureRecommended"),
        Map.entry("Procedure, Not Performed", "ProcedurePerformed"),
        Map.entry("Substance, Not Ordered", "SubstanceOrder"),
        Map.entry("Substance, Not Recommended", "SubstanceRecommended"),
        Map.entry("Substance, Not Administered", "SubstanceAdministered"),
        Map.entry("Laboratory Test, Not Ordered", "LaboratoryTestOrder"),
        Map.entry("Laboratory Test, Not Recommended", "LaboratoryTestRecommended"),
        Map.entry("Laboratory Test, Not Performed", "LaboratoryTestPerformed")
    );
  }

  public static boolean isValidNegation(final String negation) {
    return negationRationale.containsKey(negation);
  }
  public static String getTypeForNegation(final String negation) {
    return negationRationale.get(negation);
  }

}
