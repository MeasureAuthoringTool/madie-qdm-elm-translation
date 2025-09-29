package gov.cms.mat.cql_elm_translation.utils.cql;

public class VersionUtil {
  // Helper to determine if provided version is >= baseline (semantic-like: major.minor.patch)
  public static boolean isVersionAtLeast(String version, String baseline) {
    int[] vA = parseVersion(version);
    int[] vB = parseVersion(baseline);
    for (int i = 0; i < 3; i++) {
      if (vA[i] > vB[i]) {
        return true;
      }
      if (vA[i] < vB[i]) {
        return false;
      }
    }
    return true; // equal
  }

  // Extract up to first three numeric components of a version string. Missing parts default to 0.
  private static int[] parseVersion(String version) {
    int[] nums = new int[] {0, 0, 0};
    if (version == null || version.isBlank()) {
      return nums;
    }
    // Split on non-digit separators, but we only care about the first three numeric groups.
    // This will handle inputs like "7", "7.0", "7.0.1", "7.0.1-RC1", "8.0.0", etc.
    String[] parts = version.split("[^0-9]+");
    int idx = 0;
    for (String p : parts) {
      if (p.isEmpty()) {
        continue;
      }
      try {
        nums[idx++] = Integer.parseInt(p);
      } catch (NumberFormatException e) {
        // ignore malformed segment
      }
      if (idx == 3) {
        break;
      }
    }
    return nums;
  }
}
