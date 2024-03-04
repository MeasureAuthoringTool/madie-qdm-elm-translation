package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CQLModel {
  private String libraryName;
  private String versionUsed;
  private String libraryComment;
  private String usingModelVersion;
  private String usingModel;
  private String context;

  @Builder.Default private List<CQLQualityDataSetDTO> valueSetList = new ArrayList<>();
  @Builder.Default private List<CQLQualityDataSetDTO> allValueSetAndCodeList = new ArrayList<>();
  @Builder.Default private List<CQLParameter> cqlParameters = new ArrayList<>();
  @Builder.Default private List<CQLDefinition> cqlDefinitions = new ArrayList<>();
  @Builder.Default private List<CQLFunction> cqlFunctions = new ArrayList<>();
  @Builder.Default private List<CQLCodeSystem> codeSystemList = new ArrayList<>();
  @Builder.Default private List<CQLCode> codeList = new ArrayList<>();
  @Builder.Default private List<CQLIncludeLibrary> cqlIncludeLibraries = new ArrayList<>();
  @Builder.Default private Map<CQLIncludeLibrary, CQLModel> includedLibraries = new HashMap<>();

  private int lines;

  public List<CQLDefinition> getDefinitionList() {
    return cqlDefinitions;
  }

  public List<CQLDefinition> getIncludedDef() {
    List<CQLDefinition> includedDefNames = new ArrayList<>();
    for (CQLModel value : includedLibraries.values()) {
      includedDefNames.addAll(value.getDefinitionList());
    }
    return includedDefNames;
  }

  public List<CQLIdentifierObject> getCQLIdentifierDefinitions() {
    List<CQLIdentifierObject> includedDefCQLIdentifierObject = new ArrayList<>();
    for (CQLIncludeLibrary lib : includedLibraries.keySet()) {
      CQLModel model = includedLibraries.get(lib);
      for (CQLDefinition def : model.getDefinitionList()) {
        includedDefCQLIdentifierObject.add(
            new CQLIdentifierObject(lib.getAliasName(), def.getName()));
      }
    }
    return includedDefCQLIdentifierObject;
  }

  public List<CQLFunction> getIncludedFunc() {
    List<CQLFunction> includedFunctions = new ArrayList<>();

    includedLibraries.forEach(
        (k, v) -> {
          v.getCqlFunctions().forEach(f -> f.setAliasName(k.getAliasName()));
          includedFunctions.addAll(v.getCqlFunctions());
        });

    return includedFunctions;
  }

  public List<CQLIdentifierObject> getCQLIdentifierFunctions() {
    List<CQLIdentifierObject> includedFuncCQLIdentifierObject = new ArrayList<>();
    for (CQLIncludeLibrary lib : includedLibraries.keySet()) {
      CQLModel model = includedLibraries.get(lib);
      for (CQLFunction fun : model.getCqlFunctions()) {
        includedFuncCQLIdentifierObject.add(
            new CQLIdentifierObject(lib.getAliasName(), fun.getName()));
      }
    }
    return includedFuncCQLIdentifierObject;
  }

  public List<CQLQualityDataSetDTO> getIncludedValueSet() {
    List<CQLQualityDataSetDTO> includedValueSetNames = new ArrayList<>();
    for (CQLModel value : includedLibraries.values()) {
      includedValueSetNames.addAll(value.getValueSetList());
    }
    return includedValueSetNames;
  }

  public List<CQLIdentifierObject> getCQLIdentifierValueSet() {
    List<CQLIdentifierObject> includedValueSetCQLIdentifierObject = new ArrayList<>();
    for (CQLIncludeLibrary lib : includedLibraries.keySet()) {
      CQLModel model = includedLibraries.get(lib);
      for (CQLQualityDataSetDTO value : model.getValueSetList()) {
        includedValueSetCQLIdentifierObject.add(
            new CQLIdentifierObject(lib.getAliasName(), value.getName()));
      }
    }
    return includedValueSetCQLIdentifierObject;
  }

  public List<CQLParameter> getIncludedParam() {
    List<CQLParameter> includedParamNames = new ArrayList<>();
    for (CQLModel value : includedLibraries.values()) {
      includedParamNames.addAll(value.getCqlParameters());
    }
    return includedParamNames;
  }

  public List<CQLIdentifierObject> getCQLIdentifierParam() {
    List<CQLIdentifierObject> includedParamCQLIdentifierObject = new ArrayList<>();
    for (CQLIncludeLibrary lib : includedLibraries.keySet()) {
      CQLModel model = includedLibraries.get(lib);
      for (CQLParameter param : model.getCqlParameters()) {
        includedParamCQLIdentifierObject.add(
            new CQLIdentifierObject(lib.getAliasName(), param.getName()));
      }
    }
    return includedParamCQLIdentifierObject;
  }

  public List<CQLCode> getIncludedCode() {
    List<CQLCode> includedCodeNames = new ArrayList<>();
    for (CQLModel value : includedLibraries.values()) {
      includedCodeNames.addAll(value.getCodeList());
    }
    return includedCodeNames;
  }

  public List<CQLIdentifierObject> getCQLIdentifierCode() {
    List<CQLIdentifierObject> includedCodeCQLIdentifierObject = new ArrayList<>();
    for (CQLIncludeLibrary lib : includedLibraries.keySet()) {
      CQLModel model = includedLibraries.get(lib);
      for (CQLCode code : model.getCodeList()) {
        includedCodeCQLIdentifierObject.add(
            new CQLIdentifierObject(lib.getAliasName(), code.getDisplayName()));
      }
    }
    return includedCodeCQLIdentifierObject;
  }

  /**
   * Gets a valueset by name from the parent or any children
   *
   * @param formattedCodeName the name in the format libraryname-x.x.xxx|alias|code identifier if
   *     from child, otherwise just code identifer
   * @return the code found
   */
  public CQLCode getCodeByName(String formattedCodeName) {
    String codeName = formattedCodeName;
    String libraryNameVersion = null; // name in the format libraryname-x.x.xxx
    String[] codeSplit = formattedCodeName.split("\\|");
    if (codeSplit.length == 3) {
      libraryNameVersion = codeSplit[0];
      codeName = codeSplit[2];
    }

    // if the library name version is null, then the code is in the parent
    if (libraryNameVersion == null) {
      for (CQLCode code : codeList) {
        if (code.getDisplayName() == null
            ? code.getCodeName().equals(codeName)
            : code.getDisplayName().equals(codeName)) {
          return code;
        }
      }
    } else {
      final String nameVersion = libraryNameVersion;
      List<CQLIncludeLibrary> cqlIncludeLibrary =
          includedLibraries.keySet().stream()
              .filter(
                  lib ->
                      createNameVersionString(lib.getCqlLibraryName(), lib.getVersion())
                          .equals(nameVersion))
              .collect(Collectors.toList());
      if (!cqlIncludeLibrary.isEmpty()) {
        for (CQLCode code : includedLibraries.get(cqlIncludeLibrary.get(0)).getCodeList()) {
          if (code.getDisplayName().equals(codeName)) {
            return code;
          }
        }
      }
    }

    return null;
  }

  private String createNameVersionString(String name, String version) {
    return name + "-" + version;
  }

  /**
   * Gets a code by name from the parent or any children
   *
   * @param formattedValuesetName the name in the format libraryname-x.x.xxx|alias|valueset
   *     identifier
   * @return the code found
   */
  public CQLQualityDataSetDTO getValuesetByName(String formattedValuesetName) {
    String valuesetName = formattedValuesetName;
    String libraryNameVersion = null; // name in the format libraryname-x.x.xxx
    String[] valuesetSplit = formattedValuesetName.split("\\|");
    if (valuesetSplit.length == 3) {
      libraryNameVersion = valuesetSplit[0];
      valuesetName = valuesetSplit[2];
    }

    // if the library name version is null, then the code is in the parent
    if (libraryNameVersion == null) {
      for (CQLQualityDataSetDTO valueset : valueSetList) {
        if (Objects.equals(valueset.getName(), valuesetName)) {
          return valueset;
        }
      }
    } else {
      final String nameVersion = libraryNameVersion;
      List<CQLIncludeLibrary> cqlIncludeLibrary =
          includedLibraries.keySet().stream()
              .filter(
                  lib ->
                      createNameVersionString(lib.getCqlLibraryName(), lib.getVersion())
                          .equals(nameVersion))
              .collect(Collectors.toList());
      if (!cqlIncludeLibrary.isEmpty()) {
        for (CQLQualityDataSetDTO valueset :
            includedLibraries.get(cqlIncludeLibrary.get(0)).getValueSetList()) {
          if (valueset.getName().equals(valuesetName)) {
            return valueset;
          }
        }
      }
    }

    return null;
  }

  /**
   * This function returns a list containing all the definitions and functions names in the model
   *
   * @return list containing all the definitions and functions names in the model
   */
  public Set<String> getExpressionListFromCqlModel() {
    Set<String> expressionList = new HashSet<>();

    for (CQLDefinition cqlDefinition : cqlDefinitions) {
      expressionList.add(cqlDefinition.getName());
    }

    for (CQLFunction cqlFunction : cqlFunctions) {
      expressionList.add(cqlFunction.getName());
    }

    return expressionList;
  }

  public String getFormattedName() {
    return this.libraryName + "-" + this.versionUsed;
  }
}
