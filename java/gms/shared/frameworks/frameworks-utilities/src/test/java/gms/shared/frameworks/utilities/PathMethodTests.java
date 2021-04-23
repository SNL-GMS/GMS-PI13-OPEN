package gms.shared.frameworks.utilities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import gms.shared.frameworks.common.ContentType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PathMethodTests {

  private static final Method method = PathMethodTests.class.getMethods()[0];

  @ParameterizedTest
  @ValueSource(strings = {"/pathString", "pathString"})
  void testFrom(String path) {
    final PathMethod pathMethod = PathMethod.from(path, method,
        ContentType.defaultContentType(), ContentType.MSGPACK);

    assertNotNull(pathMethod);
    assertAll(
        // whether the given path did or did not have a leading slash, PathMethod should add a leading slash
        () -> assertEquals("/pathString", pathMethod.getRelativePath()),
        () -> assertEquals(method, pathMethod.getMethod()),
        () -> assertEquals(pathMethod.getInputFormat(), ContentType.defaultContentType()),
        () -> assertEquals(pathMethod.getOutputFormat(), ContentType.MSGPACK)
    );
  }
}
