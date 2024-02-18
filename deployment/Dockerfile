FROM scratch AS config
COPY db-pool.properties /db-pool.properties

FROM ptrecenti/crebito
COPY --from=config /db-pool.properties /db-pool.properties
EXPOSE 8080
ENTRYPOINT ["/crebito", "-Xms200m", "-Xmx200m", "-Dhikaricp.configurationFile='/db-pool.properties'"]
