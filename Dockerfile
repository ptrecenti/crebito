FROM ghcr.io/graalvm/native-image-community:21 AS builder
COPY . /home/gradle/project
WORKDIR /home/gradle/project
RUN microdnf install findutils && \
    ./gradlew nativeCompile


FROM gcr.io/distroless/cc
COPY --from=builder /home/gradle/project/build/native/nativeCompile/crebito /crebito
EXPOSE 8080
ENV SCALE_FACTOR="1" \
    DB_USER="rinha" \
    DB_PASS="rinha" \
    DB_NAME="rinha" \
    DB_HOSTNAME="db" \
    DB_PORT="5432" \
    DB_POOL_PLUS="1"
ENTRYPOINT ["/crebito","-Xms200m","-Xmx200m","-Dlock.strategy=OPTIMIST","-Dcustomers.cache.enabled=true"]