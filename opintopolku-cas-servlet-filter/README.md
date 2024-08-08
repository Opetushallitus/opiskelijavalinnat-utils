# OPH CAS Servlet Filter

> Huomio! Tämän paketin käyttö on ilmeisesti täysin turhaa, jos rajapintaa ei kutsu käyttäjiä todella vanhalla CAS clientillä. Ainoa asia, jonka tämä paketti ratkaisee on se, että vanhat (OPH:n?) CAS clientit lähettävät CASin tiketin headerissa.
>
> Käytä tämän paketin sijaan normaalia `org.springframework.security.cas.web.CasAuthenticationFilter`. Esim. pom.xml:
>
> ```xml
>   <dependency>
>     <groupId>org.springframework.security</groupId>
>     <artifactId>spring-security-cas</artifactId>
>     <version>${spring.version}</version>
>   </dependency>
> ```