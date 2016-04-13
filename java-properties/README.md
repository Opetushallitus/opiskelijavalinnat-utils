# OPH properties and url configuration

* Works for Java, Scala, Javascript
* Simple syntax which is the same across languages
  * Javascript: `window.url("organisaatio-service.soap", param1, param2)`
  * Java and Scala: `ophProperties.url("organisaatio-service.soap", param1, param2)`
  * .properties: `organisaatio-service.soap=/organisaatio-service/soap/$1/$2`
  * .json: `{"organisaatio-service.soap": "/organisaatio-service/soap/$1/$2"}`
* Different property resolve methods:
  * url(key, param1, param2, ...) property resolving with additional url features: baseUrl override (see below) and unused named parameters are added to querystring
  * require(key, param1, param2, ...) throws an exception if key is not defined
  * getProperty(key, param1, param2, ...) returns null if key is not defined
  * getOrElse(key, defaultValue, param1, param2, ...) returns defaultValue if key is not defined
* All property resolve methods support parameter lookup: by index ($1), by parameter name ($id) and by property key ${key:defaultValue}
  * .properties: `organisaatio-service.info=${publicLB}/organisaatio-service/info/$id/$user`
  * Javascript: `window.url("organisaatio-service.info", {id: oid, user: user.id})`
  * Parameter name supports two syntaxes: ${key} throws an exception if key is not defined, ${key:defaultValue} uses defaultValue if key not defined
  * Java supports Maps, Scala implementation supports Maps and case classes
* Supports development, property values can be overridden
  * Property values can be overriden with command line parameters: `-Dorganisaatio-service.soap=https://testserver/soap/123/456`
  * Frontend can be instructed with: `-Dfront.organisaatio-service.soap=https://testserver/soap/123/456`
  * Prefixing a key with `front.` makes it available to front without the prefix
  * Prefixing a key with `url.` makes it available to both front and backend without the prefix
  * note: You'll need to add a properties servlet to your application to serve the override the properties from backend
* Backend override properties can be loaded from files with command line. These will override values loaded with code
  * `-Doph-properties=file1.properties,file2.properties` - for properties (url. and front. filtering is applied to get front properties)
  * `-Doph-front=file3.properties,file4.properties` - for front only properties
* Easily modify base urls. URL resolving looks for "*service*.baseUrl" and "baseUrl" to resolve the whole url: "*suoritusrekisteri*.info"
  * `-Dsuoritusrekisteri.baseUrl=https://testserver/suoritusrekisteri` - for suoritusrekisteri urls
  * `-DbaseUrl=https://testserver/suoritusrekisteri` - for all urls
* Debug-mode for showing how application works: `-DOphProperties.debug=true`
* [project_info_server](https://github.com/Opetushallitus/dokumentaatio/tree/master/project_info)
parses the configuration files and is able to generate reports from the data.

    note: The files should be named according to the pattern:
    `*url.properties|*url_properties.json|*oph.properties|*oph_properties.json`, for example `suoritusrekisteri_oph.properties`.
    project_info_server takes "suoritusrekisteri" from the filename and adds dependencies for each service derived from the property key,
    for example "organisaatio-service".

See implementation and usage in following projects
* [Java](https://github.com/Opetushallitus/java-utils/tree/master/java-properties)
* [Javascript](https://github.com/Opetushallitus/java-utils/tree/master/java-properties/javascript)
* [Scala](https://github.com/Opetushallitus/scala-utils/tree/master/scala-properties_2.11)
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
* `bower install https://github.com/Opetushallitus/java-utils/blob/master/java-properties/javascript/oph_urls.js`
* [bower.json](https://github.com/Opetushallitus/hakurekisteri/blob/master/bower.json)

note: Add the file oph_urls.js to the javascript build process or refer to it in the main page with a script tag.

    <script type="text/javascript" src="static/js/oph_urls.js/index.js"></script>

### Java and Scala

    // load properties by default from /suoritusrekisteri-web-oph.properties (which should be placed in class path)
    OphProperties properties = new OphProperties("/suoritusrekisteri-web-oph.properties");
    properties.url("organisaatio-service.soap");

### Javascript

    // load properties from a static file and a rest resource which returns override properties
    // start application after resources are loaded
    window.urls.loadFromUrls("suoritusrekisteri-web-frontend-url_properties.json", "rest/v1/properties").success(function() {
      // bootstrap angular application manually after properties are loaded
      angular.element(document).ready(function() {
        angular.bootstrap(document, ['myApp'])
      })
    })
    window.url("organisaatio-service.soap")

### Supported property file formats

* .properties

        service.key=value

* .json, nested and flat structure

        {
            "service": {
                "key": "value"
            }
        }

        {
            "service.key": "value"
        }

* .js which is loaded with a script tag after including oph_urls.js can set properties directly to window.urls.XX

        // window.urls.override should be set with rest end point from backend application
        window.urls.override = {
        }
        // window.urls.properties should be set with with static file
        window.urls.properties = {
        }
        // should not be set
        window.urls.defaults = {
        }

* .js es6

        export default {
            hakuperusteetadmin: {
                paymentUpdateUrl: "/hakuperusteetadmin/api/v1/admin/payment"
            }
        }

