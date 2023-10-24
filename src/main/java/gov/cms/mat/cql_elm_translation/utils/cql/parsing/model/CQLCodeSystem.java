package gov.cms.mat.cql_elm_translation.utils.cql.parsing.model;

import lombok.Data;

// TODO: Auto-generated Javadoc
@Data
public class CQLCodeSystem {

  /** The id. */
  private String id;

  /** The code system. */
  private String codeSystem;

  /** The code system name. */
  private String codeSystemName;

  /** The code system version. */
  private String codeSystemVersion;

  /** The value set OID. */
  private String valueSetOID;

  /**
   * stores off the version uri. example: codesystem "SNOMEDCT:2017-09":
   * 'http://snomed.info/sct/731000124108' version
   * 'http://snomed.info/sct/731000124108/version/201709'
   */
  private String versionUri;
}
