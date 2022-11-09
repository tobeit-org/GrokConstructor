# grokconstructor

Grok is a collection of named regular expressions that can be used - for instance with logstash http://logstash.net/ -
to parse logfiles. GrokDiscovery http://grokdebug.herokuapp.com/ can somewhat help you by suggesting regular
expressions. GrokConstructor goes beyond that by finding many possible regular expressions
that match a whole set of logfile lines by using groks patterns and fixed strings. This can be done automatically
(which is of limited use only for small stuff), or in a incremental process.

Updated by Joan Jerez in November 2022. Now, it depends on Scala 3.2 and Tomcat 9 (javax.servlet 4.0.1).

## How to run it

### http://grokconstructor.appspot.com/

The best way is probably to use it on http://grokconstructor.appspot.com/ - 
there is also a good description, and you can use it on
some examples or for your own log lines you want to match.

### Deploy as a WAR (only working)

If you want to run it on a system without internet connection or that has an application server, anyway, build it with:
`mvn compile war:war`

Finally, you deploy the created target/GrokConstructor-*-SNAPSHOT.war e.g. on a Tomcat.

### Standalone Executable (not working)

`java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar` runs an embedded Tomcat that makes it available at http://localhost:8080/ .
Please be aware that this creates a directory .extract in the current directory that contains the unpacked webapp.

You can print additional arguments (such as ports, unpack location) with `java -jar GrokConstructor-0.1.0-SNAPSHOT-standalone.jar -h`

### With Docker (not tested)

If you don't have a JDK installation and maven installed on your server and don't want to create a standalone executable otherwise, you can also run the build and startup within a on-build docker container (courtesy of Timothy Van Heest http://turtlemonvh.github.io/). Please note that this container executes the maven build within the docker container and then starts the development server.
```
docker build -t grokconstructor .
docker run -d -p 8080:8080 grokconstructor
```
Alternatively, you can run it with docker-compose:
```
docker-compose up
```
