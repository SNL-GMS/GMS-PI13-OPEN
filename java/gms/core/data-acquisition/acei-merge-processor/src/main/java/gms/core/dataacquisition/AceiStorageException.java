package gms.core.dataacquisition;

/**
 * Unchecked exception possibly arising from ACEI storage operations.
 */
public class AceiStorageException extends RuntimeException {

  private static final long serialVersionUID = -7769452619913634942L;

  public AceiStorageException() {}

  public AceiStorageException(String message) {
    super(message);
  }

  public AceiStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public AceiStorageException(Throwable cause) {
    super(cause);
  }
}
