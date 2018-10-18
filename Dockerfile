FROM statisticsnorway/alpine-jdk11-buildtools:latest as build

RUN ["jlink", "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=2", "--module-path", "/opt/jdk/jmods", "--output", "/linked",\
 "--add-modules", "java.base"]

COPY pom.xml /loadtest/plotgen/
WORKDIR /loadtest/plotgen
RUN mvn -B verify dependency:go-offline
COPY src /loadtest/plotgen/src/
RUN mvn -B -o verify && mvn -B -o dependency:copy-dependencies



FROM alpine:latest

RUN apk --no-cache add bash curl gnuplot zip perl vim jq

COPY --from=build /linked /opt/jdk/
COPY --from=build /loadtest/plotgen/target/dependency /opt/plotgen/lib/
COPY --from=build /loadtest/plotgen/target/*.jar /opt/plotgen/

ADD prepopulate.sh /

ENV PATH=/opt/jdk/bin:$PATH

VOLUME ["/loadtest", "/results"]

CMD ["java", "-cp", "/opt/plotgen/*:/opt/plotgen/lib/*", "no.ssb.lds.loadtest.HTTPLoadTestBaselineStatistics"]
