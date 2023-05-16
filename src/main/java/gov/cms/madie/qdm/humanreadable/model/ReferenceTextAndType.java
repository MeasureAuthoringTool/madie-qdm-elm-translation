package gov.cms.madie.qdm.humanreadable.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceTextAndType {

  private String referenceText;
  private MeasureReferenceType referenceType = MeasureReferenceType.UNKNOWN;
}
