package de.btu.kt.syx.midi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.SYX;

/**
 * A structured system exclusive data buffer. Instances of this class typically 
 * represent
 * <ul>
 *   <li>the body of a short system exclusive message, e.g., a parameter change 
 *   message, or</li>
 *   <li>a part of a large system exclusive message, e.g., a bulk dump.
 * </ul>
 * 
 * <h3>Format Specifiers for System Exclusive Messages</h3>
 * <p>System exclusive messages have a device-specific data format. For
 * instance, the body<sup>1</sup> of a Yamaha TG55 parameter change message has
 * the format
 * <pre>  43H 1#H 35H 0tH 0feecccc ppH[2] vvH[2]</pre> with the 
 * parameters
 * <table style="margin-left:1.3em">
 *   <tr><td>&bull;</td><td>{@code #}:</td><td>4 bits        </td><td>hardware device ID,  </td></tr>
 *   <tr><td>&bull;</td><td>{@code t}:</td><td>4 bits        </td><td>structure type,      </td></tr>
 *   <tr><td>&bull;</td><td>{@code f}:</td><td>1 bit         </td><td>element or filter,   </td></tr>
 *   <tr><td>&bull;</td><td>{@code e}:</td><td>2 bits        </td><td>element number,      </td></tr>
 *   <tr><td>&bull;</td><td>{@code c}:</td><td>4 bits        </td><td>channel number,      </td></tr>
 *   <tr><td>&bull;</td><td>{@code p}:</td><td>2&times;7 bits</td><td>parameter number, and</td></tr>
 *   <tr><td>&bull;</td><td>{@code v}:</td><td>2&times;7 bits</td><td>parameter value.     </td></tr>
 * </table>
 * <p>The format of system exclusive message is defined by a message format 
 * specifier. A message format specifier is a white-space separated string of
 * byte format specifiers:</p>
 * <ol>
 *   <li><b>Hexadecimal</b>: Two hex digits &isin; {{@code 0}, ..., {@code F}}
 *   with <em>upper-case</em> letters, followed by an upper-case {@code H}. The
 *   first digit <em>must not</em> be greater than {@code 7}.</li>
 *   <li style="margin-top:0.4em"><b>Binary</b>: Eight binary digits &isin;
 *   {{@code 0}, {@code 1}}. The fist digit <em>must</em> be {@code 0}.</li>
 * </ol>
 * <p>Any digit in the byte format specifiers can be replaced by {@code '*'} 
 * representing an arbitrary value. Further, any digit can be replaced by a 
 * lower-case letter &isin; {{@code 'a'}, ..., {@code 'z'}, {@code '#'}} which
 * defines a named parameter. Hence, at most 27 different parameters can be
 * defined in one data struct (large SysEx messages can be partitioned into
 * several data structs). Named parameters may span multiple bytes. The
 * name {@code '#'} is reserved for {@linkplain AInstrument#getDevNum() hardware
 * device numbers}. A byte format specifier can be repeated <i>n</i> times
 * by appending {@code [n]}.</p>
 * 
 * <h3>Example</h3>
 * <p>The TG55 parameter change message above can be instantiated like this:</p>
 * <pre>
 *   String format = "43H 1#H 35H 0tH 0feecccc ppH[2] vvH[2]";
 *   SyxDataStruct prt = new {@link #SyxDataStruct(String, byte[]) SyxDataStruct}(format);
 *   {@link SyxMessage} paramChangeMsg = new {@link SyxMessage#SyxMessage(SyxDataStruct...) SyxMessage}(prt);</pre>
 * <p>Now we can write/read parameter values:</p>
 * <pre>
 *   prt.{@link #setMidiValue(char, int) setMidiValue}('p',0x10);
 *   prt.{@link #setMidiValue(char, int) setMidiValue}('v',0x01FF);
 *   
 *   int p = prt.{@link #getMidiValue(char) getMidiValue}('p'); 
 *   int v = prt.{@link #getMidiValue(char) getMidiValue}('v');</pre>
 * <p>or write/read the entire data buffer:</p>
 * <pre>
 *   byte[] data;
 *   
 *   // ... Get the data 
 *   
 *   prt.{@link #setData(byte[]) setData}(data); // Data must match pattern!
 *   data = prt.{@link #getData() getData}();</pre>
 * <hr>
 * <h3 style="margin-top:3pt;">Footnotes</h3>
 * <p style="margin-top:3pt;"><sup>1</sup>
 *   The body of a system exclusive message consists of all data bytes,
 *   <em>excluding</em> the leading status byte (0xF0 or 0xF7) and the tailing 
 *   EOX flag (0xF7).</p>
 * 
 * @see SyxMessage
 * @author Matthias Wolff
 */
public class SyxDataStruct implements Serializable 
{

  private static final long serialVersionUID = 1L;

  // -- Constants -------------------------------------------------------------

  // -  Nested Classes  - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Parameter types:
   * <ul>
   *   <li>{@link #RAW}: raw parameter without {@linkplain SyxParamInfo
   *   additional parameter information}</li>
   *   <li>{@link #RANGE}: range of integer values</li>
   *   <li>{@link #ENUM_INT}: enumeration of integer values</li>
   *   <li>{@link #ENUM_STR}: enumeration of value names</li>
   * </ul>
   */
  public static enum PTYPE
  {
    /**
     * Parameter type: raw parameter without {@linkplain SyxParamInfo additional
     * parameter information}
     */
    RAW,

    /**
     * Parameter type: range of integer values
     */
    RANGE,

    /**
     * Parameter type: enumeration of integer values
     */
    ENUM_INT,

    /**
     * Parameter type: enumeration of value names
     */
    ENUM_STR,
  }

  // -  Error Messages  - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  protected static transient final String E_FSPEC_NONE
    = "Pattern must not be null or empty";

  protected static transient final String E_BFSPEC_INVAL
    = "Invalid byte format specifier '%s' at position %d";

  protected static transient final String E_BYTEINDEX
    = "Byte index out of range";

  protected static transient final String E_PARAM_UNDEF
    = "There is no parameter named '%s'";

  protected static transient final String E_PARAM_ILLNAME
    = "Parameter name '%s' is illegal";

  protected static transient final String E_VALUE_NEG
    = "Value must not be negative (parameter '%s')";

  protected static transient final String E_VALUE_TOOLARGE
    = "Value %d is too large for parameter '%s'";

  protected static transient final String E_VALUE_MINMAX
    = "Maximum value must not be greater than minimum value";

  protected static transient final String E_DATA_TOOSHORT
    = "Data buffer shorter than struct data format";

  protected static transient final String E_DATA_NOMATCH
    = "Data buffer does not match pattern at byte %d (0x%02X)";

  protected static transient final String E_PCMSG_NOFORMAT
    = "There is no parameter change message format for parameter '%s'";

  protected static transient final String E_PARAMINFO_NONE
    = "Parameter '%s' has no parameter info";

  // -- Attributes ------------------------------------------------------------

  /**
   * The name of this struct, may be {@code null}
   */
  protected String name = null;

  /**
   * The internal data buffer
   */
  protected byte[] data = null;

  /**
   * The bit-wise data format specifier
   */
  protected char[] format = null;

  /**
   * Filter mask for matching byte arrays on the data pattern
   */
  protected byte[] filterMask = null;

  /**
   * Map of struct parameter name &rarr; permissible midiValues. If a struct 
   * parameter name is not contained in the map, the permissible value range
   * will be detected automatically from the parameter's {@linkplain 
   * #getParamBitCount(char) bit count}.
   * 
   * @see #paramInfos
   * @see #getParamBitCount(char)
   */
  protected HashMap<Character,SyxParamInfo> paramInfos = null;

  /**
   * Data change listeners. 
   * 
   * <p>Listeners will be notified when the system exclusive data in this struct
   * have been changed by one of the {@link yxDataStruct#setData(byte[], int)}
   * or {@link SyxDataStruct#setData(byte[])} methods.</p> 
   * 
   * <p>The {@code setParam}&lang;{@code Xxx}&rang; methods will not notify the
   * listeners.</p>
   */
  protected transient HashSet<ISyxDataChangeListener> listeners = null;

  // -- Constructors ----------------------------------------------------------

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format
   *          The {@linkplain SyxDataStruct format specifier}
   * @param name
   *          The name of this data struct
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid
   */
  public SyxDataStruct(String format, String name)
  throws IllegalArgumentException
  {
    int_parsePattern(format);
    this.name = name;
    this.paramInfos = new HashMap<Character,SyxParamInfo>();
    this.listeners = null; // Lazy initialization
  }
  
  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format
   *          The {@linkplain SyxDataStruct format specifier}
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid
   */
  public SyxDataStruct(String format)
  throws IllegalArgumentException
  {
    this(format,(String)null);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format
   *          The {@linkplain SyxDataStruct format specifier}
   * @param name
   *          The name of this data struct
   * @param params
   *          A parameter name {@code 'a'}, ..., {@code 'z'} or {@code '#'} 
   *          ({@code char}) followed by a non-negative parameter value ({@code
   *          int}); can be repeated
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, if the argument 
   *          sequence is {@code null} or invalid, or if either argument is out
   *          of range
   * @throws IllegalArgumentException
   *          if a parameter does mentioned in {@code params} does not exist in
   *          the format or if a value is too large to be stored in the data
   *          struct
   */
  public SyxDataStruct(String format, String name, Object... params) 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,name);
    if (params!=null)
      setMidiValues(params);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format
   *          The {@linkplain SyxDataStruct format specifier}
   * @param params
   *          A parameter name {@code 'a'}, ..., {@code 'z'} or {@code '#'} 
   *          ({@code char}) followed by a non-negative parameter value ({@code
   *          int}); can be repeated
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, if the argument 
   *          sequence is {@code null} or invalid, or if either argument is out
   *          of range
   * @throws IllegalArgumentException
   *          if a parameter does mentioned in {@code params} does not exist in
   *          the format or if a value is too large to be stored in the data
   *          struct
   */
  public SyxDataStruct(String format, Object... params) 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,(String)null,params);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format 
   *          The {@linkplain SyxDataStruct format specifier}
   * @param name
   *          The name of this data struct
   * @param data
   *          A byte array to initialize the data struct from
   * @param start
   *          Zero-based index in {@code data} from which to start copying
   *          data bytes
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, if {@code data} is 
   *          {@code null}, or if {@code start} is less than zero
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains
   *          too few bytes
   */
  public SyxDataStruct(String format, String name, byte[] data, int start)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,name);
    setData(data,start);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format 
   *          The {@linkplain SyxDataStruct format specifier}
   * @param data
   *          A byte array to initialize the data struct from
   * @param start
   *          Zero-based index in {@code data} from which to start copying
   *          data bytes
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, if {@code data} is 
   *          {@code null}, or if {@code start} is less than zero
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains
   *          too few bytes
   */
  public SyxDataStruct(String format, byte[] data, int start)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,(String)null,data,start);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format 
   *          The {@linkplain SyxDataStruct format specifier}
   * @param name
   *          The name of this data struct
   * @param data
   *          A byte array to initialize the data struct from
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, if {@code data} is 
   *          {@code null}, or if {@code start} is less than zero
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains
   *          too few bytes
   */
  public SyxDataStruct(String format, String name, byte[] data)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,name,data,0);
  }

  /**
   * Creates a structured system exclusive data buffer.
   * 
   * @param format 
   *          The {@linkplain SyxDataStruct format specifier}
   * @param data
   *          A byte array to initialize the data struct from
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, or if {@code data} 
   *          is {@code null}
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains 
   *          too few bytes
   */
  public SyxDataStruct(String format, byte[] data)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,data,0);
  }

  // -- API: Getters and Setters ----------------------------------------------

  // -  Attributes  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Returns the name of this struct, may be {@code null}.
   */
  public String getName()
  {
    return this.name;
  }

  /**
   * Sets the name of this struct.
   * 
   * @param name
   *          The name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * Returns the length of this struct in bytes.
   */
  public int getLength()
  {
    return this.data.length;
  }

  /**
   * Returns the data bytes of this struc. The returned array is copy of the 
   * internal data.
   */
  public byte[] getData()
  {
    return data.clone();
  }
  
  /**
   * Sets the data bytes of this struct.
   *
   * @param data
   *          A byte array to initialize the data struct from
   * @throws IllegalArgumentException
   *          if {@code data} is {@code null}
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains
   *          too few bytes
   */
  public void setData(byte[] data)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    setData(data,0);
  }

  /**
   * Sets the data bytes of this struct.
   *
   * @param data
   *          A byte array to initialize the data struct from
   * @param start
   *          Zero-based index in {@code data} from which to start copying data
   *          bytes
   * @returns The number of bytes copied
   * @throws IllegalArgumentException
   *          if {@code data} is {@code null}, or if {@code start} is less than
   *          zero
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains
   *          too few bytes
   */
  public int setData(byte[] data, int start)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    int_match(data,start,-1);
    for (int i=0; i<this.data.length; i++)
      this.data[i] = data[i+start];
    notifyDataChanged();
    return this.data.length;
  }
  
  /**
   * Returns the format specifier of a byte in this struct.
   * 
   * @param pos
   *          The zero-base byte index
   * @return The format specifier
   * @throws IllegalArgumentException
   *          if {@code pos} is out of range
   */
  public String getByteFormat(int pos)
  throws IllegalArgumentException
  {
    if (pos<0 || pos>=getLength())
      throw SYX.IllArgExc(E_BYTEINDEX);
    String s = "";
    for (int i=pos*8; i<pos*8+8; i++)
      s += this.format[i];
    return s;
  }

  /**
   * Returns all parameter names of a byte in this struct.
   * 
   * @param pos
   *          The zero-base byte index
   * @return A character array containing the parameter names, may be empty
   * @throws IllegalArgumentException
   *          if {@code pos} is out of range
   */
  public char[] getByteParams(int pos)
  throws IllegalArgumentException
  {
    return int_getParams(getByteFormat(pos).toCharArray());
  }

  /**
   * Returns the filter mask of a byte in this struct.
   * 
   * @param pos
   *          The zero-base byte index
   * @return The filter mask
   * @throws IllegalArgumentException
   *          if {@code pos} is out of range
   */
  public byte getByteFilter(int pos)
  throws IllegalArgumentException
  {
    if (pos<0 || pos>=getLength())
      throw SYX.IllArgExc(E_BYTEINDEX);
    return this.filterMask[pos];
  }

  // -  Struct Parameters - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Returns a character array containing all parameter names of this struct
   */
  public char[] getParamNames()
  {
    return int_getParams(this.format);
  }

  /**
   * Returns the {@linkplain SyxDataStruct.PTYPE type} of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}
   */
  public PTYPE getParamType(char pname)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(pname);
    if (pi!=null)
      return pi.getType();
    else
      return PTYPE.RAW;
  }

  /**
   * Returns the number of data bits available for a parameter. If there is no 
   * such parameter in the struct, the method returns 0.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if the {@code pname} is out of range
   */
  public int getParamBitCount(char pname)
  throws IllegalArgumentException
  {
    // Sanity checks
    if (!isValidParamName(pname))
      throw SYX.IllArgExc("Invalid argument 'name'='%s'",pname);

    // Count parameter bits
    int n = 0; //N: available data bits for parameter value
    for (int i=0; i<this.format.length; i++)
      if (format[i]==pname)
        n++;

    return n;
  }

  /**
   * Verifies that {@linkplain #getParamNames() all parameters} of this struct
   * have {@linkplain SyxParamInfo parameter infos}.
   * 
   * @see #getParamInfo(char)
   * @see #addParamInfo(SyxParamInfo)
   */
  public void doAssertAllParamInfos()
  {

    for (char pname : getParamNames())
      try
      {
        SYX.doAssert
        (
          getParamInfo(pname)!=null,
          String.format("Parameter '%s' does not have parameter info",pname)
        );
      }
      catch (InvalidMidiDataException e)
      { // Cannot happen
        throw SYX.InternalError(e);
      }
  }

  /**
   * Returns the {@linkplain SyxParamInfo parameter info} of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @return  The {@linkplain SyxParamInfo parameter info}, may be {@code null}
   *          if no information is associated with the parameter
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}
   * @see #setParamInfo(char, String, int, int, int, String)
   * @see #setParamInfo(char, String, int, int, String)
   * @see #setParamInfo(char, String, int[], String[], String)
   */
  public SyxParamInfo getParamInfo(char pname)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (!isValidParamName(pname))
      throw SYX.IllArgExc("Invalid argument 'name'='%s'",pname);
    if (getParamBitCount(pname)==0)
      throw new InvalidMidiDataException(SYX.ERR(E_PARAM_UNDEF,pname));

    return this.paramInfos.get(pname);
  }

  /**
   * Adds {@linkplain SyxParamInfo parameter information} to a parameter. If
   * information for parameter {@code pi.}{@link SyxParamInfo#getPname()
   * getPname()} is already present, it will be replaced.
   * 
   * @param pi
   *          The parameter information
   * @throws IllegalArgumentException if
   *          <ul style="margin-bottom:0em">
   *            <li>{@code pi} is {@code null}</li>
   *            <li>{@code pi.}{@link SyxParamInfo#getParent()
   *              getParent()}{@code !=this}</li>
   *          </ul>
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pi.}{@link SyxParamInfo#getPname()
   *          getPname()} in the {@linkplain SyxDataStruct format}
   */
  public void addParamInfo(SyxParamInfo pi)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (pi==null)
      throw SYX.IllArgExc(SYX.E_ARG_NULL,"pi");
    if (pi.getParent()!=this)
      throw SYX.IllArgExc("Argument 'parent' must equal this");
    char pname = pi.getPname();
    if (getParamBitCount(pname)<=0)
      throw SYX.InvMdataExc(E_PARAM_UNDEF,pname);

    // Put to parameter info map
    this.paramInfos.put(pname,pi);
  }

  /**
   * Returns the default MIDI value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range, or if there is no parameter
   *          {@code pname} in the {@linkplain SyxDataStruct format}
   */
  public int getDefMidiValue(char pname)
  throws IllegalArgumentException
  {
    try
    {
      SyxParamInfo pi = getParamInfo(pname);
      return pi!=null ? pi.getDefaultMidiValue() : 0x00;
    }
    catch (InvalidMidiDataException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns the MIDI value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range, or if there is no parameter
   *          {@code pname} in the {@linkplain SyxDataStruct format}
   * @throws InvalidMidiDataException 
   */
  public int getMidiValue(char pname)
  throws IllegalArgumentException 
  {
    try
    {
      return int_copyParamBits(pname,0,false,false);
    }
    catch (InvalidMidiDataException e)
    { // Should not happen because value should have been validated on set
      throw SYX.InternalError(e);
    }
  }

  /**
   * Sets the MIDI value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param midiValue
   *          The MIDI value, a non-negative integer
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if value is too large to be stored in 
   *          the data struct
   */
  public void setMidiValue(char pname, int midiValue)
  throws InvalidMidiDataException
  {
    int_copyParamBits(pname,midiValue,true,false);
  }

  /**
   * Sets one or several parameters of this structured system exclusive data 
   * buffer.
   * 
   * @param args
   *          A parameter name {@code 'a'}, ..., {@code 'z'} or {@code '#'} 
   *          ({@code char}) followed by a non-negative MIDI value ({@code
   *          int}); can be repeated
   * @throws IllegalArgumentException
   *          if the argument sequence is {@code null} or invalid, or if either
   *          argument is out of range
   * @throws InvalidMidiDataException
   *          if a parameter mentioned in {@code params} does not exist in the 
   *          {@linkplain SyxDataStruct format}, or if a value is too large to
   *          be stored in the data struct
   */
  public void setMidiValues(Object... args)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (args==null)
      throw SYX.IllArgExc("Argument 'params' must not be null");
    for (int i=0; i<args.length-1; i+=2)
    {
      if (!(args[i] instanceof Character))
        throw SYX.IllArgExc("Argument 'params[%d]' must be a character",i);
      if (!(args[i+1] instanceof Integer))
        throw SYX.IllArgExc("Argument 'params[%d]' must be an integer",i+1);
    }

    // Set parameters
    for (int i=0; i<args.length-1; i+=2)
    {
      char pname = (char)args[i];
      int  value = (int )args[i+1];
      setMidiValue(pname,value);
    }
  }

  /**
   * Returns the model value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if there is no {@link SyxParamInfo}
   *          for the parameter
   * @see SyxParamInfo#midi2Model(int)
   */
  public int getModelValue(char pname)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(pname);
    if (pi==null)
      throw new InvalidMidiDataException();
    return pi.midi2Model(getMidiValue(pname));
  }

  /**
   * Returns a string representation of the model value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if there is no {@link SyxParamInfo}
   *          for the parameter
   * @see {@link SyxParamInfo#midi2ModelAsString(int)} for conversion rules
   */
  public String getModelValueAsString(char pname)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    int midiValue = getMidiValue(pname);
    SyxParamInfo pi = getParamInfo(pname);
    return pi.midi2ModelAsString(midiValue);
  }

  /**
   * Sets the model value of a parameter.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param modelValue
   *          The model value
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if there is no {@link SyxParamInfo}
   *          for the parameter, or if {@code modelValue} is not permissible
   * @see SyxParamInfo#model2Midi(int)
   */
  public void setModelValue(char pname, int modelValue)
  throws InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(pname);
    if (pi==null)
      throw SYX.InvMdataExc(E_PARAMINFO_NONE,pname);
    setMidiValue(pname,pi.model2Midi(modelValue));
  }

  /**
   * Sets the model value from a value name.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param valueName
   *          A string representation of the model value
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if there is no {@link SyxParamInfo}
   *          for the parameter, or if {@code valueName} is not permissible
   * @see {@link SyxParamInfo#model2Midi(String)} for conversion rules
   */
  public void setModelValue(char pname, String valueName)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(pname);
    if (pi==null)
      throw new InvalidMidiDataException();
    setMidiValue(pname,pi.model2Midi(valueName));
  }

  /**
   * Gets the values of multiple parameters as a string. This method is useful
   * to read a sequence of characters from the MIDI system exclusive data.
   *  
   * @param pnames
   *          The parameter names; either a range, e.g. "a-j", or a list, e.g. 
   *          "abcdefghij"
   * @return The midiValues
   * @throws IllegalArgumentException
   *           if {@code names} is {@code null}, empty, or not a valid specifier
   * @throws InvalidMidiDataException
   *          if any of the parameter names specified by {@code names} is not a
   *          parameter in the {@linkplain SyxDataStruct format}
   */
  public String readString(String pnames)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    String s = "";
    for (char c : int_parseParamNames(pnames))
      s += (char)getMidiValue(c);
    return s;
  }
  
  /**
   * Sets the values of multiple parameters from a string. This method is useful
   * to write a sequence of characters into the MIDI system exclusive data.
   *  
   * @param pnames
   *          The parameter names; either a range, e.g. "a-j", or a list, e.g. 
   *          "abcdefghij"
   * @param midiValues
   *          The midiValues
   * @throws IllegalArgumentException
   *          if either argument is {@code null} or empty, if {@code names} is
   *          not a valid specifier, or if the length of {@code midiValues} does not
   *          match the number of parameters specified by {@code names}
   * @throws InvalidMidiDataException
   *          if any of the parameter names specified by {@code names} is not a
   *          parameter in the {@linkplain SyxDataStruct format}
   */
  public void writeString(String pnames, String values)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (values==null || values.length()==0)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULLEMPTY,"midiValues"));
    
    char[] pn = int_parseParamNames(pnames);
    if (pn.length!=values.length())
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARGS_DIFFLEN,"names","midiValues"));

    for (int i=0; i<pn.length; i++)
      setMidiValue(pn[i],values.charAt(i));
  }

  // -- API: Validation and Reset ---------------------------------------------

  /**
   * Determines whether {@code pname} is a valid parameter name. Valid parameter
   * names are {@code 'a'}, ..., {@code 'z'} and {@code '#'}. 
   * 
   * @param pname
   *          The parameter name
   * @return {@code true} if {@code pname} is a valid parameter name, {@code 
   *         false} otherwise
   */
  public static boolean isValidParamName(char pname)
  {
    return (pname>='a' && pname<='z') || pname=='#';
  }

  /**
   * Validates the current MIDI value of a parameter.
   * 
   * @param pname
   *          The parameter name: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if {@code pname} is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if {@linkplain #getMidiValue(char) the
   *          current MIDI value} is not permissible
   */
  public void validate(char pname)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    int midiValue = getMidiValue(pname);
    int_validateMidiValue(pname,midiValue,false);
  }

  /**
   * Validates the current MIDI value of all parameters
   * 
   * @throws InvalidMidiDataException
   *          if validation fails
   */
  public void validateAll()
  throws InvalidMidiDataException
  {
    String messages = "";

    for (char pname : getParamNames())
      try
      {
        validate(pname);
      }
      catch (InvalidMidiDataException e)
      {
        if (messages.length()>0)
          messages += "\n";
        messages += e.getMessage();
      }
      catch (IllegalArgumentException e)
      { // Cannot happen
        SYX.InternalError(e);
      }

    if (messages.length()>0)
      throw SYX.InvMdataExc(messages);
  }

  /**
   * Resets the value of a parameter to its default value.
   * 
   * @param pname
   *          The parameter name, {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}
   */
  public void reset(char pname)
  throws InvalidMidiDataException
  {
    int defMidiValue = getDefMidiValue(pname);
    int_copyParamBits(pname,defMidiValue,true,true);
  }

  /**
   * Resets all parameters to their default values.
   */
  public void reset()
  {
    try
    {
      for (char pn : getParamNames())
        reset(pn);
    }
    catch (Exception e)
    {// Cannot happen
      SYX.InternalError(e);
    }
  }

  // -- API: Other ------------------------------------------------------------

  /**
   * Checks whether a data buffer matches this struct.
   * 
   * @param data 
   *          The data buffer
   * @param len
   *          The number of bytes to match, non-positive to match entire buffer
   * @return {@code true} if the {@code data} matches this struct, {@code false}
   *         otherwise
   */
  public boolean match(byte[] data, int len)
  {
    try
    {
      int_match(data,0,len);
      return true;
    }
    catch (Exception e)
    {
      // Do nothing
    }
    return false;
  }

  /**
   * Checks whether a data buffer matches this struct.
   * 
   * @param data 
   *          The data buffer
   * @return {@code true} if the {@code data} matches this struct, {@code 
   *         false} otherwise
   */
  public boolean match(byte[] data)
  {
    return match(data,-1);
  }

  /**
   * Checks whether a system exclusive message's body matches this struct.
   * 
   * @param sxMsg
   *          The system exclusive message
   * @param len
   *          The number of bytes to match, non-positive to match entire 
   *          message body
   * @return {@code true} if the message matches this pattern, {@code false}
   *         otherwise
   */
  public boolean match(SysexMessage sxMsg, int len)
  {
    return match(sxMsg.getData(),len);
  }

  /**
   * Checks whether a system exclusive message's body matches this struct.
   * 
   * @param sxMsg
   *          The system exclusive message
   * @return {@code true} if the message matches this struct, {@code false}
   *         otherwise
   */
  public boolean match(SysexMessage sxMsg)
  {
    return match(sxMsg,-1);
  }

  /**
   * Creates system exclusive parameter change messages for given parameter
   * names.
   * 
   * @param paramNames
   *          The parameter names
   * @param devNumParam
   *          The parameter name of the device number in {@code format}, should
   *          be  {@code '#'}
   * @param devNum
   *          The device number of the hardware instrument, (1, ..., {@link 
   *          AInstrument#getMaxDeviceNumber}{@code ()}).
   * @return An array of parameter change messages
   * @throws IllegalArgumentException
   *          if {@code devNum} is out of range, if {@code paramNames} is {@code
   *          null} or invalid
   * @throws InvalidMidiDataException
   *          if any of the parameter names specified by {@code paramNames} is 
   *          not a parameter in the {@linkplain SyxDataStruct format} of this
   *          data struct, if any parameter has no {@linkplain SyxParamInfo 
   *          parameter info}, or if the parameter info of any parameter does
   *          not provide a parameter change message format
   */
  public SyxMessage[] createParamChangeMsg
  (
    String paramNames,
    char   devNumParam,
    int    devNum
  ) throws IllegalArgumentException, InvalidMidiDataException
  {
    if (devNum<0)
      throw SYX.IllArgExc("Argument 'devNum'=%d out of range",devNum);
    char[] pnames = int_parseParamNames(paramNames);

    ArrayList<SyxMessage> msgs = new ArrayList<SyxMessage>();
    String paramsDone = "";
    for (char pname : pnames)
    {
      // Parameter already included in a previous param. change message 
      if (paramsDone.indexOf(pname)>=0)
        continue;

      // Get SysEx parameter change message format
      SyxParamInfo pi = getParamInfo(pname);
      if (pi==null)
        throw new InvalidMidiDataException(SYX.ERR(E_PARAMINFO_NONE,pname));
      String format = pi.getParamChangeMsgFormat();
      if (format==null)
        throw new InvalidMidiDataException(SYX.ERR(E_PCMSG_NOFORMAT,pname));

      // Create SysEx message part and set parameter values
      SyxDataStruct prt = new SyxDataStruct(pi.getParamChangeMsgFormat());
      prt.setName(this.getName()+" Parameter Change");
      for (char pn : prt.getParamNames())
        if (pn!=devNumParam)
        {
          if (this.getParamBitCount(pn)==0)
            throw new InvalidMidiDataException(SYX.ERR(E_PARAM_UNDEF,pn));
          prt.setMidiValue(pn,this.getMidiValue(pn));
          paramsDone += pn;
        }
      prt.setMidiValue(devNumParam,devNum-1);

      // Add message to list
      msgs.add(new SyxMessage(prt));
    }

    return msgs.toArray(new SyxMessage[]{});
  }

  /**
   * Creates system exclusive parameter change messages for given parameter
   * names. Equivalent to {@link #createParamChangeMsg(String, char, int)
   * createParamChangeMsg}{@code (paramNames,'#',devNum)}.
   * 
   * @param paramNames
   *          The parameter names
   * @param devNum
   *          The device number of the hardware instrument, (1, ..., {@link 
   *          AInstrument#getMaxDeviceNumber}{@code ()}).
   * @return An array of parameter change messages
   * @throws IllegalArgumentException
   *          if {@code devNum} is out of range, if {@code paramNames} is {@code
   *          null} or invalid
   * @throws InvalidMidiDataException
   *          if any of the parameter names specified by {@code paramNames} is 
   *          not a parameter in the {@linkplain SyxDataStruct format} of this
   *          data struct, if any parameter has no {@linkplain SyxParamInfo 
   *          parameter info}, or if the parameter info of any parameter does
   *          not provide a parameter change message format
   */
  public SyxMessage[] createParamChangeMsg
  (
    String paramNames,
    int    devNum
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    return createParamChangeMsg(paramNames,'#',devNum);
  }

  // -- Listeners -------------------------------------------------------------

  /**
   * Adds a data changed listener to this struct.
   * 
   * <p>Listeners will be notified when the system exclusive data in this struct
   * have been changed by one of the {@link SyxDataStruct#setData(byte[], int)}
   * or {@link SyxDataStruct#setData(byte[])} methods.</p> 
   * 
   * <p>The {@code setParam}&lang;{@code Xxx}&rang; methods will not notify the
   * listeners.</p>
   * 
   * @param listener The listener.
   * @see #removeDataChangedListener(ISyxDataChangeListener)
   */
  public void addDataChangedListener(ISyxDataChangeListener listener)
  {
    if (this.listeners==null)
      this.listeners = new HashSet<ISyxDataChangeListener>();
    this.listeners.add(listener);
  }

  /**
   * Removes a data changed listener from this struct.
   * 
   * @param listener The listener.
   * @see #addDataChangedListener(ISyxDataChangeListener)
   */
  public void removeDataChangedListener(ISyxDataChangeListener listener)
  {
    this.listeners.remove(listener);
  }

  /**
   * Notifies all listeners that the struct's system exclusive data have 
   * changed.
   * 
   * @see #addDataChangedListener(ISyxDataChangeListener)
   * @see #removeDataChangedListener(ISyxDataChangeListener)
   */
  protected void notifyDataChanged()
  {
    if (this.listeners!=null)
      for (ISyxDataChangeListener listener : this.listeners)
        listener.syxDataChanged(this);
  }
  
  // -- Object Overrides ------------------------------------------------------
  
  @Override
  public boolean equals(Object obj)
  {
    SyxDataStruct other;
    
    // Test whether obj is a SyxDataStruct
    try
    {
      other = (SyxDataStruct)obj;
    }
    catch (Exception e) 
    {
      return false;
    }
 
    // Compare names
    if (!String.valueOf(this.name).equals(String.valueOf(other.name)))
      return false;

    // Compare data
    if (this.data!=null && other.data!=null)
      if (this.data.length != other.data.length)
        return false;
      else
        for (int i=0; i<this.data.length; i++)
          if (this.data[i]!=other.data[i])
            return false;
    else if ((this.data!=null) != (other.data!=null))
      return false;

    // Compare format
    if (this.format!=null && other.format!=null)
      if (this.format.length != other.format.length)
        return false;
      else
        for (int i=0; i<this.format.length; i++)
          if (this.format[i]!=other.format[i])
            return false;
    else if ((this.format!=null) != (other.format!=null))
      return false;

    // Compare filterMask
    if (this.filterMask!=null && other.filterMask!=null)
      if (this.filterMask.length != other.filterMask.length)
        return false;
      else
        for (int i=0; i<this.filterMask.length; i++)
          if (this.filterMask[i]!=other.filterMask[i])
            return false;
    else if ((this.filterMask!=null) != (other.filterMask!=null))
      return false;

    // Equal
    return true;
  }

  // -- Pretty-Printing -------------------------------------------------------
  
  /**
   * Pretty-prints this struct to a string.
   * 
   * @param linePrefix
   *          A prefix string for printed lines (not applied to the first line)
   * @return The printed string
   */
  public String prettyPrint(String linePrefix)
  {
    String lx = linePrefix;     // Actual line prefix
    String si = " ".repeat(10); // Line indent
    String s  = "";             // Output string
    int    l  = getLength();    // Length of struct in bytes

    // Name and Length
    s += this.name!=null ? "'"+this.name+"'" : "(unnamed)";
    s += lx+String.format("\n- Length: %d byte%s",l,l!=1?"s":"");

    // Data buffer
    s += lx+"\n- Data:   ";
    for (int i=0; i<this.data.length; i++)
    {
      s += String.format("%s ",SYX.b2sH(this.data[i]));
      if ((i+1)%16==0)
        s += lx+String.format("\n%s",si);
    }
    s += lx+String.format("\n%s",si);
    for (int i=0; i<this.data.length; i++)
    {
      s += String.format("%s ",SYX.b2sB(this.data[i]));
      if ((i+1)%8==0)
        s += lx+String.format("\n%s",si);
    }

    // Format
    s += lx+"\n- Format: ";
    for (int i=0; i<this.format.length; i++)
    {
      s += this.format[i];
      if ((i+1)%64==0)
        s += String.format("\n%s",si);
      else if ((i+1)%8==0)
        s += " ";
    }

    // Filter Mask
    s += lx+"\n- Filter: ";
    for (int i=0; i<this.filterMask.length; i++)
    {
      s += String.format("%s ",SYX.b2sB(this.filterMask[i]));
      if ((i+1)%8==0)
        s += String.format("\n%s",si);
    }

    // Parameters
    s += lx+"\n- Params: ";
    char[] pars = getParamNames();
    if (pars.length>0)
      for (int j=0; j<pars.length; j++)
      {
        char pn = pars[j];
        int  pb = getParamBitCount(pn);
        SyxParamInfo pi = null;
        try
        {
          pi = getParamInfo(pn);
        }
        catch (Exception e) { /* Ignore */ }
        String sj = pi!=null ? "  " : "";
        s += String.format("%c - length%s: %d bit%s\n",pn,sj,pb,pb!=1?"s":"");
        s += lx+si+String.format("    value%s : ",sj);
        int v = getMidiValue(pn);
        s += String.format(pb<8 ? "0x%02X" : "0x%04X",v);
        s += String.format("  %d",v);
        if ((pb==7||pb==8) && v>=0x20 && v<=0x7E)
          s += String.format("  '%c'",(char)v);
        if (pi!=null)
          try
          {
            if (pi.getType()==PTYPE.ENUM_INT)
              s += String.format(" = %d",pi.midi2Model(v));
            else if (pi.getType()==PTYPE.ENUM_STR)
              s += String.format(" = '%s'",pi.midi2ModelAsString(v));
          }
          catch (Exception e)
          {
            s += String.format(" = ??? (%s)",e.getClass().getSimpleName());
          }

        if (pi!=null)
        {
          s += "\n"+lx+si;
          s += "    "+pi.prettyPrint(lx+si+"    ");
        }
        else
          s += "\n"+lx;
        if (j<pars.length-1)
          s += lx+si; 
      }
    else
      s += "(none)\n";

    return s;
  }

  /**
   * Pretty-prints this struct to a string; equivalent to {@link 
   * #prettyPrint(String) prettyPrint}{@code ("")}.
   * 
   * @return The printed string
   */
  public String prettyPrint()
  {
    return prettyPrint("");
  }

  /**
   * Pretty-prints this struct as list of data bytes to a string.
   * 
   * @param linePrefix
   *          A prefix string for printed lines (not applied to the first line)
   * @param globalPartIndex
   *          The zero-based printed part index
   * @param globalDataPos
   *          The zero-based printed index of the first data byte
   * @return The printed string
   */
  public String prettyPrintData
  (
    String linePrefix,
    int    globalPartIndex,
    int    globalDataPos
  )
  {
    String s = "";

    int len = getLength();
    String name = this.name!=null ? "'"+this.name+"' " : "";
    s += String.format(
      "Part%4d : %04d %s(%d byte%s) \n",
      globalPartIndex,globalDataPos,name,len,len!=1?"s":""
    );

    // Part bytes
    String paramsPrinted = ""; // Parameters already printed
    for (int i=0; i<len; i++)
    {
      String bfspec = getByteFormat(i);
      String fmask  = SYX.b2sB(this.filterMask[i]);
      s += String.format("%04d %02XH - ",globalDataPos+i,this.data[i]);
      s += String.format("%s %s",bfspec,fmask);

      String byteParams = String.valueOf(getByteParams(i));
      if (byteParams.length()>0)
      {
        for (int k=0; k<byteParams.length(); )
        {
          String p = String.valueOf(byteParams.charAt(k));
          if (paramsPrinted.contains(p))
            byteParams = byteParams.replaceAll(p,"");
          else
            k++;
        }
        if (byteParams.length()>0)
        {
          s += " : ";
          for (int k=0; k<byteParams.length(); k++)
          {
            if (k>0)
              s += String.format("\n%s"," ".repeat(31)); 

            char p = byteParams.charAt(k);
            int  v = getMidiValue(p);
            int  l = getParamBitCount(p);
            
            s += String.format("%c (%2d %s) = ",p,l,l==1?"bit ":"bits");
            s += String.format(l<8 ? "  0x%02X " : "0x%04X ",v);
            s += String.format("%5d ",v);
            if ((l==7||l==8) && v>=0x20 && v<=0x7E)
              s += String.format(" '%c'",(char)v);
            else
              s += "    ";
            try
            {
              SyxParamInfo pi = getParamInfo(p);
              
              if (pi!=null)
                s += " "+pi.getDescr()+" = "+pi.midi2ModelAsString(v);
              else
                s += " (no parameter info)";
            }
            catch (InvalidMidiDataException e)
            { // Do nothing
              s += "(error: "+e.getMessage()+")";
              e.printStackTrace();
            }
          }
        }
        paramsPrinted += byteParams;
      }
      s += "\n";
    }

    return s;
  }

  /**
   * Pretty-prints this struct as list of data bytes to a string; equivalent to 
   * {@link #prettyPrintData(String,int,int) prettyPrint}{@code ("",
   * globalPartIndex, globalDataPos)}.
   * 
   * @param globalPartIndex
   *          The zero-based printed part index
   * @param globalDataPos
   *          The zero-based printed index of the first data byte
   * @return The printed string
   */
  public String prettyPrintData(int globalPartIndex, int globalDataPos)
  {
    return prettyPrintData("",globalPartIndex,globalDataPos);
  }

  // -- Workers ---------------------------------------------------------------
  
  /**
   * Parses a format specifier and initializes the internal data structure.
   * 
   * @param format
   *          The {@linkplain SyxDataStruct format specifier}
   * @throws IllegalArgumentException
   *          if the pattern is invalid
   */
  private void int_parsePattern(String format)
  throws IllegalArgumentException
  {
    if (format==null || format.length()==0)
      throw SYX.IllArgExc(SYX.ERR(E_FSPEC_NONE));
    
    this.format     = null; // Clear pattern attribute
    String npattern  = "";   // New pattern attribute as string
    String[] bfspecs = format.split("\\s+"); // Get byte format specifiers
    for (int i=0; i<bfspecs.length; i++)
    {
      String  b = bfspecs[i]; // Byte format specifier
      String  s; // Byte format specifier w/o number of repetitions
      int     n; // Number of repetitions
      
      // Get number of byte format repetitions (*[n])
      Pattern p = Pattern.compile("^([a-z#A-FH0-9\\*]+)(?:\\[(\\d+)\\])?$");
      Matcher m = p.matcher(b);
      if (m.matches())
      {
        s = m.group(1);
        if (m.group(2)!=null)
          try
          {
            n = Integer.parseInt(m.group(2));
          }
          catch (NumberFormatException e)
          {
            throw SYX.IllArgExc(SYX.ERR(E_BFSPEC_INVAL,b,i),e);
          }
        else
          n = 1;
      }
      else
        throw SYX.IllArgExc(SYX.ERR(E_BFSPEC_INVAL,b,i));
      
      // Convert byte specifier from hex to binary if necessary
      p = Pattern.compile("^.{2}H$");
      if (p.matcher(s).matches())
      {
        String t = "";
        for (int j=0; j<2; j++)
        {
          char c = s.charAt(j);
          if (isValidParamName(c) || c=='*')
            if (j==0)
              t += "0"+String.valueOf(c).repeat(3);
            else
              t += String.valueOf(c).repeat(4);
          else
          {
            int v = Integer.parseUnsignedInt(""+c,16);
            if (j==0 && v>0x07)
              throw SYX.IllArgExc(SYX.ERR(E_BFSPEC_INVAL,b,i));
            t += String.format("%4s",Integer.toBinaryString(v)).replace(" ","0");
          }
        }
        s = t;
      }
      
      // Check binary byte specifier
      p = Pattern.compile("^0[01a-z#\\*]{7}$");
      if (!p.matcher(s).matches())
        throw SYX.IllArgExc(SYX.ERR(E_BFSPEC_INVAL,b,i));
      
      // Write pattern attribute
      for (int j=0; j<n; j++)
        npattern += s;
    }
    
    // Initialize internal data structure
    this.format = npattern.toCharArray();
    int l = this.format.length;
    this.data = new byte[l/8];
    this.filterMask = new byte[l/8];
    for (int i=0; i<l; i++)
    {
      char c = this.format[i];
      int by = i/8;
      int bi = 7-i%8;
      if (c=='0'||c=='1')
      {
        this.data[by]       = SYX.bSet(this.data[by],bi,c=='1');
        this.filterMask[by] = SYX.bSet(this.filterMask[by],bi);
      }
    }

  }

  /**
   * Parses a specifier of multiple parameter names.
   * 
   * @param names
   *          The parameter names specifier; either a range, e.g. "a-j", or a 
   *          list, e.g. "abcdefghij"
   * @return A character array of parameter names
   * @throws IllegalArgumentException
   *           if {@code names} is {@code null}, empty, or not a valid specifier
   * @throws InvalidMidiDataException
   *          if any of the parameter names specified by {@code names} is not a
   *          parameter in the {@linkplain SyxDataStruct format}
   */
  public final char[] int_parseParamNames(String names)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (names==null || names.length()==0)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULLEMPTY,"names"));

    Pattern p = Pattern.compile("^([a-z])-([a-z])$");
    Matcher m = p.matcher(names);
    if (m.matches())
    {
      names = "";
      char c0 = m.group(1).charAt(0);
      char c1 = m.group(2).charAt(0);
      for (char c=c0; c<=c1; c++)
        names += c;
    }

    p = Pattern.compile("^[a-z]+$");
    m = p.matcher(names);
    if (!m.matches())
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_INVAL,names,"names"));
    
    for (int i=0; i<names.length(); i++)
    {
      char c = names.charAt(i);
      if (getParamBitCount(c)==0)
        throw new InvalidMidiDataException(SYX.ERR(E_PARAM_UNDEF,c));
    }

    return names.toCharArray();
  }
  
  /**
   * Returns all parameter names of a byte in a format specification.
   * 
   * @param format
   *          The format specification
   * @return A character array containing the parameter names, may be empty
   */
  private char[] int_getParams(char[] format)
  {
    String s = "";
    for (char c : format)
      if (isValidParamName(c))
        if (!s.contains(""+c))
          s += c;
    return s.toCharArray();
  }

  /**
   * Checks whether a data buffer matches this struct. If the data buffer does
   * <em>not</em> match the format, the method throws an exception. If the data
   * buffer matches the format, the method completes normally.
   * 
   * @param data
   *          The data buffer
   * @param start
   *          Zero-based index in {@code data} from which to start matching data
   *          bytes
   * @param len
   *          The number of bytes to match, non-positive to match entire buffer
   * @throws IllegalArgumentException
   *          if {@code data} is {@code null}, or if {@code start} is less than
   *          zero
   * @throws InvalidMidiDataException
   *          if {@code data} does not match this struct, or if it contains too
   *          few bytes
   */
  private void int_match(byte[] data, int start, int len)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (data==null)
      throw SYX.IllArgExc("Argument 'data' must not be null");
    if (start<0)
      throw SYX.IllArgExc("Argument 'start'=%d out of range",start);
    if (len<=0)
      len = this.data.length;
    len = Math.min(len,this.data.length);
    if (len>data.length-start)
      throw new InvalidMidiDataException(E_DATA_TOOSHORT);
    for (int i=0; i<len; i++)
      if ((this.filterMask[i]&data[i+start])!=(this.filterMask[i]&this.data[i]))
        throw new InvalidMidiDataException(SYX.ERR(E_DATA_NOMATCH,i,data[i+start]));
  }

  /**
   * Validates a MIDI value for a parameter.
   * 
   * @param pname
   *          The parameter name: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param midiValue
   *          The MIDI value to validate
   * @param onInit
   *          indicates to the {@link SyxParamInfo} {@linkplain
   *          SyxParamInfo#validateMidiValue(int,boolean) validator} whether the
   *          parameter is being reset or initialized ({@code onInit==true})
   *          or being set or checked ({@code onInit==false}); ignored if the
   *          parameter does not have  parameter information, i.e., if {@link
   *          #paramInfos}{@code .}{@link HashMap#get(Object) get}{@code
   *          (pname)} returns {@code null}
   * @throws IllegalArgumentException
   * @throws InvalidMidiDataException
   */
  private void int_validateMidiValue(char pname, int midiValue, boolean onInit)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Generic validation
    int n = getParamBitCount(pname);
    if (n==0)
      throw SYX.InvMdataExc(E_PARAM_UNDEF,pname);
    if (midiValue<0)
      throw SYX.IllArgExc(SYX.ERR(E_VALUE_NEG,pname));
    if (midiValue>(1<<n)-1)
      throw SYX.InvMdataExc(E_VALUE_TOOLARGE,midiValue,pname);

    // Validation by parameter info
    SyxParamInfo pi = this.paramInfos.get(pname);
    if (pi!=null)
      // Validate by parameter info
      pi.validateMidiValue(midiValue,onInit);
  }

  /**
   * Copies all bits form an integer value to the internal parameter data buffer
   * or vice versa.
   * 
   * @param pname
   *          The parameter name: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param midiValue
   *          If {@code v2d==true}, the MIDI value to be copied into the
   *          {@linkplain #data internal data array}; ignored otherwise
   * @param v2d
   *          Copy direction.
   *          If {@code true}, copy {@code midiValue} to the {@linkplain #data
   *          internal data array}; if {@code false}, copy internal data to
   *          return value. 
   * @param onInit
   *          If {@code v2d==true}, flag to indicates to validators whether the
   *          parameter is being reset or initialized ({@code onInit==true})
   *          or being set or checked ({@code onInit==false}); ignored otherwise
   * @return If {@code v2d==false}, the value, -1 otherwise
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * @throws InvalidMidiDataException
   *          if there is no parameter {@code pname} in the {@linkplain 
   *          SyxDataStruct format}, or if {@link midiValue} is not permissible
   */
  protected int int_copyParamBits
  (
    char    pname,
    int     midiValue,
    boolean v2d,
    boolean onInit
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (!isValidParamName(pname))
      throw SYX.IllArgExc(SYX.ERR(E_PARAM_ILLNAME,pname));
    if (getParamBitCount(pname)==0)
      throw new InvalidMidiDataException(SYX.ERR(E_PARAM_UNDEF,pname));

    // When copying MIDI value to internal data array: Validate MIDI value
    if (v2d)
      int_validateMidiValue(pname,midiValue,onInit);

    // Copy value bits to internal data buffer
    int retMidiValue = 0;
    int biv = 0; //biv: Bit index value
    for (int bip=format.length-1; bip>=0; bip--) //bip: bit index in pattern
      if (format[bip]==pname)
      {
        int byd = bip/8; //byd: byte index in data
        int bid = 7-bip%8; //bid: bit index in data
        if (v2d)
          this.data[byd] = SYX.bSet(this.data[byd],bid,SYX.bGet(midiValue,biv));
        else
          retMidiValue = SYX.bSet(retMidiValue,biv,SYX.bGet(data[byd],bid));
        biv++;
      }

    if (v2d)
      return -1;
    else
      return retMidiValue;
  }

}

//EOF