package de.btu.kt.syx.midi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.SYX;


/**
 * A structured {@linkplain SysexMessage MIDI system exclusive message}.
 * 
 * @see SyxDataStruct
 * @author Matthias Wolff
 */
public class SyxMessage extends SysexMessage implements Serializable 
{
  private static final long serialVersionUID = 1L;

  // -- Constants -------------------------------------------------------------

  protected static transient final String E_CHECKSUM
    = "Checksum error";

  protected transient final String E_DATA_TOOLONG
    = "Data buffer longer than message data format";

  protected static transient final String E_PART_NOTFOUND
    = "Part '%s' not found";

  protected static transient final String E_UID_NOTFOUND
    = "Parameter UID '%s' not found";

  protected static transient final String E_DUPLUCATE_UID
    = "Duplicate parameter UID '%s'";

  // -- Attributes ------------------------------------------------------------

  /**
   * The status byte of this message.
   */
  protected int status = SYSTEM_EXCLUSIVE;

  /**
   * {@linkplain SyxDataStruct System exclusive data structs} of this message
   */
  protected ArrayList<SyxDataStruct> parts;

  /**
   * List of unique parameter names of this message mapping to their {@linkplain
   * SyxParamInfo parameter information}.
   */
  protected LinkedHashMap<String,SyxParamInfo> paramMap;

  // -- Constructors ----------------------------------------------------------
  
  /**
   * Creates a new structured {@linkplain SysexMessage MIDI system exclusive 
   * message}.
   */
  public SyxMessage()
  {
    super();
    clear();
  }
  
  /**
   * Creates a new structured {@linkplain SysexMessage MIDI system exclusive 
   * message}.
   * 
   * @param parts
   *          {@linkplain SyxDataStruct System exclusive data struct(s)} to add
   *          to the message
   * @throws InvalidMidiDataException
   *           if {@code parts} contains a {@link SyxChecksum} struct and 
   *           checksum validation of the other part data failed 
   */
  public SyxMessage(SyxDataStruct... parts)
  throws InvalidMidiDataException
  {
    this();
    addParts(parts);
    for (SyxDataStruct part : this.parts)
      if (part instanceof SyxChecksum)
      {
        SyxChecksum cs = (SyxChecksum)part;
        if (!cs.validateCheckSum(this))
          throw new InvalidMidiDataException(E_CHECKSUM);
      }
  }
  
  /**
   * Creates a new structured {@linkplain SysexMessage MIDI system exclusive 
   * message}.
   * 
   * @param format
   *         The {@linkplain SyxDataStruct message format specifier}. The 
   *         specifier does <em>not</em> include the leading status byte (0xF0 
   *         or 0xF7) and the tailing EOX flag (0xF7).
   * @param data
   *         The system exclusive message data including the status byte
   * @param length
   *          The length of the valid message data in the array, including the
   *          status byte
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, or if {@code data} 
   *          is {@code null}
   * @throws InvalidMidiDataException
   *          if {@code data} does not match the data format, or if it contains 
   *          too few or too many bytes
   */
  public SyxMessage(String format, byte[] data, int length) 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this();
    if (data==null || data.length<1)
      throw SYX.IllArgExc("Argument 'data' must not be null or empty");
    this.status = SYX.b2i(data[0]);
    addParts(new SyxDataStruct(format));
    this.setMessage(data,length);
  }
  
  /**
   * Creates a new structured {@linkplain SysexMessage MIDI system exclusive 
   * message}.
   * 
   * @param format
   *         The {@linkplain SyxDataStruct message format specifier}. The 
   *         specifier does <em>not</em> include the leading status byte (0xF0 
   *         or 0xF7) and the tailing EOX flag (0xF7).
   * @param data
   *         The system exclusive message data including the status byte
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, or if {@code data} 
   *          is {@code null}
   * @throws InvalidMidiDataException
   *          if the status byte is invalid for a system exclusive message, or
   *          if {@code data} does not match the data format, or if it contains
   *          too few or too many bytes
   */
  public SyxMessage(String format, byte[] data) 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(format,data,data.length);
    if (getLength()<data.length)
      throw new InvalidMidiDataException(E_DATA_TOOLONG);
  }

  /**
   * Creates a new structured {@linkplain SysexMessage MIDI system exclusive 
   * message}.
   * 
   * @param format
   *         The {@linkplain SyxDataStruct message format specifier}. The 
   *         specifier does <em>not</em> include the leading status byte (0xF0 
   *         or 0xF7) and the tailing EOX flag (0xF7).
   * @param sxMsg
   *          A {@link SysexMessage} to copy the message data from 
   * @throws IllegalArgumentException
   *          if {@code format} is {@code null} or invalid, or if {@code sxMsg} 
   *          is {@code null}
   * @throws InvalidMidiDataException
   *          if {@code sxMsg.}{@link SysexMessage#getData() getData()} does not
   *          match the data format, or if it contains too few bytes
   */
  public SyxMessage(String format, SysexMessage sxMsg)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this();
    if (sxMsg==null)
      throw SYX.IllArgExc("Argument 'sxMsg' must not be null");
    addParts(new SyxDataStruct(format,sxMsg.getData()));
  }

  // -- Initialization --------------------------------------------------------

  /**
   * Clears this message (remove {@linkplain #parts parts} and {@linkplain
   * #paramMap parameter map}).
   */
  public void clear()
  {
    this.parts    = new ArrayList<SyxDataStruct>();
    this.paramMap = new LinkedHashMap<String,SyxParamInfo>();
  }

  /**
   * Resets all parameters to their default values.
   */
  public void reset()
  {
    for (SyxDataStruct prt : getParts())
      prt.reset();
  }

  /**
   * Makes a shallow copy of another message.
   * 
   * @param other
   *          The other message
   * @throws IllegalArgumentException
   *          if the argument is {@code null}
   */
  public void copy(SyxMessage other)
  throws IllegalArgumentException
  {
    if (other==null)
      throw new IllegalArgumentException();
    this.parts = other.parts;
    this.paramMap = other.paramMap;
  }

  // -- API: Parts ------------------------------------------------------------

  /**
   * Determines whether this message is empty. A message is empty iff there are
   * no {@linkplain #parts parts}.
   */
  public boolean isEmpty()
  {
    return this.parts.size()==0;
  }

  /**
   * Adds {@linkplain SyxDataStruct system exclusive data structs} to this 
   * message.
   * 
   * <p><b>Note:</b>
   * If you use {@linkplain SyxParamInfo parameter information}, invoke this
   * method <em>after</em> {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * adding} those to the parts! Not doing so results in the parts' parameters
   * missing in the {@linkplain #paramMap parameter UID map}. A correct 
   * implementation looks like this:</p>
   * <pre style="margin-left:2.8em"> {@link SyxDataStruct} myPart = new {@link SyxDataStruct}(...);
   * myPart.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(...);
   * ... 
   * myPart.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(...);
   * {@link #addParts(SyxDataStruct...) addParts}(myPart);</pre>
   * 
   * @param parts
   *          {@linkplain SyxDataStruct System exclusive data struct(s)} to add
   *          to the message
   * @throws InvalidMidiDataException
   *          if {@code parts} contains invalid MIDI data
   */
  public void addParts(SyxDataStruct... parts)
  throws InvalidMidiDataException
  {
    if (parts!=null)
      for (SyxDataStruct part : parts)
      {
        this.parts.add(part);
        mapParamUIDs(part);
      }
  }

  /**
   * Returns all parts of this message.
   * 
   * @return An array containing the parts, may be empty. Changes made to 
   *         elements of the array will modify this message's data content.
   */
  public SyxDataStruct[] getParts()
  {
    return this.parts.toArray(new SyxDataStruct[0]); 
  }

  /**
   * Gets a message part.
   * 
   * @param index
   *          The zero-based index
   * @return The part, changes made will be effective in this message
   * @throws IllegalArgumentException
   *          if {@code index} is out of range
   */
  public SyxDataStruct getPart(int index)
  throws IllegalArgumentException
  {
    if (index<0 || index>=this.parts.size())
      throw SYX.IllArgExc("Argument 'index'=%d out of range",index);
    return this.parts.get(index);
  }

  /**
   * Gets a message part.
   * 
   * @param name
   *         The part name. If there are multiple parts with the same name, the
   *         method will return the first of them.
   * @return The part, changes made will be effective in this message
   * @throws IllegalArgumentException
   *          if {@code name} is {@code null}, or if no part with this name
   *          exists
   */
  public SyxDataStruct getPart(String name)
  throws IllegalArgumentException
  {
    if (name==null)
      throw SYX.IllArgExc("Argument 'name' must not be null");
    for (int i=0; i<this.parts.size(); i++)
      if (name.equals(this.parts.get(i).getName()))
        return this.parts.get(i);
    throw SYX.IllArgExc(SYX.ERR(E_PART_NOTFOUND,name));
  }

  /**
   * Validates the message's checksum if present. Should be invoked when 
   * initializing this system exclusive message from received data.
   *  
   * @throws InvalidMidiDataException
   *           if the checksum contained in the message does not equal the 
   *           checksum computed from the message's data.
   */
  public void validateChecksum()
  throws InvalidMidiDataException
  {
    for (SyxDataStruct part : this.parts)
      if (part instanceof SyxChecksum)
      {
        SyxChecksum cs = (SyxChecksum)part;
        byte csMsg     = cs.getCheckSum();         // Message checksum
        byte csCmp     = cs.computeCheckSum(this); // Computed checksum
        if (csMsg!=csCmp)
          throw new InvalidMidiDataException(E_CHECKSUM);
      }
  }

  /**
   * Updates the message's checksum if present. Should be invoked before sending
   * this system exclusive message to a hardware instrument.
   */
  public void updateChecksum()
  {
    for (SyxDataStruct part : this.parts)
      if (part instanceof SyxChecksum)
      {
        SyxChecksum cs = (SyxChecksum)part;
        cs.setCheckSum(cs.computeCheckSum(this));
      }
  }

  // -- API: Part Parameters --------------------------------------------------

  // -  Parameter UIDs  - - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Returns an array containing all unique parameter names (UIDs) in this
   * message. The returned array may be empty.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   */
  public String[] getParamUIDs()
  {
    return this.paramMap.keySet().toArray(new String[0]);
  }

  /**
   * Finds a parameter.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param UID
   *          The unique parameter name
   * @return {@code UID} if a parameter with this unique name exist, {@code
   *         null} otherwise
   */
  public String findParamUID(String UID)
  {
    if (this.paramMap.containsKey(UID))
      return UID;
    else
      return null;
  }

  /**
   * Finds parameters and returns an array of the respective unique parameter
   * names.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param regex
   *          A regular expression specifying the unique parameter name(s) to
   *          search for
   * @return A string array; may be empty if no matching parameters were found
   * @throws IllegalArgumentException if the argument is {@code null}, or empty,
   *         or not a valid regular expression
   * @see #findParamUIDsSimple(String)
   */
  public String[] findParamUIDs(String regex)
  throws IllegalArgumentException
  {
    if (regex==null || regex.length()==0)
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"regex");

    ArrayList<String> UIDs = new ArrayList<String>();
    try
    {
      Pattern pattern = Pattern.compile(regex);
      for (String UID : getParamUIDs())
      {
        Matcher matcher = pattern.matcher(UID);
        if (matcher.matches())
          UIDs.add(UID);
      }
    }
    catch (PatternSyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }
    return UIDs.toArray(new String[0]);
  }

  /**
   * Finds parameters and returns an array of the respective unique parameter
   * names.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param pattern
   *          A simplified pattern specifying the unique parameter name(s) to
   *          search for, one of the following:
   *          <ul style="margin-bottom:0">
   *            <li>"{@code *}" or "{@code **}" to list <em>all</em> parameters,
   *              or</li>
   *            <li>a unique parameter name with wildcards
   *              <ul style="margin-bottom:0">
   *                <li>"{@code *}" matching any sequence of characters except
   *                  '{@code .}'</li>
   *                <li>"{@code **}" matching any sequence of characters
   *                  including '{@code .}'</li>
   *              </ul></li>
   *          </ul>
   * @return A string array; may be empty if no matching parameters were found
   * @throws IllegalArgumentException if the argument is {@code null} or empty
   * 
   * @see #findParamUIDs(String)
   */
  public String[] findParamUIDsSimple(String pattern)
  {
    if (pattern==null || pattern.length()==0)
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"pattern");

    if ("*".equals(pattern) || "**".equals(pattern))
      return getParamUIDs();

    String regex = pattern
      .replace("**","[a-zA-Z_0-9.]$")
      .replace("*","\\w+")
      .replace(".","\\.")
      .replace("$","*");
    return findParamUIDs(regex);
  }

  /**
   * Finds a parameter and returns the respective {@linkplain SyxParamInfo
   * parameter information}.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param UID
   *          The unique parameter name
   * @return The parameter information or {@code null} if no such parameter
   *         exists
   */
  public SyxParamInfo findParam(String UID)
  {
    if (findParamUID(UID)!=null)
      return this.paramMap.get(UID);
    else
      return null;
  }

  /**
   * Returns {@linkplain SyxParamInfo parameter information} for a unique
   * parameter name (UID).
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param UID
   *          The unique parameter name
   * @return The parameter information
   * @throws IllegalArgumentException
   *          If there is no parameter named {@code UID} in this message
   */
  public SyxParamInfo getParamInfo(String UID)
  throws IllegalArgumentException
  {
    SyxParamInfo pi = findParam(UID);
    if (pi==null)
      throw SYX.IllArgExc(SYX.ERR(E_UID_NOTFOUND,UID)); 
    return pi;
  }

  /**
   * Finds parameters and returns an array of the respective {@linkplain
   * SyxParamInfo parameter information} objects.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param regex
   *          A regular expression specifying the unique parameter name(s) to
   *          search for
   * @return An array of {@link SyxParamInfo}s; may be empty if no matching
   *         parameters were found
   * @throws IllegalArgumentException if the argument is {@code null}, or empty,
   *         or not a valid regular expression
   * @see #findParamsSimple(String)
   */
  public SyxParamInfo[] findParams(String regex)
  {
    ArrayList<SyxParamInfo> pis = new ArrayList<SyxParamInfo>();
    for (String UID : findParamUIDs(regex))
      pis.add(getParamInfo(UID));
    return pis.toArray(new SyxParamInfo[0]);
  }

  /**
   * Finds parameters and returns an array of the respective {@linkplain
   * SyxParamInfo parameter information} objects.
   * 
   * <p><b>Note:</b>
   * In order to use unique parameter names, {@linkplain SyxParamInfo parameter
   * information} must be {@linkplain SyxDataStruct#addParamInfo(SyxParamInfo)
   * added} to all {@linkplain #getParts() parts}.</p>
   * 
   * @param pattern
   *          A simplified pattern specifying the unique parameter name(s) to
   *          search for, one of the following:
   *          <ul style="margin-bottom:0">
   *            <li>"{@code *}" or "{@code **}" to list <em>all</em> parameters,
   *              or</li>
   *            <li>a unique parameter name with wildcards
   *              <ul style="margin-bottom:0">
   *                <li>"{@code *}" matching any sequence of characters except
   *                  '{@code .}'</li>
   *                <li>"{@code **}" matching any sequence of characters
   *                  including '{@code .}'</li>
   *              </ul></li>
   *          </ul>
   * @return An array of {@link SyxParamInfo}s; may be empty if no matching
   *         parameters were found
   * @throws IllegalArgumentException if the argument is {@code null} or empty
   * 
   * @see #findParams(String)
   */
  public SyxParamInfo[] findParamsSimple(String pattern)
  {
    ArrayList<SyxParamInfo> pis = new ArrayList<SyxParamInfo>();
    for (String UID : findParamUIDsSimple(pattern))
      pis.add(getParamInfo(UID));
    return pis.toArray(new SyxParamInfo[0]);
  }

  // -  Parameter Values  - - - - - - - - - - - - - - - - - - - - - - - - - - -

  /**
   * Returns the MIDI value of a parameter.
   * 
   * @param UID
   *          The unique parameter name
   * @throws IllegalArgumentException
   *           if there is no parameter named {@code UID} in this message
   * @throws InvalidMidiDataException
   *           if validation of the retrieved MIDI value failed
   */
  public int getMidiValue(String UID)
  throws IllegalArgumentException
  { 
    SyxParamInfo pi = getParamInfo(UID);
    return pi.getParent().getMidiValue(pi.getPname());
  }

  /**
   * Sets the MIDI value of a parameter.
   * 
   * @param UID
   *          The unique parameter name
   * @param midiValue
   *          The MIDI value
   * @throws IllegalArgumentException
   *          if there is no parameter named {@code UID} in this message
   * @throws InvalidMidiDataException
   *          if {@code midiValue} is not permissible
   */
  public void setMidiValue(String UID, int midiValue)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(UID);
    pi.getParent().setMidiValue(pi.getPname(),midiValue);
  }

  /**
   * Returns the model value of a parameter.
   * 
   * @param UID
   *          The unique parameter name
   * @throws IllegalArgumentException
   *           if there is no parameter named {@code UID} in this message
   */
  public int getModelValue(String UID)
  throws IllegalArgumentException
  {
    SyxParamInfo pi = getParamInfo(UID);
    try
    {
      return pi.getParent().getModelValue(pi.getPname());
    }
    catch (InvalidMidiDataException e)
    { // Cannot happen
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Returns a string representation of the model value of a parameter.
   * 
   * @param UID
   *          The unique parameter name
   * @throws IllegalArgumentException
   *           if there is no parameter named {@code UID} in this message
   */
  public String getModelValueAsString(String UID)
  throws IllegalArgumentException
  {
    SyxParamInfo pi = getParamInfo(UID);
    try
    {
      return pi.getParent().getModelValueAsString(pi.getPname());
    }
    catch (InvalidMidiDataException e)
    { // Cannot happen
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Reads several parameters as a string.
   * 
   * <p>The method first invokes {@link #findParamsSimple(String)
   * findParamsSimple}{@code (UIDpfx+"**")} to determine the list of parameters
   * to read and then {@linkplain #getMidiValue(String) gets these
   * parameters} one-by-one and writes their character values into the returned
   * string</p> 
   * 
   * @param UIDpfx
   *          The common prefix of the unique names of the parameters to write
   * @return string
   *          The read string
   * @throws IllegalArgumentException
   *          if {@code UIDpfx} is {@code null} or empty, or if no parameters
   *          with the UID prefix {@code UIDpfx} were found
   * @throws InvalidMidiDataException
   *          if conversion of parameter values to characters failed
   */
  public String readString(String UIDpfx)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (UIDpfx==null || UIDpfx.length()==0)
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"UIDpfx");

    String[] UIDs = findParamUIDsSimple(UIDpfx+"**");
    if (UIDs.length==0)
      throw SYX.IllArgExc("No parameter UID beginning with '"+UIDpfx+"' found");

    try
    {
      String s = "";
      for (int i=0; i<UIDs.length; i++)
        s += (char)(getMidiValue(UIDs[i]));
      return s;
    }
    catch (Throwable e)
    {
      throw SYX.InvMdataExc
        (
          "Reading characters from '%s' failed. Cause: %s",
          UIDpfx,e.getCause()
        );
    }
  }

  /**
   * Sets the model value of a parameter.
   * 
   * @param UID
   *          The unique parameter name
   * @param modelValue
   *          The model value
   * @throws IllegalArgumentException
   *          if there is no parameter named {@code UID} in this message
   * @throws InvalidMidiDataException
   *          if {@code modelValue} is not permissible
   */
  public void setModelValue(String UID, int modelValue)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(UID);
    pi.getParent().setModelValue(pi.getPname(),modelValue);
  }

  /**
   * Sets the model value of a parameter from a value name.
   * 
   * @param UID
   *          The unique parameter name
   * @param valueName
   *          A string representation of the model value
   * @throws IllegalArgumentException
   *          if there is no parameter named {@code UID} in this message
   * @throws InvalidMidiDataException
   *          if {@code modelValue} is not permissible
   */
  public void setModelValue(String UID, String valueName)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    SyxParamInfo pi = getParamInfo(UID);
    pi.getParent().setModelValue(pi.getPname(),valueName);
  }

  /**
   * Writes a string into several parameters.
   * 
   * <p>The method first invokes {@link #findParamsSimple(String)
   * findParamsSimple}{@code (UIDpfx+"**")} to determine the list of parameters
   * to write and then {@linkplain #setMidiValue(String, int) sets these
   * parameters} one-by-one to the characters of {@code string}.</p> 
   * 
   * @param UIDpfx
   *          The common prefix of the unique names of the parameters to write
   * @param string
   *          The string to write
   * @param lenient
   *          if {@code true}, excess characters in {@code string} will be
   *          ignored and missing characters will be replaced by spaces; if
   *          {@code false}, the number of characters in {@code string} must
   *          equal the size of the UID list returned by {@link
   *          #findParamsSimple(String) findParamsSimple}{@code (UIDpfx+"**")}
   * @throws IllegalArgumentException if
   *          <ul style="margin-bottom:0">
   *            <li>{@code UIDpfx} is {@code null} or empty, or</li>
   *            <li>no parameters with the UID prefix {@code UIDpfx} were found,
   *              or</li>
   *            <li>
   *              if {@code lenient} is {@code false} and {@code string} is
   *              {@code null} or contains a wrong number of characters.</li>
   *          </ul>
   * @throws InvalidMidiDataException
   *          if parameter setting failed
   */
  public void writeString(String UIDpfx, String string, boolean lenient)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (UIDpfx==null || UIDpfx.length()==0)
      throw SYX.IllArgExc(SYX.E_ARG_NULLEMPTY,"UIDpfx");

    String[] UIDs = findParamUIDsSimple(UIDpfx+"**");
    if (UIDs.length==0)
      throw SYX.IllArgExc("No parameter UID beginning with '"+UIDpfx+"' found");

    if (!lenient && (string==null || string.length()!=UIDs.length))
      throw SYX.IllArgExc(SYX.E_ARG_BADLEN,"string",UIDs.length);

    if (string==null)
      string = "";
    if (string.length()>UIDs.length)
      string = string.substring(0,UIDs.length);
    if (string.length()<UIDs.length)
      string = String.format("%-"+UIDs.length+"s",string);
    
    for (int i=0; i<UIDs.length; i++)
      setMidiValue(UIDs[i],string.charAt(i));
  }

  /**
   * Validates the current values of all parameters.
   * 
   * @throws InvalidMidiDataException
   *          if validation fails
   */
  public void validate()
  throws InvalidMidiDataException
  {
    String messages = "";
    
    for (SyxDataStruct prt : getParts())
      try
      {
        prt.validateAll();
      }
      catch (InvalidMidiDataException e)
      {
        if (messages.length()>0)
          messages += "\n";
        messages += e.getMessage();
      }

    if (messages.length()>0)
      throw SYX.InvMdataExc(messages);
  }

  // -- Overrides of SysexMessage ---------------------------------------------

  /**
   * Sets the data of this system exclusive message.
   *
   * @param data 
   *          The system exclusive message data, <em>including</em> the status
   *          byte (0xF0 or 0xF7). The end-of-exclusive byte (EOX, 0xF7) at the
   *          end of {@code data} is optional. If missing, EOX will be appended
   *          automatically.
   * @param length
   *          The length of the valid message data in the array, including the
   *          status byte
   * @throws IllegalArgumentException
   *          if {@code data} is {@code null}
   * @throws InvalidMidiDataException 
   *          if the first byte is not a valid status byte (0xF0 or 0xF7), or if
   *          {@code data} does not match the data format (see {@link 
   *          #getParts()})
   */
  @Override
   public void setMessage(byte[] data, int length)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (data==null)
      throw SYX.IllArgExc("Argument 'data' must not be null");
    if (length < 1 || (length > 0 && length > data.length))
      throw new IndexOutOfBoundsException("length out of bounds: "+length);
    int status = SYX.b2i(data[0]);
    if (status!=SYSTEM_EXCLUSIVE && status!=SPECIAL_SYSTEM_EXCLUSIVE)
      throw new InvalidMidiDataException("Status byte missing in data");

    // Set message data
    this.status = status;
    int i=1; // Zero-based position in data: skip status byte in data
    for (SyxDataStruct part : parts)
      i += part.setData(data,i);
  }

  /**
   * Sets the data of this system exclusive message.
   *
   * @param status 
   *          The SysEx status byte for the message (0xF0 or 0xF7)
   * @param data
   *          The system exclusive message data, <em>excluding</em> the status
   *          byte. The end-of-exclusive byte (EOX, 0xF7) at the end of {@code
   *          data} is optional. If missing, EOX will be appended automatically.
   * @param length 
   *          The length of the valid message data in the array
   * @throws InvalidMidiDataException
   *          if the {@code status} is not a valid status byte (0xF0 or 0xF7), 
   *          or if {@code data} does not match the data format
   */
  @Override
  public void setMessage(int status, byte[] data, int length)
  throws InvalidMidiDataException
  {
    if (status!=SYSTEM_EXCLUSIVE && status!=SPECIAL_SYSTEM_EXCLUSIVE)
      throw new InvalidMidiDataException();
    if (length < 0 || length > data.length)
      throw new IndexOutOfBoundsException("length out of bounds: "+length);
    byte[] mdata = new byte[length+1]; // +1 for status byte!
    mdata[0] = SYX.i2b(status);
    if (length > 1)
      System.arraycopy(data,0,mdata,1,length);
    this.setMessage(mdata,length);
  }

  /**
   * Returns a copy of this SysEx message data content. The returned array of 
   * bytes does neither include the status byte (0xF0 or 0xF7), nor the 
   * end-of-exclusive byte (EOX, 0xF7).
   */
  @Override
  public byte[] getData()
  {
    int len = getLength()-2; // Without status and EOX bytes!
    byte[] data = new byte[len];
    int i = 0;
    for (SyxDataStruct part : parts)
    {
      int l = part.getLength();
      System.arraycopy(part.getData(),0,data,i,l);
      i += l;
    }
    return data;
  }
  
  @Override
  public Object clone()
  {
    SyxMessage other = new SyxMessage();
    for (SyxDataStruct part : this.parts)
      try
      {
        other.addParts(part);
      } 
      catch (InvalidMidiDataException e)
      { // Cannot happen
        e.printStackTrace();
      }
    return other;
  }

  // -- Overrides of MidiMessage ----------------------------------------------

  /**
   * Returns the total length of the system exclusive message in bytes, 
   * including the status byte (0xF0 or 0xF7) and the end-of-exclusive byte 
   * (EOX, 0xF7).
   */
  @Override
  public int getLength()
  {
    int len = 2; // Status byte and EOX byte
    for (SyxDataStruct part : parts)
      len += part.getLength();
    return len;
  }

  /**
   * Returns a copy of the complete system exclusive message. The first byte of
   * the returned byte array is the status byte of the message (0xF0 or 0xF7).
   * The subsequent bytes are message data bytes as returned by {@link
   * #getData()}. The last byte is the end-of-exclusive byte (EOX, 0xF7).
   * 
   * @return The complete message data.
   */
  @Override
  public byte[] getMessage()
  {
    int len = getLength();
    byte[] data = new byte[len];
    data[0] = SYX.i2b(this.status);
    System.arraycopy(getData(),0,data,1,getData().length);
    data[len-1] = SYX.i2b(ShortMessage.END_OF_EXCLUSIVE);
    return data;
  }

  /**
   * Returns the status byte (0xF0 or 0xF7) of the system exclusive message.
   */
  @Override
  public int getStatus()
  {
    return this.status;
  }

  // -- Overrides of Object ---------------------------------------------------

  @Override
  public boolean equals(Object obj)
  {
    SyxMessage other;
    
    // Test whether obj is a SyxDataStruct
    try
    {
      other = (SyxMessage)obj;
    }
    catch (Exception e) 
    {
      return false;
    }

    // Compare status and EOX
    if (this.status!=other.status)
      return false;

    // Compare parts
    if (this.parts.size()!=other.parts.size())
      return false;
    for (int i=0; i<this.parts.size()-1; i++)
      if (!this.parts.get(i).equals(other.parts.get(i)))
          return false;

    // Equal
    return true;
  }

  // -- Pretty-Printing -------------------------------------------------------
  
  /**
   * Pretty-prints this message into a string.
   * 
   * @param timeStamp
   *          The message's time stamp, negative if unknown 
   * @return The printed string
   */
  public String prettyPrint(long timeStamp)
  {
    String s = "";

    // Status byte
    int status = getStatus();
    s += String.format("0000 %02XH (%s)\n",status,SYX.getStatusName(status));
    
    // Parts
    int pos = 1; // Part offset (1: parts start after status byte)
    for (int i=0; i<this.parts.size(); i++)
    {
      SyxDataStruct ds = this.parts.get(i);
      s   += ds.prettyPrintData(i,pos);
      pos += ds.getLength();
    }

    // EOX
    int eox = ShortMessage.END_OF_EXCLUSIVE;
    s += "EOX\n";
    s += String.format("%04d %02XH (%s)\n",pos,eox,SYX.getStatusName(eox));

    // Message length
    s += String.format("%d %s",getLength(),getLength()==1 ? "byte" : "bytes");

    // Time stamp
    if (timeStamp>=0)
    {
      s += String.format("\n");
      s += String.format("Time stamp       : %d",timeStamp);
    }

    // Checksum
    SyxDataStruct lastPart = getPart(getParts().length-1);
    if (lastPart instanceof SyxChecksum)
    {
      SyxChecksum cs = (SyxChecksum)lastPart;
      byte csm = cs.getCheckSum(); // Checksum stores in message
      byte csc = cs.computeCheckSum(this); // Computed checksum
      s += String.format("\n");
      s += String.format("Message checksum : 0x%02X\n",csm);
      s += String.format("Computed checksum: 0x%02X"  ,csc);
    }
    
    return s;
  }

  /**
   * Pretty-prints this message into a string.
   * 
   * @return The printed string
   */
  public String prettyPrint()
  {
    return prettyPrint(-1);
  }

  // -- Workers ---------------------------------------------------------------

  /**
   * Maps all parameters in a {@link SyxDataStruct} which have unique
   * identifiers (UIDs).
   * 
   * @param part
   *          The data struct
   * @throws InvalidMidiDataException
   *          if a duplicate parameter UID is found
   */
  protected void mapParamUIDs(SyxDataStruct part)
  throws InvalidMidiDataException
  {
    if (part==null)
      return;
    for (char pname : part.getParamNames())
    {
      SyxParamInfo pi = null;
      try
      {
        pi = part.getParamInfo(pname);
      }
      catch (InvalidMidiDataException e)
      { // Cannot happen
        e.printStackTrace();
      }
      if (pi==null)
        continue;
      String UID = pi.getUID();
      if (this.paramMap.containsKey(UID))
        throw new InvalidMidiDataException(SYX.ERR(E_DUPLUCATE_UID,UID));
      this.paramMap.put(UID,pi);
    }
  }

}

// EOX