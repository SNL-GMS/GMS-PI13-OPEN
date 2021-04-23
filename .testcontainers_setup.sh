if [ -f "$HOME/.testcontainers.properties" ]; then rm $HOME/.testcontainers.properties || true; fi

echo docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy > $HOME/.testcontainers.properties
echo ryuk.container.image=${CI_DOCKER_REGISTRY}/testcontainers/ryuk:0.2.3 >> $HOME/.testcontainers.properties
echo tinyimage.container.image = ${CI_DOCKER_REGISTRY}/alpine:3.5 >> $HOME/.testcontainers.properties
echo ambassador.container.image = ${CI_DOCKER_REGISTRY}/richnorth/ambassador:latest >> $HOME/.testcontainers.properties
echo compose.container.image = ${CI_DOCKER_REGISTRY}/docker/compose:1.8.0 >> $HOME/.testcontainers.properties
echo kafka.container.image = ${CI_DOCKER_REGISTRY}/gms-common/kafka:${VERSION} >> $HOME/.testcontainers.properties
echo socat.container.image = ${CI_DOCKER_REGISTRY}/alpine/socat:1.7.3.4-r0 >> $HOME/.testcontainers.properties
