package de.btu.kt.syx.util;

/**
 * Global message logger singleton.
 * 
 * <p>This class does not expose public methods. Access to the logger is gained
 * by implementing the {@link ILogger} interface.</p>
 * 
 * @author Matthias Wolff
 */
final class Logger
{
  /**
   * Horizontal ruler length (number of {@code '-'}s) 
   */
  private static int hrlen = 89;

  /**
   * Log indent (number of spaces).
   */
  private static String indent = "";

  /**
   * The most recent log ID. Implementations of {@link ILogger} provide their
   * (unique) log IDs. If {@link #log(String, boolean, int, String)} is invoked
   * with another but the {@code lastLogID}, a line break will be inserted into
   * the log.
   */
  private static String lastLogID = "";

  /**
   * Sets the log indent.
   * 
   * @param n The new log indent
   */
  protected static void setLogIndent(int n)
  {
    if (n<=0)
      indent = "";
    else
      indent = " ".repeat(n);
  }

  /**
   * Increments or decrements the log indent.
   * 
   * @param n The increment to add
   */
  private static void addToLogIndent(int n)
  {
    int l = Math.max(0,indent.length()+n);
    indent = "";
    for (int i=0; i<l; i++) indent = indent+" ";
  }

  /**
   * Returns a horizontal rule as a string.
   */
  protected static String getHrule()
  {
    return "-".repeat(hrlen-indent.length());
  }

  /**
   * Writes a message to the log.
   * 
   * @param logID
   *          The logger's ID
   * @param ii 
   *          Increment indent, positive increments will be applied 
   *          <em>after</em> printing the message, negative increments will be 
   *          applied <em>before</em> printing the message
   * @param msg
   *          The message
   */
  protected static void log(String logID, boolean bErrLog, int ii, String msg)
  {
    String[] lines = msg.split("\\r?\\n|\\r",-1);
    String   log   = "";

    // Decrement log indent
    if (ii<0)
      addToLogIndent(ii);
    
    // Newline if new log line breaks unfinished last log line
    if (!lastLogID.equals("") && !logID.equals(lastLogID))
    {
      log = log+"\n";
      lastLogID = "";
    }
    
    for (int i=0; i<lines.length; i++)
    {
      if (i==0 && logID.equals(lastLogID))
      {
        // Finish last log line
        log = log + lines[i];
        if (i<lines.length-1) 
        {
          log = log + "\n";
          lastLogID = "";
        }
      }
      else
      {
        if (i<lines.length-1 || lines[i].length()>0)
          log = log + String.format("%-8s: %s%s",logID,indent,lines[i]);
        if (i<lines.length-1) 
        {
          log = log + "\n";
          lastLogID = "";
        }
        else 
          lastLogID = lines[i].length()>0 ? logID : "";
      }
    }
    
    // Log to console
    if (bErrLog)
      System.err.print(log);
    else
      System.out.print(log);

    // Increment log indent
    if (ii>0)
      addToLogIndent(ii);
  }

}

// EOF