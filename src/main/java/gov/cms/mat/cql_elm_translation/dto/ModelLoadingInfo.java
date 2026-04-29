package gov.cms.mat.cql_elm_translation.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.hl7.cql.model.ModelIdentifier;

@Data
public class ModelLoadingInfo {
  @Setter(AccessLevel.NONE)
  private final ModelIdentifier modelIdentifier;

  private volatile ModelLoadingState loadingState;
  private volatile String errorMessage;

  public ModelLoadingInfo(ModelIdentifier modelIdentifier) {
    this.modelIdentifier = modelIdentifier;
    this.loadingState = ModelLoadingState.NOT_LOADED;
  }
}
