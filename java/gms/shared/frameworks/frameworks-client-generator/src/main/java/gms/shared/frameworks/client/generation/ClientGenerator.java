package gms.shared.frameworks.client.generation;

import gms.shared.frameworks.client.ServiceClientJdkHttp;
import gms.shared.frameworks.client.ServiceRequest;
import gms.shared.frameworks.common.ContentType;
import gms.shared.frameworks.common.annotations.Component;
import gms.shared.frameworks.systemconfig.SystemConfig;
import gms.shared.frameworks.utilities.AnnotationUtils;
import gms.shared.frameworks.utilities.PathMethod;
import gms.shared.frameworks.utilities.ServiceReflectionUtilities;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates client implementations that use HTTP given an interface.
 */
public class ClientGenerator {

  private ClientGenerator() {
  }

  /**
   * Creates a proxy instantiation of the given client interface that is implemented using HTTP.
   *
   * @param clientClass the class of the interface
   * @param <T>         the type of the interface, same as the return type
   * @return an instance of the client interface
   * @throws NullPointerException     if clientClass is null
   * @throws IllegalArgumentException if the clientClass doesn't have @Component
   */
  public static <T> T createClient(Class<T> clientClass) {
    return createClient(clientClass, ServiceClientJdkHttp.create(),
        SystemConfig.create(getComponentName(clientClass)));
  }

  /**
   * Creates a proxy instantiation of the given client interface that is implemented using the given
   * HTTP client.
   *
   * @param clientClass the class of the interface
   * @param httpClient  the HTTP client to use
   * @param sysConfig   the system configuration client, used to lookup connection info (hostname,
   *                    port, etc.) and configuration for HTTP client
   * @param <T>         the type of the interface, same as the return type
   * @return an instance of the client interface
   * @throws NullPointerException     if clientClass or httpClient is null
   * @throws IllegalArgumentException if the clientClass doesn't have @Component
   */
  static <T> T createClient(Class<T> clientClass, ServiceClientJdkHttp httpClient,
      SystemConfig sysConfig) {
    Objects.requireNonNull(clientClass, "Cannot create client from null class");
    Objects.requireNonNull(httpClient, "Cannot create client from null httpClient");

    final URL url = sysConfig.getUrl();
    final Duration timeout = sysConfig.getValueAsDuration(SystemConfig.CLIENT_TIMEOUT);
    final Map<Method, PathMethod> pathMethods = pathMethodsByMethod(clientClass);
    return clientClass.cast(Proxy.newProxyInstance(
        ClientGenerator.class.getClassLoader(),
        new Class[]{clientClass}, handler(httpClient, url, timeout, pathMethods)));
  }
  
  private static String getComponentName(Class clientClass) {
    return AnnotationUtils.findClassAnnotation(clientClass, Component.class)
        .orElseThrow(() -> new IllegalArgumentException("Client interface must have @Component"))
        .value();
  }

  private static <T> Map<Method, PathMethod> pathMethodsByMethod(Class<T> clientClass) {
    return ServiceReflectionUtilities.findPathAnnotatedMethodsOnlyOrThrow(clientClass)
        .stream().collect(Collectors.toMap(PathMethod::getMethod, Function.identity()));
  }

  private static InvocationHandler handler(ServiceClientJdkHttp client,
      URL url, Duration timeout, Map<Method, PathMethod> pathMethods) {
    return (proxyObj, method, args) -> sendRequest(
        client, args[0], url, timeout, pathMethods.get(method));
  }

  private static Object sendRequest(ServiceClientJdkHttp httpClient,
      Object requestBody, URL url, Duration timeout, PathMethod pathMethod)
      throws MalformedURLException {
    final Method m = pathMethod.getMethod();
    // if the method returns void, deserialize as string (not void, which doesn't work)
    final Type responseType = ServiceReflectionUtilities.methodReturnsVoid(m)
        ? String.class : m.getGenericReturnType();
    return httpClient.send(ServiceRequest.from(
        appendToUrl(url, pathMethod.getRelativePath()), requestBody,
        timeout, responseType,
        pathMethod.getInputFormat(), pathMethod.getOutputFormat()));
  }

  private static URL appendToUrl(URL url, String path) throws MalformedURLException {
    return new URL(url.getProtocol(), url.getHost(), url.getPort(),
        url.getFile() + path, null);
  }
}
