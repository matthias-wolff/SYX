package de.btu.kt.syx;

/**
 * Error indicating an illegal data or program state. Errors of this type occur
 * most probably because of a bug and should therefore not be caught or handled.
 * 
 * @author Matthias Wolff
 */
public class IllegalStateError extends Error
{
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new IllegalStateError with {@code null} as its detail message.
   * The cause is not initialized, and may subsequently be initialized by a call
   * to {@link Error#initCause(Throwable) initCause}.
   */
  public IllegalStateError()
  {
    super();
  }

  /**
   * Creates a new IllegalStateError with the specified detail message. The
   * cause is not initialized, and may subsequently be initialized by a call to
   * {@link Error#initCause(Throwable) initCause}.
   * 
   * @param message
   *          The detail message. The detail message is saved for later
   *          retrieval by the {@link Error#getMessage() getMessage()} method.
   */
  public IllegalStateError(String message)
  {
    super(message);
  }

  /**
   * Creates a new IllegalStateError with the specified cause and a detail
   * message of ({@code cause==null ? null : cause.toString())} (which typically
   * contains the class and detail message of {@code cause}).
   * 
   * @param cause
   *          The cause (which is saved for later retrieval by the {@link
   *          Error#getCause() getCause()} method). (A {@code null} value is
   *          permitted, and indicates that the cause is nonexistent or
   *          unknown.)
   */
  public IllegalStateError(Throwable cause)
  {
    super(cause);
  }

  /**
   * Creates a new IllegalStateError with the specified detail message and
   * cause.
   * 
   * @param message
   *          The detail message. The detail message is saved for later
   *          retrieval by the {@link Error#getMessage() getMessage()} method.
   * @param cause
   *          The cause (which is saved for later retrieval by the {@link
   *          Error#getCause() getCause()} method). (A {@code null} value is
   *          permitted, and indicates that the cause is nonexistent or
   *          unknown.)
   */
  public IllegalStateError(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Creates a new IllegalStateError with the specified detail message, cause,
   * suppression enabled or disabled, and writable stack trace enabled or
   * disabled.
   * 
   * @param message
   *          The detail message. The detail message is saved for later
   *          retrieval by the {@link Error#getMessage() getMessage()} method.
   * @param cause
   *          The cause (which is saved for later retrieval by the {@link
   *          Error#getCause() getCause()} method). (A {@code null} value is
   *          permitted, and indicates that the cause is nonexistent or
   *          unknown.)
   * @param enableSuppression
   *          whether or not suppression is enabled or disabled
   * @param writableStackTrace
   *          whether or not the stack trace should be writable
   */
  public IllegalStateError
  (
    String message,
    Throwable cause,
    boolean enableSuppression,
    boolean writableStackTrace
  )
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}

// EOF