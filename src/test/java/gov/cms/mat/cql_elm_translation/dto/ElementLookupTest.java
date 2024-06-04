package gov.cms.mat.cql_elm_translation.dto;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;

class ElementLookupTest {

  @Test
  void testHashSetUniqueness() {
    ElementLookup elementLookup1 =
        ElementLookup.builder()
            .id("ID1")
            .oid("111.222.333")
            .name("TestCode")
            .codeName("TestCodeName")
            .codeSystemName("Amazing Code System")
            .release("Release1")
            .displayName("Test Code")
            .datatype("Code")
            .build();

    ElementLookup elementLookup2 = elementLookup1.toBuilder().id("ID2").build();

    Set<ElementLookup> set = new HashSet<>();
    set.add(elementLookup1);
    set.add(elementLookup2);
    assertThat(set.size(), is(equalTo(1)));
  }
}
