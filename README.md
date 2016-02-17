# java-utils

Yleiskäyttöisiä java-kirjastoja Opetushallituksen verkkokehitykseen

## Kehitys

* JDK 1.7, koska osa käyttävistä projekteista on vielä 1.7
* Jokainen alimoduli julkaisee oman jar-pakettinsa
* Alimodulit mahdollisimman yksinkertaisina: [SRP](https://en.wikipedia.org/wiki/Single_responsibility_principle)
* Lisää uudet java-luokat omiin alimoduleihinsa, varsinkin jos liity kiinteästi olemassaolevaan pakettiin
* Alimodulilla oma versionumeronsa, nosta jos teet rikkovia muutoksia alimoduliin
* Rootin versionumeroa ei pitäisi olla tarvetta muokata
* OPH:n Bamboo ajaa "mvn clean deploy"-komennon mikä buildaa jar-paketit ja asentaa ne artifactoryyn

## Komentoja

    mvn test
