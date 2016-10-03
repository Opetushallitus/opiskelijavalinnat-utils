# java url client for testing http connections

makes connections in one thread to given url as many times as given and reads content and logs slow queries and mean stats

## build

    mvn package

## run

    java -jar target/url-connection-tester-0.1.0-SNAPSHOT-jar-with-dependencies.jar <url> <iterations, default 1> <slow query treshold, default 2s>

## example

    $ java -jar target/url-connection-tester-0.1.0-SNAPSHOT-jar-with-dependencies.jar https://testi.virkailija.opintopolku.fi/cas/login 10
    16:29:31,630  INFO Tester:49 - Calling https://testi.virkailija.opintopolku.fi/cas/login 10 times
    16:29:32,514  INFO Tester:60 - Warm up request done. request stats: length: 839ms, response size: 1827 chars
    16:29:32,783  INFO Tester:84 - Url https://testi.virkailija.opintopolku.fi/cas/login connections mean stats of 10 requests: mean length: 26ms, max length: 0.037s, errors: 0 kpl
    16:29:32,783  INFO Tester:86 - No queries took over 2 seconds
