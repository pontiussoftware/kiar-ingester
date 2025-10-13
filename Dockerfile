FROM gradle:jdk21 AS build

COPY --chown=gradle:gradle . /src
WORKDIR /src
RUN gradle --no-daemon distTar
WORKDIR /src/kiar-ingest/build/distributions/
RUN tar xf ./kiar-ingest.tar

FROM openjdk:21-jdk

COPY --from=build /src/kiar-ingest/build/distributions/kiar-ingest /kiar-ingest
CMD ["rm", "-rf", "/src"]

EXPOSE 7070

ENTRYPOINT /kiar-ingest/bin/nmr