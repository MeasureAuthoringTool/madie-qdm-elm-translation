# cql-elm-translation

This is a Springboot application to translate CQL to ELM.

Transltion service uses madie-rest-commons and madie-server-commons as dependencies, these artifacts are hosted on github packages.


Github requires authentication before downloading artifacts, So Add github credentials ( recommended to use Github Access Token ).

Add the following server in ./m2/settings.xml
```
  <servers>
    <server>
      <id>github</id>
      <username>Your Github UserName</username>
      <password>Your Github Access Token</password>
    </server>
  </servers>
</settings>
```

To build
```
mvn clean install
```
