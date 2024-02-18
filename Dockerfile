FROM ghcr.io/graalvm/native-image-community:21 AS builder
COPY . /home/gradle/project
WORKDIR /home/gradle/project
RUN microdnf install findutils && \
    ./gradlew nativeCompile


FROM gcr.io/distroless/cc
COPY --from=builder /home/gradle/project/build/native/nativeCompile/crebito /crebito
EXPOSE 8080
ENV SCALE_FACTOR \
    DB_USER \
    DB_PASS \
    DB_NAME \
    DB_HOSTNAME \
    DB_PORT \
    DB_POOL
CMD ["/crebito","-Xms64m","-Xmx64m","-ea","-server"]