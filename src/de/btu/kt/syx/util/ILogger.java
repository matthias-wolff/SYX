package de.btu.kt.syx.util;

/**
 * Interface to the {@linkplain Logger global message logger}. Classes that want
 * to use the global must implement this interface.
 * 
 * @author Matthias Wolff
 */
public interface ILogger
{

  // -- Abstract API ----------------------------------------------------------

  /**
   * Returns the ID of this logger (max. 8 characters).
   */
  public String getLogID();

  /**
   * Returns the verbose level of this logger. Log messages 
   */
  public int getVerbose();

  // -- Default API: Normal Log (stdout) --------------------------------------

  /**
   * Writes a message to the log.
   * 
   * @param v
   *          The verbose level of the message. The message will only be written
   *          if {@code v<=}{@link #getVerbose()}
   * @param ii 
   *          Increment indent, positive increments will be applied 
   *          <em>after</em> printing the message, negative increments will be 
   *          applied <em>before</em> printing the message
   * @param format
   *          The message format string
   * @param args
   *          The argument(s) to the format string
   */
  public default void log(int v, int ii, String format, Object... args)
  {
    if (v<=getVerbose())
      if (args==null || args.length==0)
        Logger.log(getLogID(),false,ii,format);
      else
        Logger.log(getLogID(),false,ii,String.format(format,args));
  }

  /**
   * Writes a message to the log; shortcut for {@link #log(int, int, String,
   * Object...) log}{@code (v,0,msg,args)}. 
   * 
   * @param v
   *          The verbose level of the message. The message will only be written
   *          if {@code v<=}{@link #getVerbose()}.
   * @param format
   *          The message format string
   * @param args
   *          The argument(s) to the format string
   */
  public default void log(int v, String format, Object... args)
  {
    log(v,0,format,args);
  }

  /**
   * Writes a message to the log; shortcut for {@link #log(int, String, 
   * Object...) log}{@code (0,msg,args)}.
   * 
   * @param format
   *          The message format string
   * @param args 
   *          The argument(s) to the format string
   */
  public default void log(String format, Object... args)
  {
    log(0,0,format,args);
  }

  /**
   * Writes a throwable to the log (on all verbose levels).
   * 
   * @param e The throwable
   */
  public default void log(Throwable e)
  {
    e.printStackTrace();
  }

  /**
   * Writes a horizontal rule to the log.
   * 
   * @param v The verbose level of the rule
   */
  public default void logHrule(int v)
  {
    log(v,"%s\n",Logger.getHrule());
  }

  /**
   * Writes a horizontal rule to the log. Shortcut for {@link #logHrule(int) 
   * logHrule}{@code (0)}.
   */
  public default void logHrule()
  {
    logHrule(0);
  }

  // -- Default API: Error Log (stderr) ---------------------------------------

  /**
   * Writes a message to the error log.
   * 
   * @param v
   *          The verbose level of the message. The message will only be written
   *          if {@code v<=}{@link #getVerbose()}
   * @param ii 
   *          Increment indent, positive increments will be applied 
   *          <em>after</em> printing the message, negative increments will be 
   *          applied <em>before</em> printing the message
   * @param format
   *          The message format string
   * @param args
   *          The argument(s) to the format string
   */
  public default void errlog(int v, int ii, String format, Object... args)
  {
    if (v<=getVerbose())
      if (args==null || args.length==0)
        Logger.log(getLogID(),true,ii,format);
      else
        Logger.log(getLogID(),true,ii,String.format(format,args));
  }

  /**
   * Writes a message to the error log; shortcut for {@link #errlog(int, int,
   * String, Object...) errlog}{@code (v,0,msg,args)}. 
   * 
   * @param v
   *          The verbose level of the message. The message will only be written
   *          if {@code v<=}{@link #getVerbose()}.
   * @param format
   *          The message format string
   * @param args
   *          The argument(s) to the format string
   */
  public default void errlog(int v, String format, Object... args)
  {
    errlog(v,0,format,args);
  }

  /**
   * Writes a message to the error log; shortcut for {@link #errlog(int, String,
   * Object...) errlog}{@code (0,msg,args)}.
   * 
   * @param format
   *          The message format string
   * @param args 
   *          The argument(s) to the format string
   */
  public default void errlog(String format, Object... args)
  {
    errlog(0,0,format,args);
  }

  /**
   * Writes a throwable to the log (on all verbose levels).
   * 
   * @param e The throwable
   */
  public default void errlog(Throwable e)
  {
    e.printStackTrace();
  }

  // -- Default API: General Log (stdout and stderr) --------------------------
//
//  /**
//   * Auxiliary static version of {@link #log(int, int, String, Object...)}. The
//   * verbose level is always 0.
//   * 
//   * @param logID
//   *          The log ID (max. 8 characters)
//   * @param ii 
//   *          Increment indent, positive increments will be applied 
//   *          <em>after</em> printing the message, negative increments will be 
//   *          applied <em>before</em> printing the message
//   * @param format
//   *          The message format string
//   * @param args
//   *          The argument(s) to the format string
//   */
//  public static void slog(String logID, int ii, String format, Object... args)
//  {
//    if (args==null || args.length==0)
//      Logger.log(logID,false,ii,format);
//    else
//      Logger.log(logID,false,ii,String.format(format,args));
//  }
//
//  /**
//   * Auxiliary static version of {@link #errlog(int, int, String, Object...)}.
//   * The verbose level is always 0.
//   * 
//   * @param logID
//   *          The log ID (max. 8 characters)
//   * @param ii 
//   *          Increment indent, positive increments will be applied 
//   *          <em>after</em> printing the message, negative increments will be 
//   *          applied <em>before</em> printing the message
//   * @param format
//   *          The message format string
//   * @param args
//   *          The argument(s) to the format string
//   */
//  public static void serrlog(String logID, int ii, String format, Object... args)
//  {
//    if (args==null || args.length==0)
//      Logger.log(logID,true,ii,format);
//    else
//      Logger.log(logID,true,ii,String.format(format,args));
//  }
//
//  /**
//   * Writes a throwable to the log (on all verbose levels).
//   * 
//   * @param e The throwable
//   */
//  public static void serrlog(Throwable e)
//  {
//    e.printStackTrace();
//  }

  /**
   * Sets the log indent.
   * 
   * @param n
   *          The new log indent
   */
  public default void setLogIndent(int n)
  {
    Logger.setLogIndent(n);
  }

}

// EOF