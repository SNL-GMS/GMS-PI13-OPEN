package gms.shared.frameworks.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;

public class NettyRequest implements Request {

    private HttpServerRequest request;
    private byte[] body;

    public NettyRequest(HttpServerRequest request, byte[] body) {
        this.request = request;
        this.body = body;
    }

    @Override
    public Optional<String> getPathParam(String name) {
        return Optional.ofNullable(this.request.param(name));
    }

    @Override
    public String getBody() {
        return new String(this.body);
    }

    @Override
    public byte[] getRawBody() {
        return this.body;
    }

    @Override
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(this.request.requestHeaders().get(name));
    }

    @Override
    public Map<String, String> getHeaders() {
        return this.request.requestHeaders().entries().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
