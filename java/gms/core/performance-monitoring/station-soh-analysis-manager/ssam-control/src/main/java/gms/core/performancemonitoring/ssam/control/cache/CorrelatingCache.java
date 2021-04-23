package gms.core.performancemonitoring.ssam.control.cache;

public interface CorrelatingCache<S, T> {

  void add(T item);

  void track(S parent);

}
