package gms.integration.util;

public interface FinishingConsumer<T> {

  boolean consume(T t);
}
