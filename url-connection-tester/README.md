# java url client for testing http connections

makes connections in one thread to given url as many times as given and reads content and logs slow queries and mean stats

## build

mvn package

## run

java -jar target/url-connection-tester-0.1.0-SNAPSHOT-jar-with-dependencies.jar <url> <iterations, default 1> <slow query treshold, default 2s>

## example

java -jar target/url-connection-tester-0.1.0-SNAPSHOT-jar-with-dependencies.jar https://testi.virkailija.opintopolku.fi/cas/login 1000
