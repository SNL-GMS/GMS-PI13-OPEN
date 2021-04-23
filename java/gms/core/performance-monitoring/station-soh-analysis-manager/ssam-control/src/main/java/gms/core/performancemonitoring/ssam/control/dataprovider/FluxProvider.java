package gms.core.performancemonitoring.ssam.control.dataprovider;

import reactor.core.publisher.Flux;

public interface FluxProvider<T> {

  Flux<T> getFlux();
}
