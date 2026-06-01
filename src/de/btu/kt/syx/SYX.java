package de.btu.kt.syx;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;

/**
 * SYX constants and static utilities
 * 
 * @author Matthias Wolff
 */
public class SYX
{
  // -- Constants -------------------------------------------------------------

  // -  General Error Messages  - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_NULL
  = "Argument '%s' must not be null";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_NULLEMPTY
  = "Argument '%s' must not be null or empty";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_TOOSHORT
  = "Argument '%s' contains too few values";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_BADLEN
  = "Argument '%s' is null or contains wrong number of values (must be %d)";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_NEG
  = "Argument '%s' must not be negative";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_INVAL
  = "Value '%s' for argument '%s' is invalid";

  /**
   * Error detail message format string.
   */
  public static final String E_ARG_DUPVAL
  = "Arguments '%s' must not contain duplicate values";

  /**
   * Error detail message format string.
   */
  public static final String E_ARGS_DIFFLEN
  = "Arguments '%s' and '%s' must have the same lenght";

  // -  Options of getResourcePath  - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Option for {@link SYX#getPath(Class, String, int) SYX.getPath(...)}: Path
   * in source tree; default is in binary tree.
   */
  public static final int SOURCE = 0x01;

  /**
   * Option for {@link SYX#getPath(Class, String, int) SYX.getPath(...)}: Path
   * relative to the root if the binary tree or source tree, default is relative
   * to class file.
   */
  public static final int ROOT = 0x02;

  /**
   * Option for {@link SYX#getPath(Class, String, int) SYX.getPath(...)}: Create
   * resource (folder); default is do not create.
   */
  public static final int CREATE = 0x02;

  // -- Constructor not Supported ---------------------------------------------

  /**
   * This class cannot be instantiated.
   */
  private SYX()
  {
    throw new NotSupportedException();
  }

  // -- Bit Processing --------------------------------------------------------

  /**
   * Sets or clears a bit in a byte.
   * 
   * @param b   The byte to write into
   * @param pos The bit in {@code b} to set or clear, 0...7
   * @param val The new bit value
   * @return The modified value of {@code b}
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static byte bSet(byte b, int pos, boolean val)
  throws IllegalArgumentException
  {
    if (pos<0 || pos>7)
      throw SYX.IllArgExc("Argument 'pos'=%d out of range",pos);
    if (val)
      b |= 1<<pos;
    else
      b &= ~(1<<pos);
    return b;
  }
  
  /**
   * Sets or clears a bit in a byte.
   * 
   * @param b   The source byte
   * @param pos The bit to set or clear, 0...7
   * @param val The new bit value, 1 iff val!=0 and 0 otherwise
   * @return The modified byte
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static byte bSet(byte b, int pos, int val)
  {
   return SYX.bSet(b,pos,val!=0);
  }
  
  /**
   * Sets a bit in a byte.
   * 
   * @param b   The source byte
   * @param pos The bit to set, 0...7
   * @return The modified byte
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static byte bSet(byte b, int pos)
  throws IllegalArgumentException
  {
    return SYX.bSet(b,pos,true);
  }

  /**
   * Sets or clears a bit in a word.
   * 
   * @param w   The source word
   * @param pos The bit to set or clear, 0...31
   * @param val The new bit value
   * @return The modified word
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static int bSet(int w, int pos, boolean val)
  {
    if (pos<0 || pos>31)
      throw SYX.IllArgExc("Argument 'pos'=%d out of range",pos);
    if (val)
      w |= 1<<pos;
    else
      w &= ~(1<<pos);
    return w;
  }

  /**
   * Sets or clears a bit in a word.
   * 
   * @param w   The source word
   * @param pos The bit to set or clear, 0...31
   * @param val The new bit value, 1 iff val!=0 and 0 otherwise
   * @return The modified word
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static int bSet(int w, int pos, int val)
  {
   return SYX.bSet(w,pos,val!=0);
  }

  /**
   * Sets a bit in a word.
   * 
   * @param w   The source word
   * @param pos The bit to set, 0...31
   * @return The modified word
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static int bSet(int w, int pos)
  {
   return SYX.bSet(w,pos,true);
  }
  
  /**
   * Clears a bit in a byte.
   * 
   * @param b   The source byte
   * @param pos The bit to clear, 0...7
   * @return The modified byte
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static byte bClr(byte b, int pos)
  throws IllegalArgumentException
  {
    return SYX.bSet(b,pos,false);
  }
  
  /**
   * Clears a bit in a word.
   * 
   * @param w   The source byte
   * @param pos The bit to clear, 0...31
   * @return The modified word
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static int bClr(int w, int pos)
  throws IllegalArgumentException
  {
    return SYX.bSet(w,pos,false);
  }

  /**
   * Gets a bit from a byte.
   * 
   * @param b   The source byte
   * @param pos The bit to get, 0...7
   * @return The modified byte
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static boolean bGet(byte b, int pos)
  throws IllegalArgumentException
  {
    if (pos<0 || pos>7)
      throw SYX.IllArgExc("Argument 'pos'=%d out of range",pos);
    return (b & (1<<pos))!=0;
  }

  /**
   * Gets a bit from a word.
   * 
   * @param w   The source byte
   * @param pos The bit to get, 0...31
   * @return The modified byte
   * @throws IllegalArgumentException
   *           if {@code pos} is out of range 
   */
  public static boolean bGet(int w, int pos)
  throws IllegalArgumentException
  {
    if (pos<0 || pos>31)
      throw SYX.IllArgExc("Argument 'pos'=%d out of range",pos);
    return (w & (1<<pos))!=0;
  }

  // -- Number Conversion -----------------------------------------------------
  
  /**
   * Converts to byte to an unsigned integer.
   * 
   * @param b The byte
   * @return The unsigned integer.
   */
  public static int b2i(byte b)
  {
    return b&0xFF;
  }

  /**
   * Converts an unsigned integer to a byte.
   * @param i The unsigned integer 
   * @return The byte
   */
  public static byte i2b(int i)
  {
    return (byte)(i&0xFF);
  }

  // -- String Representations of Numbers -------------------------------------
  
  /**
   * Converts a byte to a hexadecimal number string.
   * 
   * @param b The byte
   * @return The string representation, "00"..."FF"
   */
  public static String b2sH(byte b)
  {
    return String.format("%02X",b);
  }

  /**
   * Converts a byte to a binary number string.
   * 
   * @param b The byte
   * @return The string representation, "00000000"..."11111111"
   */
  public static String b2sB(byte b)
  {
    int v = SYX.b2i(b);
    return String.format("%8s",Integer.toBinaryString(v)).replace(" ","0");
  }

  /**
   * Converts a string representing a hexadecimal number to a byte.
   * 
   * @param s The string, "00"..."FF" case does no matter
   * @return The byte
   * @throws IllegalArgumentException
   *           if {@code s} cannot be converted to a byte
   */
  public static byte s2bH(String s)
  throws IllegalArgumentException
  {
    int i = Integer.parseInt(s,16);
    if (i<0||i>255)
      throw SYX.IllArgExc("Argument 's'=%s out of range",s);
    return i2b(i);
  }

  /**
   * Converts a string representing a binary number to a byte.
   * 
   * @param s The string, "00000000"..."11111111"
   * @return The byte
   * @throws IllegalArgumentException
   *           if {@code s} cannot be converted to a byte
   */
  public static byte s2bB(String s)
  throws IllegalArgumentException
  {
    int i = Integer.parseInt(s,2);
    if (i<0||i>255)
      throw SYX.IllArgExc("Argument 's'=%d out of range",s);
    return i2b(i);
  }

  // -- Number Range Utilities ------------------------------------------------

  /**
   * Checks whether an integer value is permissible.
   * 
   * @param val
   *          The value
   * @param valMin
   *          The smallest permissible value
   * @param valMax
   *          The greatest permissible value
   * @return {@code true} if the value is permissible, {@code false} otherwise
   */
  public static boolean valueIn(int val, int valMin, int valMax)
  {
    return (val>=valMin && val<=valMax);
  }

  /**
   * Checks whether an integer value is permissible.
   * 
   * @param val
   *          The value
   * @param permissible
   *          An array of permissible midiValues
   * @return {@code true} if the value is permissible, {@code false} otherwise
   */
  public static boolean valueIn(int val, int[] permissible)
  {
    for (int i=0; i<permissible.length; i++)
      if (val==permissible[i])
        return true;
    return false;
  }

  /**
   * Returns a range of integers.
   * 
   * @param valMin
   *          The smallest value of the range
   * @param valMax
   *          The greatest value of the range (inclusive)
   * @return The array [valMin,valMax]
   */
  public static int[] getIntRange(int valMin, int valMax)
  {
    int[] vals = new int[valMax-valMin+1];
    for (int val=valMin; val<=valMax; val++)
      vals[val-valMin]=val;
    return vals;
  }

  // -- Detuning Encoders/Decoders --------------------------------------------

  /**
   * Encodes detuning model values into a float.
   * 
   * @param pol
   *          The detune polarity &isin; {&hairsp;-1,&hairsp;+1&hairsp;}
   * @param semitones
   *          The number of semitones, non-negative
   * @param cents
   *          The number of cents &isin; [0,99]
   * @return The detuning in semitones, decimal places denote fine detuning
   *         in cents
   * @throws IllegalArgumentException
   *          if any argument is out of range
   * 
   * @see #decodeDetunePol(float)
   * @see #decodeDetuneSemitones(float)
   * @see #decodeDetuneCents(float)
   */
  public static final float encodeDetune(int pol, int semitones, int cents)
  throws IllegalArgumentException
  {
    if (pol!=-1 && pol!=+1)
      throw SYX.IllArgExc(SYX.E_ARG_INVAL,pol,"pol");
    if (semitones<0)
      throw SYX.IllArgExc(SYX.E_ARG_INVAL,semitones,"semitones");
    if (cents<0 || cents>99)
      throw SYX.IllArgExc(SYX.E_ARG_INVAL,cents,"cents");

    return  pol * semitones + ((float)cents)/100.f;
  }

  /**
   * Decodes the detune polarity {@linkplain #encodeDetune(int, int, int)
   * encoded} in a float value.
   * 
   * @param detune
   *          The detuning in semitones, decimal places denote fine detuning
   *          in cents
   * @return The detune polarity &isin; {&hairsp;-1,&hairsp;+1&hairsp;}
   * 
   * @see #encodeDetune(int, int, int)
   * @see #decodeDetuneSemitones(float)
   * @see #decodeDetuneCents(float)
   */
  public static final int decodeDetunePol(float detune)
  {
    return int_decodeDetuneInCents(detune)<0.f ? -1 : +1;
  }

  /**
   * Decodes the number of detuning semitones {@linkplain #encodeDetune(int,
   * int, int) encoded} in a float value.
   * 
   * @param detune
   *          The detuning in semitones, decimal places denote fine detuning
   *          in cents
   * @return The number of semitones, non-negative
   * 
   * @see #encodeDetune(int, int, int)
   * @see #decodeDetunePol(float)
   * @see #decodeDetuneCents(float)
   */
  public static final int decodeDetuneSemitones(float detune)
  {
    return Math.abs(int_decodeDetuneInCents(detune)) / 100;
  }

  /**
   * Decodes the number of detuning cents {@linkplain #encodeDetune(int, int,
   * int) encoded} in a float value.
   * 
   * @param detune
   *          The detuning in semitones, decimal places denote fine detuning
   *          in cents
   * @return The number of cents &isin; [0,99]
   * 
   * @see #encodeDetune(int, int, int)
   * @see #decodeDetunePol(float)
   * @see #decodeDetuneSemitones(float)
   */
  public static final int decodeDetuneCents(float detune)
  {
    return Math.abs(int_decodeDetuneInCents(detune)) % 100;
  }

  /**
   * Returns the detuning measured in integer semitones {@linkplain
   * #encodeDetune(String, int, int) encoded} in a float value.
   * 
   * @param detune
   *          The detuning in semitones, decimal places denote fine detuning
   *          in cents
   */
  private static final int int_decodeDetuneInCents(float detune)
  {
    return Math.round(detune*100.f);
  }

  // -- MIDI Data Utilities ---------------------------------------------------

  /**
   * Returns a human-readable name for a MIDI status byte.
   * 
   * @param status
   *          The status byte
   * @return The name
   */
  public static String getStatusName(int status)
  {
    String c = String.format("channel %d",status & 0x0F);
    if (status>=0x80 && status<=0x8F)
      return String.format("Note off, %s",c);
    if (status>=0x90 && status<=0x9F)
      return String.format("Note on, %s",c);
    if (status>=0xA0 && status<=0xAF)
      return String.format("Polyphonic aftertouch, %s",c);
    if (status>=0xB0 && status<=0xBF)
      return String.format("Control/mode change, %s",c);
    if (status>=0xC0 && status<=0xCF)
      return String.format("Program change, %s",c);
    if (status>=0xD0 && status<=0xDF)
      return String.format("Channel aftertouch, %s",c);
    if (status>=0xE0 && status<=0xEF)
      return String.format("Pitch wheel, %s",c);
    if (status==0xF0) return "System exclusive";
    if (status==0xF1) return "MIDI time code quarter frame";
    if (status==0xF2) return "System Common - Song position pointer";
    if (status==0xF3) return "System Common - Song select";
    if (status==0xF4) return "System Common - undefined";
    if (status==0xF5) return "System Common - undefined";
    if (status==0xF6) return "System Common - Tune request";
    if (status==0xF7) return "System Common - End of system exclusive";
    if (status==0xF8) return "System real time - MIDI clock";
    if (status==0xF9) return "System real time - undefined";
    if (status==0xFA) return "System real time - MIDI start";
    if (status==0xFB) return "System real time - MIDI continue";
    if (status==0xFC) return "System real time - MIDI stop";
    if (status==0xFD) return "System real time - undefined";
    if (status==0xFE) return "System real time - Active senseing";
    if (status==0xFF) return "System real time - Reset";
    return "(invalid)";
  }

  // -- Error, Exception and Assertion Utilities ------------------------------

  /**
   * Formats an error message.
   * 
   * @param msg
   *          The message format string
   * @param args
   *          The arguments
   * @return The formatted error message
   */
  public static String ERR(String msg, Object... args)
  {
    return String.format(msg,args);
  }

  /**
   * Returns an {@linkplain Error error object} for internal errors.
   * 
   * @param message
   *          Error message; may be {@code null} or empty
   * @return the error object
   */
  public static Error InternalError(String message)
  {
    String s = "Oops - that should not have happened (bug!?)";
    if (message!=null && message.length()>0)
      s += ". Message: "+message;
    return new Error(s);
  }

  /**
   * Returns an {@linkplain Error error object} for internal errors.
   * 
   * @param cause
   *          The cause of the error
   * @return the error object
   */
  public static Error InternalError(Throwable cause)
  {
    return new Error("Oops - that should not have happened (bug!?)",cause);
  }

  /**
   * Returns an {@code IllegalArgumentException} with a formatted detail
   * message.
   * 
   * <p>The method is equivalent to</p>
   * <p style="margin-left:3.3em">
   *   {@code new }{@link IllegalArgumentException}{@code (String.}{@link 
   *   String#format(String, Object...) format}{@code (format,args))}
   * </p>
   * 
   * <b>Usage:</b><pre>
   *     throw SYX.IllArgExc(format,args);</pre>
   * 
   * @param format
   *          The message format string
   * @param args
   *          The arguments
   * @return the Exception
   */
  public static IllegalArgumentException IllArgExc(String format, Object... args)
  {
    return new IllegalArgumentException(ERR(format,args));
  }

  /**
   * Returns an {@code InvalidMidiDataException} with a formatted detail
   * message.
   * 
   * <p>The method is equivalent to</p>
   * <p style="margin-left:3.3em">
   *   {@code new }{@link InvalidMidiDataException}{@code (String.}{@link 
   *   String#format(String, Object...) format}{@code (format,args))}
   * </p>
   * 
   * <b>Usage:</b><pre>
   *     throw SYX.InvMdataExc(format,args);</pre>
   * 
   * @param format
   *          The message format string
   * @param args
   *          The arguments
   * @return the Exception
   */
  public static NotSupportedException NotSuppExc(String format, Object... args)
  {
    return new NotSupportedException(ERR(format,args));
  }

  /**
   * Returns an {@code InvalidMidiDataException} with a formatted detail
   * message.
   * 
   * <p>The method is equivalent to</p>
   * <p style="margin-left:3.3em">
   *   {@code new }{@link InvalidMidiDataException}{@code (String.}{@link 
   *   String#format(String, Object...) format}{@code (format,args))}
   * </p>
   * 
   * <b>Usage:</b><pre>
   *     throw SYX.InvMdataExc(format,args);</pre>
   * 
   * @param format
   *          The message format string
   * @param args
   *          The arguments
   * @return The Exception
   */
  public static InvalidMidiDataException InvMdataExc(String format, Object... args)
  {
    return new InvalidMidiDataException(ERR(format,args));
  }

  /**
   * Throws an {@linkplain Error error} iff {@code condition} is {@code false}.
   * 
   * @param condition
   *          The condition
   * @param message
   *          An assertion message; may be {@code null} or empty
   */
  public static void doAssert(boolean condition, String message)
  {
    if (condition)
      return;
    
    String s = "Assertion failed";
    if (message!=null && message.length()>0)
      s += ": "+message;
    throw new Error(s);
  }

  /**
   * Throws an {@linkplain Error error} iff {@code condition} is {@code false}.
   * Convenience shortcut for {@link #doAssert(boolean, String) doAssert}{@code
   * (condition,}<code style="color:red">null</code>{@code )}.
   * 
   * @param condition
   *          The condition
   */
  public static void doAssert(boolean condition)
  {
    SYX.doAssert(condition,null);
  }

  // -- Pretty-Printing Utilities ---------------------------------------------

  /**
   * Pretty-prints a byte array to a string.
   * 
   * @param data
   *          The byte array
   * @param linePrefix
   *          A prefix string for printed lines (not applied to the first line)
   * @return The printed string
   */
  public static String prettyPrintByteArray(byte[] data, String linePrefix)
  {
    if (data==null)
      return "(null)";
    String s = "";
    for (int i=0; i<data.length; i++)
    {
      s += String.format("%02X ",data[i]);
      if ((i+1)%16==0)
        s += "\n"+linePrefix;
    }
    return s;
  }

  /**
   * Pretty-prints a {@link MidiMessage}.
   * 
   * @param message
   *          The MIDI message
   * @param timeStamp
   *          The message's time stamp, -1 for unknown
   * @return The printed string
   */
  public static String prettyPrintMidiMessage(
    MidiMessage message,
    long        timeStamp
  )
  {
    int l = message.getLength();
    int t = message.getStatus();
    byte[] data = message.getMessage();

    String s = "";
    if (timeStamp>=0)
      s += String.format("- Time stamp: %d\n",timeStamp);
    else
      s += "- Time stamp: n/a\n";
    s += String.format("- Length:     %d bytes\n",l);
    s += String.format("- Status:     %02X (%s)\n",t,getStatusName(t));
    s += String.format("- Data:      ");
    for (int i=1; i<l; i++)
    {
      if ((i>1) && (i%16==1)) 
        s += "\n             ";
      s += String.format(" %02X",data[i]);
    }

    return s;
  }

  /**
   * Pretty-prints a {@link MidiMessage}.
   * 
   * @param message
   *          The MIDI message
   * @return The printed string
   */
  public static String prettyPrintMidiMessage(MidiMessage message)
  {
    return SYX.prettyPrintMidiMessage(message,-1);
  } 

  // -- Host & Path Utilities -------------------------------------------------

  /**
   * Returns the host name.
   * 
   * <p><b>Source:</b></p>
   * <p style="text-indent:3.3em"><a 
   *   href="https://stackoverflow.com/questions/7348711/recommended-way-to-get-hostname-in-java"
   *   >Recommended way to get hostname in Java</a>, comment of Dan Ortega</p>
   * 
   * @return the host name
   */
  public static String getHostName()
  {
    try 
    {
      final String[]    cmd = {"hostname"};
      InputStream       in  = Runtime.getRuntime().exec(cmd).getInputStream();
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader    ibr = new BufferedReader(isr);
      return ibr.readLine();
    } 
    catch (IOException e) 
    {
      return "(unknown)";
    }
  }

  /**
   * Returns an absolute path relative to a class file.
   *
   * <h4>Examples:</h4>
   *<pre>
   *     // Get path relative to a class file in the source tree:
   *     SYX.getPath(clazz,relpath,{@link #SOURCE SYX.SOURCE});
   * 
   *     // Get ORK source root folder
   *     SYX.getPath(ORK.class,"",{@link #SOURCE SYX.SOURCE}));
   * 
   *     // Get ORK temporary folder
   *     SYX.getPath(ORK.class,"../tmp",{@link #ROOT SYX.ROOT}));
   * 
   *     // Get ORK resources folder in binary tree
   *     SYX.getPath(ORK.class,"resources",0));
   * 
   *     // Get subfolder in ORK tmporary folder (create if not exists)
   *     SYX.getPath(ORK.class,"../tmp/delete me!/foo/bar",{@link #ROOT SYX.ROOT}|{@link #CREATE SYX.CREATE}));
   * 
   *     // Get SYX binary root folder without specifying class and relative path
   *     SYX.getPath(null,null,0));
   *</pre>
   *
   * @param clazz
   *          The class. If {@code null}, {@link SYX}{@code .class} will be used.
   * @param relpath
   *          The path relative to the binary or source file of {@code clazz}.
   *          If {@code null} or empty, the method returns the path to the
   *          folder containing the class file.
   * @param options
   *          Options, any bitwise combination of {@link #SOURCE SYX.SOURCE},
   *          {@link #ROOT SYX.ROOT} and {@link #CREATE SYX.CREATE}
   * @return A valid, absolute path
   * @throws Error if the path could not be determined, or if the path does not
   *         exist ({@code options&}{@link #CREATE SYX.CREATE}{@code ==0}), or
   *         if the path could not be created ({@code options&}{@link #CREATE
   *         SYX.CREATE}{@code !=0})
   */
  public static String getPath(Class<?> clazz, String relpath, int options)
  {
    URL    url;                                                                // A URL
    String pth;                                                                // A path
    boolean source = (options & SYX.SOURCE)!=0;                                // Source-folder option
    boolean root   = (options & SYX.ROOT  )!=0;                                // Root of tree option
    boolean create = (options & SYX.CREATE)!=0;                                // Create resource (folder) option
    try                                                                        // try
    {                                                                          // >>
      if (clazz==null)                                                         //   Class not specified >>
        clazz = SYX.class;                                                     //     Use this class
      if (relpath==null)                                                       //   Argument relpath is null >>
        relpath="";                                                            //     Make it the empty string
      if (root)                                                                //   Relative to root folder 
        url = clazz.getProtectionDomain().getCodeSource().getLocation();       //     Get URL of binary root folder
      else                                                                     //   Relative to class folder
        url = clazz.getResource("");                                           //     Get binary package URI
      pth = Paths.get(url.toURI())                                             //     Get path for URL 
             .resolve(relpath)                                                 //     ... append relative path
             .toAbsolutePath()                                                 //     ... make absolute
             .normalize()                                                      //     ... normalize
             .toString();                                                      //     ... convert to string
      if (source)                                                              //   Resource in source tree >>
        pth = pth.replace("/bin/","/src/").replace("\\bin\\","\\src\\");       //     Change binary -> source folder
      Path p = Paths.get(pth);                                                 //   Get Path for string
      if (create)                                                              //   If file should be created >>
        Files.createDirectories(p);                                            //     Try it
      if (!Files.exists(p))                                                    //   File does not exist >>
        throw new FileNotFoundException(pth);                                  //     Throw exception
      return pth;                                                              //   Return verified absolute path
    }                                                                          // <<
    catch (Exception e)                                                        // Catch anything
    {                                                                          // >>
      throw new Error(e);                                                      //   Throw error
    }                                                                          // <<
  }

  /**
   * Returns the file extension.
   * 
   * @param filename
   *          The file name
   * @return the file extension
   */
  public static String getFileExtension(String filename)
  {
    if (filename==null) 
      return null;
    int dotIndex = filename.lastIndexOf(".");
    if (dotIndex>=0)
      return filename.substring(dotIndex+1);
    return "";
  }

}

// EOF