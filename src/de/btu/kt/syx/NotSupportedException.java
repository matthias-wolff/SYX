package de.btu.kt.syx;

/**
 * General operation-not-supported exception
 * 
 * @author Matthias Wolff
 */
public class NotSupportedException extends Error
{
  private static final long serialVersionUID = 1L;

  /**
   * Creates a new NotSupportedException with {@code null} as its detail
   * message. The cause is not initialized, and may subsequently be initialized
   * by a call to {@link Error#initCause(Throwable) initCause}.
   */
  public NotSupportedException()
  {
    super();
  }

  /**
   * Creates a new NotSupportedException with the specified detail message. The
   * cause is not initialized, and may subsequently be initialized by a call to
   * {@link Error#initCause(Throwable) initCause}.
   * 
   * @param message
   *          The detail message. The detail message is saved for later
   *          retrieval by the {@link Error#getMessage() getMessage()} method.
   */
  public NotSupportedException(String message)
  {
    super(message);
  }

  /**
   * Creates a new NotSupportedException with the specified cause and a detail
   * message of ({@code cause==null ? null : cause.toString())} (which typically
   * contains the class and detail message of {@code cause}).
   * 
   * @param cause
   *          The cause (which is saved for later retrieval by the {@link
   *          Error#getCause() getCause()} method). (A {@code null} value is
   *          permitted, and indicates that the cause is nonexistent or
   *          unknown.)
   */
  public NotSupportedException(Throwable cause)
  {
    super(cause);
  }

  /**
   * Creates a new NotSupportedException with the specified detail message and
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
  public NotSupportedException(String message, Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Creates a new NotSupportedException with the specified detail message,
   * cause, suppression enabled or disabled, and writable stack trace enabled or
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
  public NotSupportedException
  (
    String    message,
    Throwable cause,
    boolean   enableSuppression,
    boolean   writableStackTrace
  )
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}

// EOF