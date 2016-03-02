# OPH properties and url configuration

* Works for java, scala, javascript
* Simple syntax which is the same across languages
  * javascript:
        ```javascript window.url("organisaatio-service.soap", param1, param2)```
  * java and scala: `ophProperties.url("organisaatio-service.soap", param1, param2)`
  * .properties: `organisaatio-service.soap=/organisaatio-service/soap/$1/$2`
  * .json: `{"organisaatio-service.soap": "/organisaatio-service/soap/$1/$2"}`
* Supports named parameters
    * .properties: `organisaatio-service.info=/organisaatio-service/info/$id/$user`
    * javascript: `window.url("organisaatio-service.info", {id: oid, user: user.id})`
    * java supports Maps
    * scala implementation supports Maps and case classes
* Supports development, property keys can be overridden
    * Property values can be overriden with command line parameters: `-Dorganisaatio-service.soap=https://testserver/soap/123/456`
    * Frontend can be instructed with: `-Dfront.organisaatio-service.soap=https://testserver/soap/123/456`
    * Prefixing a key with `front.` makes it available to front without the prefix
    * Prefixing a key with `url.` makes it available to both front and backend without the prefix
    * note: You'll need to add a properties servlet to your application to serve the override the properties from backend
* Url resolving looks for "<service>.baseUrl" and "baseUrl" to resolve the whole url: "suoritusrekisteri.info"
    * `-Dsuoritusrekisteri.baseUrl=https://testserver/suoritusrekisteri` - for suoritusrekisteri urls
    * `-DbaseUrl=https://testserver/suoritusrekisteri` - for all urls
* project_info_server is able to generate dependency graphs from url property keys. The files can be named
`*url.properties|*url_properties.json|*oph.properties|*oph_properties.json`, for example `suoritusrekisteri_oph.properties`.
project_info_server takes "suoritusrekisteri" from the filename and adds dependencies for each key service, for example "organisaatio-service".

See implementation and usage in following projects
* [java](https://github.com/Opetushallitus/java-utils/tree/master/java-properties)
* [javascript](https://github.com/Opetushallitus/java-utils/tree/master/java-properties/src/main/javascript)
* [scala](https://github.com/Opetushallitus/scala-utils/tree/master/scala-properties_2.11)
* [project_info_server](https://github.com/Opetushallitus/dokumentaatio/tree/master/project_info)
* [suoritusrekisteri](https://github.com/Opetushallitus/hakurekisteri) - uses scala-properties and oph_urls.js

## Configuration

### Maven

    <dependency>
        <groupId>fi.vm.sade.java-utils</groupId>
        <artifactId>java-properties</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

### SBT

    "fi.vm.sade" %% "scala-properties" % "0.0.1-SNAPSHOT"

### Bower, example from suoritusrekisteri

* [.bowerrc](https://github.com/Opetushallitus/hakurekisteri/blob/master/.bowerrc)
* [bower.json](https://github.com/Opetushallitus/hakurekisteri/blob/master/bower.json)

