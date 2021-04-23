package gms.shared.frameworks.test.utils.services;

/**
 * GMS Service types used for component and integration TestContainers.
 */
public enum GmsServiceType {
    ACEI_MERGE_PROCESSOR("acei-merge-processor"),
    BASTION("bastion"),
    CD11_INJECTOR("cd11-injector"),
    CONFIG_LOADER("config-loader"),
    CONNMAN("da-connman"),
    DATAMAN("da-dataman"),
    ETCD("etcd"),
    INTERACTIVE_ANALYSIS_API_GATEWAY("interactive-analysis-api-gateway"),
    INTERACTIVE_ANALYSIS_CONFIG_SERVICE("interactive-analysis-config-service"),
    INTERACTIVE_ANALYSIS_UI("interactive-analysis-ui"),
    JAVADOC("javadoc"),
    KAFKA_ONE("kafka1"),
    KAFKA_TWO("kafka2"),
    KAFKA_THREE("kafka3"),
    OSD_RSDF_KAFKA_CONSUMER("frameworks-osd-rsdf-kafka-consumer"),
    OSD_SERVICE("frameworks-osd-service"),
    OSD_STATION_SOH_KAFKA_CONSUMER("frameworks-osd-station-soh-kafka-consumer"),
    OSD_TTL_WORKER("frameworks-osd-ttl-worker"),
    POSTGRES_SERVICE("postgresql-gms"),
    POSTGRES_EXPORTER("postgresql-exporter"),
    PROCESSING_CONFIG_SERVICE("frameworks-configuration-service"),
    PROMETHEUS("prometheus"),
    RSDF_STREAMS_PROCESSOR("cd11-rsdf-processor"),
    SOH_CONTROL("soh-control"),
    SWAGGER("swagger-gms"),
    TRAEFIK("traefik"),
    ZOOKEEPER("zoo");

    private final String type;

    GmsServiceType(String type) {
        this.type = type;
    }

    public static GmsServiceType getEnum(String type) {
        for (GmsServiceType serviceType : values()) {
            if (serviceType.toString().equalsIgnoreCase(type)) {
                return serviceType;
            }
        }

        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return this.type;
    }
}

