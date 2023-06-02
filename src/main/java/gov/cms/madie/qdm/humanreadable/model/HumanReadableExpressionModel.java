package gov.cms.madie.qdm.humanreadable.model;

import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReadableExpressionModel {
  private String name;
  private String logic;
  private String id;

  private String idFromName(String name) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (c >= 48 && c <= 57
          || // 0-9
          c >= 65 && c <= 90
          || // A-Z
          c >= 97 && c <= 122
          || // a-z
          c == 95) { // _
        result.append(c);
      } else {
        result.append('_');
      }
    }
    return result.toString() + Math.abs(new Random().nextInt());
  }
}
