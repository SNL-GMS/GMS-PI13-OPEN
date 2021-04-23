package gms.shared.frameworks.osd.api.util;

import gms.shared.frameworks.coi.exceptions.StorageUnavailableException;
import java.util.Arrays;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.JDBCConnectionException;

public class RepositoryExceptionUtils {

  private RepositoryExceptionUtils() {
    // prevent instantiation
  }

  public static Exception wrap(Exception e) {
    if (isStorageUnavailableException(e)) {
      return new StorageUnavailableException(e);
    }
    return e;
  }

  public static boolean isStorageUnavailableException(Exception e) {
    return containsCause(e, JDBCConnectionException.class);
  }

  public static boolean containsCause(Exception e, Class<?> clazz) {
    return ExceptionUtils.indexOfThrowable(e, clazz) >= 0;
  }

  public static boolean containsAnyCause(Exception e, Class<?>... clazzes) {
    return Arrays.stream(clazzes).anyMatch(c -> containsCause(e, c));
  }

}
