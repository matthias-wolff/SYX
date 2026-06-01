package de.btu.kt.syx.apps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.MidiUnavailableException;

import de.btu.kt.syx.test.ASyxTestCase;
import de.btu.kt.syx.util.ILogger;
import de.btu.kt.syx.util.MidiInterface;

/**
 * Base class of console-based apps.
 * 
 * @see {@link #getCommandHandlers()}
 *   for instructions how to implement command handlers
 * @see {@linkplain #main(String[]) Main method} for minimal working example
 *
 * @author Matthias Wolff,
 *         <a href="https://www.b-tu.de/en/">BTU Cottbus-Senftenberg</a>
 */
public abstract class AAppc extends ASyxTestCase
{

  // -- Nested Classes --------------------------------------------------------

  /**
   * An error report containing an error counter and a list of error messages.
   */
  public class ErrorReport
  {
    public int               nErrors  = 0;
    public ArrayList<String> messages = new ArrayList<String>();

    public ErrorReport()
    { // Do nothing
    }

    public void add(ErrorReport other)
    {
      if (other==null)
        return;
      this.nErrors += other.nErrors;
      this.messages.addAll(other.messages);
    }

    public String getMessages()
    {
      String s = "";
      for (String message : messages)
        s += (s.length()>0?"\n":"")+message;
      return s;
    }
  }

  /**
   * Thrown by {@linkplain AAppc#getCommandHandlers() command handlers} if
   * they do not accept the UI command.
   */
  public class CommandNotAcceptedException extends Exception
  {
    private static final long serialVersionUID = 1L;
  }
  
  // -- Constants -------------------------------------------------------------

  /**
   * No MIDI interface error message.
   */
  protected static final String E_NOMIDI
    = "Not possible (no MIDI interface connected)";

  // -- Attributes ------------------------------------------------------------

  int verbose = 0;

  Scanner input = null;

  /**
   * Returns an array containing all command handler methods of this app.
   * 
   *  @see #getCommandHandlers()
   */
  private Method[] cmdHandlers = getCommandHandlers();

  /**
   * The most recent user command
   */
  private String lastCommand = null;

  /**
   * MIDI interface
   */
  private MidiInterface mi = null;

  // -- App Life Cycle --------------------------------------------------------

  /**
   * The app's start-up procedure.
   */
  protected abstract void startup();

  /**
   * Runs the app.
   * 
   * @param withMidi
   *          If {@code true} initialize a MIDI interface for the app.
   */
  protected final void run(boolean withMidi)
  {
    log("%s\nVerbose level: %d\n\n",getAppName(),getVerbose());
    if (withMidi)
      initMidi();
    startup();
    log("\nEnter 'q' to quit, '?' for help.\n");
    input = new Scanner(System.in);
    while (promptAndDispatch());
    input.close();
    disposeMidi();
    shutdown();
    log("END OF PROGRAM");
  }

  /**
   * The app's shut-down procedure.
   */
  protected abstract void shutdown();

  /**
   * Prompts for user input at the system console and dispatches the user input
   * to command handlers.
   * 
   * @return {@code true} if the prompt-and-dispatch loop should be continued
   *         and {@code false} if the loop&mdash;and thus the
   *         program&mdash;shall be terminated
   */
  protected final boolean promptAndDispatch()
  {
    log(0,0,"> ");
    //String command = System.console().readLine().trim();
    String command = input.nextLine();

    for (Method m : cmdHandlers)
      try
      {
        boolean cont = (boolean)m.invoke(this,command);
        lastCommand = command;
        return cont;
      }
      catch (InvocationTargetException e)
      { // Continue traversing command handlers
        if (!(e.getCause() instanceof CommandNotAcceptedException))
          errlog(e);
      }
      catch (IllegalAccessException e)
      {// Should not happen
        errlog(e);
      }

    errlog
    (
      "Command '%s' is unknown (or bug in command handler method)\n",
      command
    );
    lastCommand = command;
    return true;
  }

  /**
   * Initializes a MIDI interface for this app.
   * 
   * @see #get
   * @see _OrkTest#getMidiInterfaceName()
   */
  private final void initMidi()
  {
    // Find a known MIDI interface
    String miName = null;
    try
    {
      log(2,2,"Determining MIDI interface name ...\n");
      miName = ASyxTestCase.getMidiInterfaceName();
      log(2,-2," ok\nMIDI interface is '%s'\n",miName);
    }
    catch (MidiUnavailableException e)
    {
      errlog("FAILED\n");
      log(0,-2,"");
      errlog("Cause: %s (%s)\n",e.getMessage(),e.getClass().getSimpleName());
    }

    // Create MIDI interface
    if (miName!=null)
      try
      {
        log(0,2,"Opening MIDI interface '%s' ... ",miName);
        mi = new MidiInterface(miName,getMidiVerbose());
        mi.open();
        log(0,-2,"ok\n");
      }
      catch (MidiUnavailableException e)
      {
        errlog("FAILED\n");
        log(0,-2,"");
        errlog("Cause: %s (%s)\n",e.getMessage(),e.getClass().getSimpleName());
      }
  }
 
  /**
   * Disposes of the MIDI interface for this app.
   */
  private final void disposeMidi()
  {
    MidiInterface mi = getMidiInterface();
    if (mi==null)
      return;
    log(0,2,"Closing MIDI interface '%s' ... ",mi.getName());
    mi.close();
    log(0,-2,"ok\n");
  }

  // -- Getters and Setters ---------------------------------------------------

  /**
   * Returns the app name.
   */
  protected abstract String getAppName();

  /**
   * Returns the {@linkplain MidiInterface MIDI interface} used by this app.
   * 
   * @return
   *   The MIDI interface or {@code null} if the app runs without a MIDI
   *   interface
   */
  protected final MidiInterface getMidiInterface()
  {
    return mi;
  }

  /**
   * Returns the verbose level of this app's message and {@linkplain #err error} 
   * {@linkplain ILogger logger}s.
   */
  @Override
  public int getVerbose()
  {
    return this.verbose;
  }

  /**
   * Sets the verbose level of this app's message and {@linkplain #err error} 
   * {@linkplain ILogger logger}s.
   *
   * @param verbose
   *          the verbose level
   */
  public void setVerbose(int verbose)
  {
    this.verbose = verbose;
  }
  
  /**
   * Returns the verbose level of the MIDI interface. The base class
   * implementation returns 0.
   */
  protected int getMidiVerbose()
  {
    return 0;
  }

  /**
   * Returns an extra text to append to the help. The base class implementation
   * returns {@code null}.
   * 
   * @return
   *   The extra help text (may contain line breaks) or {@code null} if there
   *   is not extra help text
   */
  protected String getExtraHelpText()
  {
    return null;
  }

  // -- Command handling ------------------------------------------------------

  /**
   * Returns an array containing the command handler methods of this app, sorted
   * by method name.
   * <ul>
   *   <li>Command handler methods must be public.</li>
   *   <li>Command handler method names must start with {@code onCmd}.</li>
   *   <li>Command handler methods must have exactly one {@link String} argument
   *     (the command input by the user).</li>
   *   <li>Command handler methods must return {@code true}.</li>
   *   <li>If the command is {@code null}, the handler must print a help text.
   *     </li>
   *   <li>If the command is not {@code null}, the handler must either execute 
   *     the command or throw an {@link CommandNotAcceptedException} if it does 
   *     not handle the command.</li>
   * </ul>
   * 
   * <p><b>Example:</b><br>
   * <pre>@SuppressWarnings("unused") // Suppress compiler warning
   *public boolean onCmdXxx({@link String} command)
   *throws {@link CommandNotAcceptedException}
   *{
   *  // Print help
   *  if (command==null)
   *  {
   *    {@link #printHelpLine(String,String) printHelpLine}(/*command string*&#47;,/*one text line*&#47;); // Required
   *    {@link #printHelpLine(String) printHelpLine}(/*one text line*&#47;); // Optionally: additional line (repeat if necessary)
   *    return true;
   *  }
   *  
   *  // Check command string
   *  if (!/*handles command; check command string!*&#47;)
   *    throw new {@link CommandNotAcceptedException}();
   *
   *  // Execute command
   *  {@link #cmdResponse(String) cmdResponse}(/*short response text; one line*&#47;);
   *  
   *  try
   *  {
   *    // Your command handler code here...
   *  }
   *  catch ({@link Exception} e) // All exceptions must be caught and reported!
   *  {
   *    {@link #err}.log(e);
   *  }
   *  
   *  // Continue command loop
   *  return true; // All commands except "q" (quit) must return true!
   *}</pre>
   *
   * @see {@link #onCmdZHelp(String)} and {@link #onCmdZQuit(String)} for best
   *      practice examples
   * @see {@linkplain #main(String[]) Main method} for minimal working example
   */
  private final Method[] getCommandHandlers()
  {
    ArrayList<Method> chm = new ArrayList<Method>();
    for (Method m : getClass().getMethods())
      if (m.getName().startsWith("onCmd"))
      {
        m.setAccessible(true);
        chm.add(m);
      }
    chm.sort( (m1,m2) -> m1.getName().compareTo(m2.getName()) );
    return chm.toArray(new Method[0]);
  }

  /**
   * Returns the most recent user command. The returned value may be {@code 
   * null}.
   */
  protected final String getLastCommand()
  {
    return lastCommand;
  }

  /**
   * Prints a short UI response text before executing an UI command.
   * 
   * @param format
   *          The response format string
   * @param args 
   *          The argument(s) to the format string
   */
  protected final void cmdResponse(String format, Object... args)
  {
    String s = String.format(format,args);
    log(" ".repeat(10)+"%s\n",s);
  }

  // -- Default Command Handlers ----------------------------------------------

  /**
   * UI {@linkplain #getCommandHandlers() command handler} &ndash; List 
   * available MIDI interfaces.
   * 
   * @param command The UI command
   * @return {@code true}
   * @throws CommandNotAcceptedException if {@code command} is not "m"
   */
  public boolean onCmdXListMidiInterfaces(String command)
  throws CommandNotAcceptedException
  {
    if (command==null)
    {
      printHelpLine("mi","List available MIDI interfaces");
      return true;
    }
    if (!("mi".equals(command)))
      throw new CommandNotAcceptedException();

    cmdResponse("list MIDI devices");
    log(MidiInterface.printMidiDeviceList());

    return true;
  }

  /**
   * UI {@linkplain #getCommandHandlers() command handler} &ndash; Print help.
   * 
   * @param command
   *          The UI command
   * @return {@code true}
   * @throws CommandNotAcceptedException if {@code command} is not "?"
   */
  public final boolean onCmdZzHelp(String command)
  throws CommandNotAcceptedException
  {
    if (command==null)
    {
      printHelpLine("?","Print this help");
      return true;
    }
    if (!("?".equals(command)))
      throw new CommandNotAcceptedException();

    cmdResponse("help");
    logHrule();
    log("User Commands\n");
    for (Method m : cmdHandlers)
      try
      {
        m.invoke(this,(Object)null);
      }
      catch (InvocationTargetException | IllegalAccessException e)
      {// Should not happen
        e.printStackTrace();
      }
    String extra = getExtraHelpText();
    if (extra!=null && extra.length()>0)
      log("\n"+extra+"\n");
    logHrule();
    return true;
  }

  public final boolean onCmdZaVerbose(String command)
  throws CommandNotAcceptedException
  {
    if (command==null)
    { // Print help
      printHelpLine("v<n>","Set verbose level <n>");
      return true;
    }
    Pattern pattern = Pattern.compile("v([0-9])");
    Matcher matcher = pattern.matcher(command);
    if (!matcher.matches())
      throw new CommandNotAcceptedException();

    int v = Integer.valueOf(matcher.group(1));
    cmdResponse("verbose level: %d",v);
    setVerbose(v);

    return true;
  }

  /**
   * UI {@linkplain #getCommandHandlers() command handler} &ndash; Quit the app.
   * 
   * @param command
   *          The UI command
   * @return {@code false}
   * @throws CommandNotAcceptedException if {@code command} is not "q"
   */
  public final boolean onCmdZzQuit(String command)
  throws CommandNotAcceptedException
  {
    if (command==null)
    { // Print help
      printHelpLine("q","Quit program");
      return true;
    }
    if (!("q".equals(command)))
      throw new CommandNotAcceptedException();

    cmdResponse("quit");
    return false;
  }

  // -- Printing Help ---------------------------------------------------------

  /**
   * Returns the length of a help line prefix. Implementations should return the
   * length of the longest command string.
   */
  protected abstract int getHelpPrefixLength();

  /**
   * Prints one line of help text.
   * 
   * @param prefix
   *          The line prefix, typically the command string
   * @param textLine
   *          The help text, must not contain line breaks
   */
  protected final void printHelpLine(String prefix, String textLine)
  {
    int    prfLen = getHelpPrefixLength();
    String delim  = "".equals(prefix) ? " " : ":";
    String format = String.format("  %%-%ds%s %%s",prfLen,delim);
    log(format+"\n",prefix,textLine);
  }

  /**
   * Prints one line of help text without a line prefix, shortcut for {@link
   * #printHelpLine(String, String) printHelpLine}{@code ("",textLine)}.
   * 
   * @param textLine
   *          The help text, must not contain line breaks
   */
  protected final void printHelpLine(String textLine)
  {
    printHelpLine("",textLine);
  }

  // == MAIN (Minimal Demo APP) ===============================================

  /**
   * A minimal demo app. Provides a hello world command "hello" as an example,
   * and a command "mi" which lists all available MIDI interfaces.
   * 
   * @param args
   *          The command line arguments
   */
  public static void main(String[] args)
  {
    AAppc theApp = new AAppc()
    {

      @Override
      protected String getAppName()
      {
        return "SYX CONSOLE APP DEMO";
      }

      @Override
      public String getLogID()
      {
        return "DEMO APP";
      }

      @Override
      protected void startup()
      {
        // Do nothing
      }

      @Override
      protected void shutdown()
      {
        // Do nothing
      }

      @Override
      protected int getHelpPrefixLength()
      {
        return 8;
      }

      /**
       * UI {@linkplain #getCommandHandlers() command handler} &ndash; 
       * Hello world command
       * 
       * @param command The UI command
       * @return {@code true}
       * @throws CommandNotAcceptedException
       *   if {@code command} is not "hello"
       */
      @SuppressWarnings("unused") // Suppress compiler warning
      public boolean onCmdHelloWorld(String command)
      throws CommandNotAcceptedException
      {
        // Print help
        if (command==null)
        {
          printHelpLine("hello","Hello World command");
          return true;
        }
        if (!"hello".equals(command))
          throw new CommandNotAcceptedException();
        
        // Execute command
        cmdResponse("Hello World!");
        try
        {
          log("Here we would handle an actual command...\n");
        }
        catch (Exception e) // All exceptions must be caught and reported!
        {
          errlog(e);
        }
        
        // Continue command loop
        return true;
      }
    };

    theApp.run(false);
  }
  
}

// EOX