package de.btu.kt.syx.midi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SequencedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.midi.InvalidMidiDataException;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.SyxDataStruct.PTYPE;

/**
 * Instances of this class provide additional information for parameters of
 * {@link SyxDataStruct}s.
 * 
 * <p>Additional parameter information is optional. It facilitates</p>
 * <ul>
 *   <li>validation of parameter values,</li>
 *   <li>conversion between 
 *     <ul style="margin-bottom:0pt;">
 *       <li>the <em>MIDI value</em> of the parameter as stored in the system
 *       exclusive message and</li>
 *       <li>the <em>model value</em> of the parameter as displayed on the
 *       hardware instrument,</li>
 *     </ul>
 *   <li>and data binding.</li>
 * </ul>
 *
 * <p>Additional parameter information comprises:</p>
 * <ul>
 *   <li>General information
 *     <ul>
 *       <li>{@linkplain SyxDataStruct.PTYPE parameter type}: there are
 *         <ul style="margin-bottom:0pt;">
 *           <li>{@linkplain PTYPE#RAW raw} parameters: without additional
 *             parameter information (the permissible MIDI value range is
 *             automatically derived from the {@link SyxDataStruct}'s 
 *             {@linkplain SyxDataStruct format specifier}),</li>
 *           <li>{@linkplain PTYPE#RANGE range} parameters: permissible MIDI
 *           values are in the range [{@link #minMidiValue}...{@link
 *           #maxMidiValue}],</li>
 *           <li>{@linkplain PTYPE#ENUM_INT enumerated integers} parameters:
 *           permissible MIDI values are listed in {@link #valueMap}, and</li>
 *           <li>{@linkplain PTYPE#ENUM_STR enumerated names} parameters:
 *           permissible MIDI values are listed in {@link #valueNamesMap}.</li>
 *         </ul></li>
 *       <li>{@link #parent}: The {@link SyxDataStruct} containing the parameter
 *       </li>
 *       <li>{@link #pname}: The parameter name in the {@linkplain SyxDataStruct
 *         format specifier} of the {@link SyxDataStruct} containing the
 *         parameter</li>
 *       <li>{@link #UID}: The unique parameter name in the {@link SyxMessage}
 *       </li>
 *       <li>{@link #descr}: The parameter name (as seen on
 *       hardware instrument)</li>
 *     </ul></li>
 *   <li>MIDI value information
 *     <ul>
 *       <li>{@link #defMidiValue}: The default MIDI value (used for parameter
 *       initialization)</li>
 *       <li>{@link #minMidiValue}: The smallest permissible MIDI 
 *       value<sup>1)</sup></li>
 *       <li>{@link #maxMidiValue}: The greatest permissible MIDI 
 *       value<sup>1)</sup></li>
 *     </ul></li>
 *   <li>MIDI&harr;model value conversion information
 *     <ul>
 *       <li>{@link #modelValueOffset}: Offset of the model value wrt. to the
 *       MIDI value ({@linkplain PTYPE#RANGE range} parameters only)</li>
 *       <li>{@link #valueMap}: A list of permissible model values mapped to
 *       their respective MIDI values ({@linkplain PTYPE#ENUM_INT enumerated
 *       integers} parameters only)</li>
 *       <li>{@link #valueNamesMap}: A list of permissible model value names 
 *       mapped to their respective MIDI values ({@linkplain PTYPE#ENUM_STR
 *       enumerated names} parameters only)</li>
 *     </ul></li>
 *   <li>Parameter change message information
 *     <ul>
 *       <li>{@link #paramChangeMsgFormat}: {@linkplain SyxDataStruct Format
 *       specifier} of the system exclusive parameter change message (optional)
 *       </li>  
 *     </ul></li>
 * </ul>
 *
 * <p><sup>1)</sup> In case of {@linkplain PTYPE#ENUM_INT enumerated integers}
 * and {@linkplain PTYPE#ENUM_STR enumerated names} parameters, there may be
 * MIDI values in the range [{@link #minMidiValue}...{@link #maxMidiValue}]
 * which are <em>not</em> permissible.</p> 
 * 
 * @see {@linkplain #SyxParamInfo(SyxDataStruct, char, String, int, int, int,
 *      String) Constructor} for {@linkplain PTYPE#RANGE range} parameters
 * @see {@linkplain #SyxParamInfo(SyxDataStruct, char, String, int[], int[],
 *      String) Constructor} for {@linkplain PTYPE#ENUM_INT enumerated integers}
 *      parameters
 * @see {@linkplain #SyxParamInfo(SyxDataStruct, char, String, int[], String[],
 *      String) Constructor} for {@linkplain PTYPE#ENUM_STR enumerated names}
 *      parameters
 * @see {@linkplain #SyxParamInfo(SyxDataStruct, char, String, SequencedMap,
 *      String) Constructor} for {@linkplain PTYPE#ENUM_INT enumerated integers}
 *      or {@linkplain PTYPE#ENUM_STR enumerated names} parameters
 */
public class SyxParamInfo implements Serializable
{

  private static final long serialVersionUID = 1L;

  // -- Attributes ------------------------------------------------------------

  /**
   * The {@link SyxDataStruct} containing the parameter.
   */
  private SyxDataStruct parent;

  /**
   * The parameter name in the {@link SyxDataStruct} containing the parameter.
   */
  private char pname;

  /**
   * The unique parameter name in the {@link SyxMessage}.
   */
  private String UID;

  /**
   * The parameter name (as seen on hardware instrument)
   */
  private String descr;

  /**
   * The default MIDI value
   */
  private int defMidiValue;

  /**
   * The smallest permissible MIDI value
   */
  private int minMidiValue;

  /**
   * The largest permissible MIDI value
   */
  private int maxMidiValue;

  /**
   * The model value offset.
   * 
   * <p style="margin-left:2em">
   *   {@code modelValue = midiValue + modelValueOffset}<br>
   *   {@code midiValue = modelValue - modelValueOffset}
   * </p>
   */
  private int modelValueOffset;

  /**
   * An array of two integers; is {@code null} if the parameter does not have
   * <em>consecutive</em> integer model values.
   * <ul>
   *   <li>{@code modelValueRange[0]}: the smallest permissible model value,
   *     </li>
   *   <li>{@code modelValueRange[1]}: the largest permissible model value.</li>
   */
  private int[] modelValueRange;

  /**
   * Map of model values to MIDI values. The key set contains all permissible 
   * model values and the value set contains the respective permissible MIDI
   * values.
   *
   * <h3>Remark:</h3>
   * <p>If this attribute is set, {@link #valueNamesMap} must be {@code null},
   * and the parameter type is {@link PTYPE#ENUM_INT}.</p>
   */
  private SequencedMap<Integer,Integer> valueMap;

  /**
   * Map of model value names to MIDI values. The key set contains all 
   * permissible model value names and the value set contains the respective
   * permissible MIDI values.
   * 
   * <h3>Remark:</h3>
   * <p>If this attribute is set, {@link #valueMap} must be {@code null}, and
   * the parameter type is {@link PTYPE#ENUM_STR}.</p>
   */
  private SequencedMap<String,Integer> valueNamesMap;

  /**
   * {@linkplain SyxDataStruct Format specifier} of the system exclusive 
   * parameter change message, may be {@code null}.
   */
  private String paramChangeMsgFormat;

  // -- Constructors ----------------------------------------------------------

  /**
   * Creates a new generic {@linkplain PTYPE#RANGE range} parameter info.
   * 
   * <p><b>Notes:</b><ul>
   *   <li>Other constructors must invoke this constructor!</li>
   *   <li>The {@linkplain #minMidiValue smallest permissible MIDI value} will
   *     be set to {@code 0}.</li>
   *   <li>The {@linkplain #maxMidiValue largest permissible MIDI value} will be
   *     set to the {@linkplain #int_getMaxStorageMidiValue() greatest possible
   *     value} that can be stored for this parameter in {@code parent}.</li>
   *   <li>The {@linkplain #defMidiValue default MIDI value} will be set to
   *     {@code 0}.</li>
   *   <li>The {@linkplain #modelValueOffset model value offset} will be set to
   *     {@code 0}.</li>
   * </ul></p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException if
   *          <ul>
   *            <li>{@code parent} is {@code null}</li>
   *            <li>{@code pname} is {@linkplain
   *              SyxDataStruct#isValidParamName(char) invalid} or no such
   *              parameter exists in {@code parent}</li>
   *            <li>{@code UID} is {@code null}, empty, or {@linkplain 
   *              #checkUID(String) invalid}</li>
   *          </ul>
   * 
   * @see #setValueRange(int, int, int)
   * @see #setValueMap(SequencedMap)
   * @see #setValueMap(int[], int[])
   * @see #setValueMap(int[], String[])
   */
  public SyxParamInfo
  (
    SyxDataStruct parent,
    char          pname,
    String        UID,
    String        descr
  )
  throws IllegalArgumentException
  {
    // Sanity checks
    if (parent==null)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULL,"parent"));
    if (!SyxDataStruct.isValidParamName(pname))
      throw SYX.IllArgExc(SYX.ERR(SyxDataStruct.E_PARAM_ILLNAME,pname));
    SyxParamInfo.checkUID(UID);
    if (parent.getParamBitCount(pname)==0)
      SYX.IllArgExc(SyxDataStruct.E_PARAM_UNDEF,pname);

    // (Zero-)Initialize attributes
    this.parent               = parent;
    this.pname                = pname;
    this.UID                  = UID;
    this.descr                = descr;
    this.defMidiValue         = 0;
    this.paramChangeMsgFormat = null;

    // Initialize default value range
    try
    {
      setValueRange(0,int_getMaxStorageMidiValue(),0);
      // Final check already done by setValueRange
    }
    catch (IllegalArgumentException | InvalidMidiDataException e)
    { // Cannot happen
      throw SYX.InternalError(e);
    }
  }

  /**
   * Creates a new parameter info for a {@linkplain SyxDataStruct.PTYPE range}
   * parameter.
   * 
   * <p>This constructor invokes {@link #SyxParamInfo(SyxDataStruct,char,String,
   * String) SyxParamInfo}{@code (parent,pname,UID,descr)}, and then {@link
   * #setValueRange(int,int,int) setValueRange}{@code
   * (minMidiValue,maxMidiValue,modelValueOffset)}.</p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param minMidiValue
   *          The smallest permissible MIDI value, non-negative
   * @param maxMidiValue
   *          The largest permissible MIDI value, non-negative
   * @param modelValueOffset
   *          The {@linkplain #modelValueOffset model value offset}
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException
   *          for conditions see {@link #SyxParamInfo(SyxDataStruct,char,String,
   *          String)} and {@link #setValueRange(int,int,int)}
   * @throws InvalidMidiDataException
   *          ditto 
   */
  public SyxParamInfo
  (
    SyxDataStruct parent,
    char          pname,
    String        UID,
    int           minMidiValue,
    int           maxMidiValue,
    int           modelValueOffset,
    String        descr
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(parent,pname,UID,descr);
    setValueRange(minMidiValue,maxMidiValue,modelValueOffset);
    // Final check already done by setValueRange
  }

  /**
   * Creates a new parameter info for a {@linkplain SyxDataStruct.PTYPE range}
   * parameter. Convenience shortcut for {@link #SyxParamInfo(SyxDataStruct,
   * char, String, int, int, int, String) SyxParamInfo}{@code
   * (parent,pname,UID,minMidiValue,maxMidiValue,}<tt><span style="color:red"
   * >0</span></tt>{@code ,descr)}.</p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param minMidiValue
   *          The smallest permissible MIDI value, non-negative
   * @param maxMidiValue
   *          The largest permissible MIDI value, non-negative
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code parent} is {@code null}</li>
   *            <li>{@code pname} is {@linkplain 
   *              SyxDataStruct#isValidParamName(char) invalid} or no such
   *              parameter exists in {@code parent}</li>
   *            <li>{@code UID} is {@code null}, empty, or {@linkplain 
   *              #checkUID(String) invalid}</li>
   *            <li>{@code minMidiValue}>{@code maxMidiValue}</li>
   *          </ul>
   * @throws InvalidMidiDataException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code minMidiValue} or {@code maxMidiValue} is
   *              negative</li>
   *            <li>{@code minMidiValue} or {@code maxMidiValue} is greater than
   *              the {@linkplain #int_getMaxStorageMidiValue() greatest
   *              possible value} that can be stored for this parameter in the
   *              {@link #getParent() SyxDataStruct} that contains it</li>
   *          </ul>
   */
  public SyxParamInfo
  (
    SyxDataStruct parent,
    char          pname,
    String        UID,
    int           minMidiValue,
    int           maxMidiValue,
    String        descr
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(parent,pname,UID,minMidiValue,maxMidiValue,0,descr);
  }

  /**
   * Creates a new parameter info for an {@linkplain SyxDataStruct.PTYPE
   * enumerated values} or an {@linkplain SyxDataStruct.PTYPE enumerated value
   * names} parameter.
   * 
   * <p>This constructor invokes {@link #SyxParamInfo(SyxDataStruct,char,String,
   * String) SyxParamInfo}{@code (parent,pname,UID,descr)}, and then {@link
   * #setValueMap(SequencedMap) setValueMap}{@code (valueMap)}.</p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param valueMap
   *          Map of model values to MIDI values, either
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code <Integer,Integer>}: The key set contains all
   *              permissible model values and the value set contains the
   *              respective permissible MIDI values, or </li>
   *            <li>{@code <String,Integer>}: The key set contains all 
   *              permissible model value names (as seen on hardware instrument)
   *              and the value set contains the respective permissible MIDI
   *              values.</li>
   *          </ul>
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException
   *          for conditions see {@link #SyxParamInfo(SyxDataStruct,char,String,
   *          String)} and {@link #setValueMap(SequencedMap)}
   * @throws InvalidMidiDataException
   *          ditto 
    */
  public SyxParamInfo
  (
    SyxDataStruct                parent,
    char                         pname,
    String                       UID,
    SequencedMap<Object,Integer> valueMap,
    String                       descr
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(parent,pname,UID,descr);
    setValueMap(valueMap);
    // Final check already done by setValueMap
  }

  /**
   * Creates a new parameter info for an {@linkplain SyxDataStruct.PTYPE
   * enumerated values} parameter.
   * 
   * <p>This constructor invokes {@link #SyxParamInfo(SyxDataStruct,char,String,
   * String) SyxParamInfo}{@code (parent,pname,UID,descr)}, and then {@link
   * #setValueMap(int[], int[]) setValueMap}{@code (midiValues,modelValues)}.
   * </p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param midiValues
   *          An array of permissible MIDI values
   * @param modelValues
   *          An array containing the respective model parameters for all
   *          permissible {@code midiValues}. If {@code null}, the constructor
   *          will use {@code midiValues} creating an identity map.
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException
   *          for conditions see {@link #SyxParamInfo(SyxDataStruct,char,String,
   *          String)} and {@link #setValueMap(int[], int[])}
   * @throws InvalidMidiDataException
   *          ditto 
   */
  public SyxParamInfo
  (
    SyxDataStruct parent,
    char          pname,
    String        UID,
    int[]         midiValues,
    int[]         modelValues,
    String        descr
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(parent,pname,UID,descr);
    setValueMap(midiValues,modelValues);
    // Final check already done by setValueMap
 }

  /**
   * Creates a new parameter info for an {@linkplain SyxDataStruct.PTYPE
   * enumerated value names} parameter.
   * 
   * <p>This constructor invokes {@link #SyxParamInfo(SyxDataStruct,char,String,
   * String) SyxParamInfo}{@code (parent,pname,UID,descr)}, and then {@link
   * #setValueMap(int[], String[]) setValueMap}{@code (midiValues,valueNames)}.
   * </p>
   * 
   * @param parent
   *          The {@link SyxDataStruct} containing the parameter
   * @param pname
   *          The parameter name in the {@link SyxDataStruct} containing the
   *          parameter: {@code 'a'}, ..., {@code 'z'} or {@code '#'}
   * @param UID
   *          The unique parameter name in the {@link SyxMessage}
   * @param midiValues
   *          An array of permissible MIDI values
   * @param valueNames
   *          An array the respective permissible value names (as seen on
   *          hardware instrument)
   * @param descr
   *          The parameter name (as seen on hardware instrument)
   * @throws IllegalArgumentException
   *          for conditions see {@link #SyxParamInfo(SyxDataStruct,char,String,
   *          String)} and {@link #setValueMap(int[], String[])}
   * @throws InvalidMidiDataException
   *          ditto 
   */
  public SyxParamInfo
  (
    SyxDataStruct parent,
    char          pname,
    String        UID,
    int[]         midiValues, 
    String[]      valueNames, 
    String        descr
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    this(parent,pname,UID,descr);
    setValueMap(midiValues,valueNames);
    // Final check already done by setValueMap
  }

  // -- API: Initialization Setters -------------------------------------------

  /**
   * Sets the MIDI value range and the model value offset making this parameter
   * a {@linkplain PTYPE#RANGE range} parameter.
   * 
   * <p><b>Notes:</b> 
   *   <ul>
   *     <li>If a {@linkplain #valueMap MIDI value map} or a {@linkplain
   *       #valueNamesMap value names map} is present, the method will clear
   *       them.</li>
   *     <li>If the present {@linkplain #defMidiValue default MIDI value} is
   *       outside the range {@code [minMidiValue,maxMidiValue]}, the method
   *       will set it to {@code minMidiValue}.</li>
   *   </ul>
   * </p> 
   * 
   * @param minMidiValue
   *          The smallest permissible MIDI value, non-negative
   * @param maxMidiValue
   *          The largest permissible MIDI value, non-negative
   * @param modelValueOffset
   *          The {@linkplain #modelValueOffset model value offset}
   * @throws IllegalArgumentException 
   *          if {@code minMidiValue}>{@code maxMidiValue}
   * @throws InvalidMidiDataException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code minMidiValue} or {@code maxMidiValue} is
   *              negative</li>
   *            <li>{@code minMidiValue} or {@code maxMidiValue} is greater than
   *              the {@linkplain #int_getMaxStorageMidiValue() greatest
   *              possible value} that can be stored for this parameter in the
   *              {@link #getParent() SyxDataStruct} that contains it</li>
   *          </ul>
   * 
   * @see #setValueMap(SequencedMap)
   * @see #setValueMap(int[], int[])
   * @see #setValueMap(int[], String[])
   */
  public final void setValueRange
  (
    int minMidiValue,
    int maxMidiValue,
    int modelValueOffset
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (minMidiValue<0)
      throw SYX.InvMdataExc(SYX.ERR(SYX.E_ARG_NEG,"minMidiValue"));
    if (maxMidiValue<0)
      throw SYX.InvMdataExc(SYX.ERR(SYX.E_ARG_NEG,"maxMidiValue"));
    if (maxMidiValue<minMidiValue)
      throw SYX.IllArgExc(SyxDataStruct.E_VALUE_MINMAX);
    int maxxMidiValue = int_getMaxStorageMidiValue();
    if (minMidiValue>maxxMidiValue)
      throw SYX.InvMdataExc(SyxDataStruct.E_VALUE_TOOLARGE,minMidiValue,pname);
    if (maxMidiValue>maxxMidiValue)
      throw SYX.InvMdataExc(SyxDataStruct.E_VALUE_TOOLARGE,minMidiValue,pname);

    // Initialize fields
    this.minMidiValue     = minMidiValue;
    this.maxMidiValue     = maxMidiValue;
    this.modelValueOffset = modelValueOffset;
    this.modelValueRange  = new int[]
                            {midi2Model(minMidiValue),midi2Model(maxMidiValue)};
    this.valueMap         = null;
    this.valueNamesMap    = null;

    // - Correct default MIDI value if necessary
    if (!SYX.valueIn(this.defMidiValue,minMidiValue,maxMidiValue))
      this.defMidiValue = minMidiValue;

    // Final check
    check();
  }

  /**
   * Sets the MIDI value map making this parameter either a {@linkplain 
   * PTYPE#ENUM_INT enumerated values} or a {@linkplain 
   * PTYPE#ENUM_STR enumerated value names} parameter, depending on the type
   * of argument {@code valueMap}.
   * 
   * <p><b>Notes:</b> 
   *   <ul>
   *     <li>The method will automatically detect and set the {@linkplain
   *       #minMidiValue smallest} and {@linkplain #maxMidiValue greatest}
   *       permissible MIDI values according to {@code valueMap.}{@link
   *       SequencedMap#values() values()}.</li>
   *     <li>The method will set the {@linkplain #modelValueOffset model value
   *       offset} to {@code 0}.</li>
   *     <li>If the present {@linkplain #defMidiValue default MIDI value} is
   *       not contained in {@code valueMap.}{@link SequencedMap#values()
   *       values()}, the method will set it to the first MIDI value in the map.
   *       </li>
   *   </ul>
   * </p> 
   * 
   * @param valueMap
   *          Map of model values to MIDI values, either
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code <Integer,Integer>}: The key set contains all
   *              permissible model values and the value set contains the
   *              respective permissible MIDI values, or </li>
   *            <li>{@code <String,Integer>}: The key set contains all 
   *              permissible model value names (as seen on hardware instrument)
   *              and the value set contains the respective permissible MIDI
   *              values.</li>
   *          </ul>
   * @throws IllegalArgumentException
   *           if {@code valueMap} is {@code null}, or contains keys of bad or
   *           inconsistent type, or contains less than two entries
   * @throws InvalidMidiDataException
   *          if {@code valueMap} contains negative MIDI values or MIDI values
   *          greater than the {@linkplain #int_getMaxStorageMidiValue()
   *          greatest possible value} that can be stored for this parameter in
   *          the {@link #getParent() SyxDataStruct} that contains it
   * 
   * @see #setValueRange(int, int, int)
   * @see #setValueMap(int[], int[])
   * @see #setValueMap(int[], String[])
   */
  public final void setValueMap(SequencedMap<Object,Integer> valueMap)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (valueMap==null)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULL),"valueMap");
    if (valueMap.size()<2)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_TOOSHORT),"valueMap");

    // - Convert valueMap to either <Integer,Integer> or <String,Integer)
    int nIntKeys      = 0;
    int nStrKeys      = 0;
    int minMidiValue  = Integer.MAX_VALUE;
    int maxMidiValue  = Integer.MIN_VALUE;
    int minModelValue = Integer.MAX_VALUE;
    int maxModelValue = Integer.MIN_VALUE;
    int maxxMidiValue = int_getMaxStorageMidiValue();
    ArrayList<Integer>      modelValues = new ArrayList<Integer>();
    SequencedMap<Integer,Integer> viMap = new LinkedHashMap<Integer,Integer>();
    SequencedMap<String ,Integer> vsMap = new LinkedHashMap<String ,Integer>();
    int i = 0;
    for (Entry<Object,Integer> entry : valueMap.entrySet())
    {
      // - MIDI values
      int midiValue  = entry.getValue();
      String s = "valueMap.values["+i+"]";
      if (midiValue<0)
        throw SYX.InvMdataExc(SYX.ERR(SyxDataStruct.E_VALUE_NEG,s));
      if (midiValue>maxxMidiValue)
        throw SYX.InvMdataExc(SYX.ERR(SyxDataStruct.E_VALUE_TOOLARGE,midiValue,s));
      minMidiValue = Math.min(minMidiValue,midiValue);
      maxMidiValue = Math.max(maxMidiValue,midiValue);

      // - Model values
      if (entry.getKey() instanceof Integer)
      {
        int modelValue = (Integer)entry.getKey();
        minModelValue = Math.min(minModelValue,modelValue);
        maxModelValue = Math.max(maxModelValue,modelValue);
        modelValues.add(modelValue);
        viMap.put(modelValue,midiValue);
        nIntKeys++;
      }
      else if (entry.getKey() instanceof String)
      {
        vsMap.put((String)entry.getKey(),midiValue);
        nStrKeys++;
      }
      i++;
    }
    if (nIntKeys!=valueMap.size())
      viMap = null;
    if (nStrKeys!=valueMap.size())
      vsMap = null;
    if (viMap==null && vsMap==null)
      throw SYX.IllArgExc
      (
        "All keys in 'valueMap' must have the same type (Integer or String)"
      );

    // Determine whether model vales are a consecutive integer range
    boolean consecutiveModelValues = false;
    if (valueMap.firstEntry().getKey() instanceof Integer)
      if (maxModelValue-minModelValue+1 == modelValues.size())
      {
        consecutiveModelValues=true;
        for (int v=minModelValue; v<=maxModelValue; v++)
          if (!modelValues.contains(v))
          {
            consecutiveModelValues=false;
            break;
          }
      }

    // Initialize fields
    this.minMidiValue     = minMidiValue;
    this.maxMidiValue     = maxMidiValue;
    this.modelValueOffset = 0;
    this.modelValueRange  = null;
    this.valueMap         = viMap;
    this.valueNamesMap    = vsMap;

    // - Correct default MIDI value if necessary
    if (!valueMap.values().contains(this.defMidiValue))
      this.defMidiValue = valueMap.values().toArray(new Integer[0])[0];

    // - Set consecutive model value range if possible
    if (consecutiveModelValues)
      this.modelValueRange = new int[] {minModelValue,maxModelValue};

    // Final check
    check();
  }

  /**
   * Sets the {@linkplain #valueMap model values to MIDI values map} making this
   * parameter a {@linkplain PTYPE#ENUM_INT enumerated values} parameter.
   * 
   * <p><b>Notes:</b> 
   *   <ul>
   *     <li>The method will automatically detect and set the {@linkplain
   *       #minMidiValue smallest} and {@linkplain #maxMidiValue greatest}
   *       permissible MIDI values according to {@code midiValues}.</li>
   *     <li>The method will set the {@linkplain #modelValueOffset model value
   *       offset} to {@code 0}.</li>
   *     <li>If the present {@linkplain #defMidiValue default MIDI value} is
   *       not contained in {@code midiValues}, the method will set it to
   *       {@code midiValues[0]}.</li>
   *   </ul>
   * </p> 
   * 
   * @param midiValues
   *          An array of permissible MIDI values
   * @param modelValues
   *          An array containing the respective model parameters for all
   *          permissible {@code midiValues}. If {@code null}, the method
   *          will use {@code midiValues} creating an identity map.
   * @throws IllegalArgumentException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code midiValues} is {@code null} or contains less than two
   *              values, or</li>
   *            <li>{@code modelValues.length!=midiValues.length}, or</li>
   *            <li>{@code modelValues} contains duplicate values</li>
   *          </ul>
   * @throws InvalidMidiDataException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code midiValues} contains negative values, or</li>
   *            <li>{@code midiValues} contains values greater than the
   *              {@linkplain #int_getMaxStorageMidiValue() greatest possible
   *              value} that can be stored for this parameter in the {@link
   *              #getParent() SyxDataStruct} that contains it</li>
   *          </ul>
   * 
   * @see #setValueRange(int, int, int)
   * @see #setValueMap(SequencedMap)
   * @see #setValueMap(int[], String[])
   */
  public final void setValueMap
  (
      int[] midiValues,
      int[] modelValues
  )
  throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (midiValues==null)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULL,"midiValues"));
    if (modelValues==null)
      modelValues = midiValues;
    if (modelValues.length!=midiValues.length)
      throw SYX.IllArgExc
      (
        SYX.ERR(SYX.E_ARGS_DIFFLEN,"midiValues","modelValues")
      );

    // Make sequenced map modelValues -> midiValues
    LinkedHashMap<Object,Integer> map = new LinkedHashMap<Object,Integer>();
    for (int i=0; i<midiValues.length; i++)
      if (map.containsKey(modelValues[i]))
        throw SYX.IllArgExc(SYX.E_ARG_DUPVAL,"modelValues");
      else
        map.put(modelValues[i],midiValues[i]);

    // Set sequenced map
    setValueMap(map);
    // Final check already done by setValueMap(SequencedMap)
  }

  /**
   * Sets the {@linkplain #valueNamesMap model value names to MIDI values map}
   * making this parameter a {@linkplain PTYPE#ENUM_STR enumerated value names}
   * parameter.
   * 
   * <p><b>Notes:</b> 
   *   <ul>
   *     <li>The method will automatically detect and set the {@linkplain
   *       #minMidiValue smallest} and {@linkplain #maxMidiValue greatest}
   *       permissible MIDI values according to {@code midiValues}.</li>
   *     <li>The method will set the {@linkplain #modelValueOffset model value
   *       offset} to {@code 0}.</li>
   *     <li>If the present {@linkplain #defMidiValue default MIDI value} is
   *       not contained in {@code midiValues}, the method will set it to
   *       {@code midiValues[0]}.</li>
   *   </ul>
   * </p> 
   * 
   * @param midiValues
   *          An array of permissible MIDI values
   * @param valueNames
   *          An array the respective permissible value names (as seen on
   *          hardware instrument)
   * @throws IllegalArgumentException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code midiValues} is {@code null} or contains less than two
   *              values, or</li>
   *            <li>{@code valueNames} is {@code null} or contains less than two
   *              values, or</li>
   *            <li>{@code valueNames.length!=midiValues.length}, or</li>
   *            <li>{@code valueNames} contains duplicate values</li>
   *          </ul>
   * @throws InvalidMidiDataException if
   *          <ul style="margin-bottom:0pt">
   *            <li>{@code midiValues} contains negative values, or</li>
   *            <li>{@code midiValues} contains values greater than the
   *              {@linkplain #int_getMaxStorageMidiValue() greatest possible
   *              value} that can be stored for this parameter in the {@link
   *              #getParent() SyxDataStruct} that contains it</li>
   *          </ul>
   * 
   * @see #setValueRange(int, int, int)
   * @see #setValueMap(SequencedMap)
   * @see #setValueMap(int[], int[])
   */
  public final void setValueMap
  (
      int[]    midiValues,
      String[] valueNames
  )
   throws IllegalArgumentException, InvalidMidiDataException
  {
    // Sanity checks
    if (midiValues==null)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULL,"midiValues"));
    if (valueNames==null)
      throw SYX.IllArgExc(SYX.ERR(SYX.E_ARG_NULL,"valueNames"));
    if (valueNames.length!=midiValues.length)
      throw SYX.IllArgExc
      (
        SYX.ERR(SYX.E_ARGS_DIFFLEN,"midiValues","valueNames")
      );

    // Make sequenced map valueNames -> midiValues
    LinkedHashMap<Object,Integer> map = new LinkedHashMap<Object,Integer>();
    for (int i=0; i<midiValues.length; i++)
      if (map.containsKey(valueNames[i]))
        throw SYX.IllArgExc(SYX.E_ARG_DUPVAL,"valueNames");
      else
        map.put(valueNames[i],midiValues[i]);

    // Set sequenced map
    setValueMap(map);
    // Final check already done by setValueMap(SequencedMap)
  }

  // -- API: Getters and Setters ----------------------------------------------

  /**
   * Returns the {@linkplain SyxDataStruct.PTYPE parameter type}.
   */
  public final SyxDataStruct.PTYPE getType()
  {
    if (this.valueMap!=null)
      return SyxDataStruct.PTYPE.ENUM_INT;
    if (this.valueNamesMap!=null)
      return SyxDataStruct.PTYPE.ENUM_STR;
    return SyxDataStruct.PTYPE.RANGE;
  }

  /**
   * Returns the {@link SyxDataStruct} containing this parameter.
   * 
   * @see #parent
   */
  public final SyxDataStruct getParent()
  {
    return this.parent;
  }

  /**
   * Returns the {@linkplain SyxDataStruct parameter name} in the {@link
   * #getParent() SyxDataStruct} containing this parameter.
   * 
   * @see #pname
   */
  public final char getPname()
  {
    return this.pname;
  }

  /**
   * Returns the unique parameter name in the {@link SyxMessage}.
   *
   * @see #UID
   */
  public final String getUID()
  {
    return this.UID;
  }

  /**
   * Returns the parameter name (as seen on hardware instrument).
   * 
   * @see #descr
   */
  public final String getDescr()
  {
    return this.descr;
  }

  /**
   * Returns the default MIDI value.
   * 
   * @see #defMidiValue
   */
  public final int getDefaultMidiValue()
  {
    return this.defMidiValue;
  }

  /**
   * Returns the MIDI value range.
   * 
   * @return An array of two integers: {@code [}{@link #minMidiValue}{@code
   *         ,}{@link #maxMidiValue}{@code ]}
   * @see #minMidiValue
   * @see #maxMidiValue
   */
  public final int[] getMidiValueRange()
  {
    return new int[]{this.minMidiValue,this.maxMidiValue};
  }

  /**
   * Returns the {@linkplain #modelValueOffset model value offset}.
   * 
   * @see #modelValueOffset
   */
  public final int getOffsValue()
  {
    return this.modelValueOffset;
  }

  /**
   * Returns an array of permissible MIDI values for {@linkplain PTYPE 
   * enumerated} parameters or {@code null} for {@linkplain PTYPE range}
   * parameters. The returned array is a copy of the internal data.
   * 
   * @see #valueMap
   * @see #valueNamesMap
   */
  public final int[] getMidiValues()
  {
    Collection<Integer> values = null;
    if (this.valueMap!=null)
      values = this.valueMap.values();
    else if (this.valueNamesMap!=null)
      values = this.valueNamesMap.values();

    if (values==null)
      return null;

    int[] a = new int[values.size()];
    int i = 0;
    for (Integer v : values)
    {
      a[i] = v;
      i++;
    }
    return a;
  }

  /**
   * Returns the model value range.
   * 
   * @return An array of two integers containing the smallest and the largest
   *         permissible model value iff this parameter is
   *         <ul style="margin-top:0;margin-bottom:0.2em">
   *           <li>a {@linkplain PTYPE#RANGE range} parameter, or
   *             </li>
   *           <li>an {@linkplain PTYPE#ENUM_INT enumerated integers} parameter
   *             whose model values are consecutive</ul>
   *         </ul>
   *         In all other cases, the method returns {@code null}.
   * 
   * @see #getModelValues()
   * @see #valueMap
   * @see #modelValueRange
   */
  public final int[] getModelValueRange()
  {
    return this.modelValueRange;
  }

  /**
   * Returns the permissible model values.
   * 
   * @return A array of integer model values; {@code null} for {@linkplain
   * PTYPE#ENUM_STR enumerated value names} parameters.
   * 
   * @see #getModelValueRange()
   */
  public final int[] getModelValues()
  {
    if (this.modelValueRange!=null)
      return SYX.getIntRange(this.modelValueRange[0],this.modelValueRange[1]);
    else if (this.valueMap!=null)
    {
      int[] a = new int[this.valueMap.size()];
      int i = 0;
      for (Integer v : this.valueMap.keySet())
      {
        a[i] = v;
        i++;
      }
      return a;
    }
    else
      return null;
  }

  /**
   * Returns an array of names for all permissible MIDI values in case of 
   * {@linkplain PTYPE enumerated value names} parameters or {@code null} in
   * case of {@linkplain PTYPE enumerated integer values} or {@linkplain PTYPE
   * range} parameters. The returned array is a copy of the internal data.
   * 
   * @see #valueNamesMap
   */
  public final String[] getValueNames()
  {
    if (this.valueNamesMap!=null)
      return this.valueNamesMap.keySet().toArray(new String[0]);
    else
      return null;
  }

  /**
   * Returns the {@linkplain SyxDataStruct format specifier} of the system 
   * exclusive parameter change message, may be {@code null}.
   */
  public final String getParamChangeMsgFormat()
  {
    return this.paramChangeMsgFormat;
  }

  /**
   * Sets the default MIDI value.
   * 
   * <p><b>Note:</b> If the parameter has another than the {@linkplain PTYPE#RAW
   * default} {@linkplain #setValueRange(int, int, int) value range} or a
   * {@linkplain #setValueMap(SequencedMap) value map}, they must be set
   * <em>before</em> setting the default value. Otherwise {@linkplain
   * #validateMidiValue(int) validation} may fail.</p>
   * 
   * @param defMidiValue
   *          The default MIDI value
   * @throws InvalidMidiDataException
   *          if {@code defMidiValue} is not permissible
   * @see #setDefaultModelValue(int))
   * @see #setDefaultModelValue(String)
   * @see #defMidiValue
   */
  public final void setDefaultMidiValue(int defMidiValue)
  throws InvalidMidiDataException
  {
    validateDefMidiValue(defMidiValue);
    this.defMidiValue = defMidiValue;
    check();
  }

  /**
   * Sets the default MIDI value. Convenience shortcut for {@link
   * #setDefaultMidiValue(int) setDefaultMidiValue}{@code (}{@link
   * #model2Midi(String) model2Midi}{@code (defModelValue))}.
   * 
   * <p><b>Note:</b> If the parameter has another than the {@linkplain PTYPE#RAW
   * default} {@linkplain #setValueRange(int, int, int) value range} or a
   * {@linkplain #setValueMap(SequencedMap) value map}, they must be set
   * <em>before</em> setting the default value. Otherwise {@linkplain
   * #validateDefMidiValue(int) validation} may fail.</p>
   * 
   * @param defModelValue
   *          The default model value
   * @throws InvalidMidiDataException
   *          if {@code defModelValue} is not permissible
   * @see #setDefaultMidiValue(int)
   * @see #setDefaultModelValue(String)
   * @see #defMidiValue
   */
  public final void setDefaultModelValue(int defModelValue)
  throws InvalidMidiDataException
  {
    setDefaultMidiValue(model2Midi(defModelValue));
  }

  /**
   * Sets the default MIDI value. Convenience shortcut for {@link
   * #setDefaultMidiValue(int) setDefaultMidiValue}{@code (}{@link
   * #model2Midi(String) model2Midi}{@code (defModelValue))}.
   * 
   * <p><b>Note:</b> If the parameter has another than the {@linkplain PTYPE#RAW
   * default} {@linkplain #setValueRange(int, int, int) value range} or a
   * {@linkplain #setValueMap(SequencedMap) value map}, they must be set
   * <em>before</em> setting the default value. Otherwise {@linkplain
   * #validateMidiValue(int) validation} may fail.</p>
   * 
   * @param defModelValue
   *          The default model value
   * @throws InvalidMidiDataException
   *          if {@code defModelValue} is not permissible
   * @see #setDefaultModelValue(int)
   * @see #setDefaultMidiValue(int)
   * @see #defMidiValue
   */
  public final void setDefaultModelValue(String defModelValue)
  throws InvalidMidiDataException
  {
    setDefaultMidiValue(model2Midi(defModelValue));
  }

  /**
   * Sets the {@linkplain SyxDataStruct format specifier} of the system
   * exclusive parameter change message.
   * 
   * @param The format specifier
   */
  public final void setParamChangeMsgFormat(String format)
  {
    this.paramChangeMsgFormat = format;
    check();
  }

  // -- API: Validation -------------------------------------------------------

  /**
   * Validates a MIDI value. Internal implementation which is invoked by {@link
   * #validateMidiValue(int,boolean)} and {@link #validateDefMidiValue(int)}.
   * 
   * @param midiValue
   *          The MIDI value
   * @throws InvalidMidiDataException
   *          if {@code midiValue} is not permissible
   */
  private final void int_validateMidiValueGeneric(int midiValue)
  throws InvalidMidiDataException
  {
    if (midiValue<this.minMidiValue || midiValue>this.maxMidiValue)
      throw SYX.InvMdataExc
      (
        "MIDI value 0x%02X for parameter '%s' is out of range [0x%02X...0x%02X]",
        midiValue, this.UID, this.minMidiValue, this.maxMidiValue
      );

    Collection<Integer> midiValues = null;
    if (this.valueNamesMap!=null)
      midiValues = this.valueNamesMap.values();
    else if (this.valueMap!=null)
      midiValues = this.valueMap.values();
    if (midiValues==null)
      return;
    if (!midiValues.contains(midiValue))
      throw SYX.InvMdataExc
      (
        "MIDI value 0x%02X for '%s' is not in permissible value set",
        midiValue,this.UID
      );
  }

  /**
   * Validates a MIDI value. If the MIDI value is permissible, the method will
   * return normally. Otherwise the method will throw an {@link
   * InvalidMidiDataException}. 
   * 
   * <p><b>Notes:</b>
   * <ul style="margin-bottom:0pt">
   *   <li>The method is invoked
   *     <ul style="margin-bottom:0pt">
   *       <li><em>before</em> the parameter value is set, reset or initialized,
   *         and </li>
   *       <li>while the {@link #getParent() SyxDataStruct} containing this
   *         parameter is {@linkplain SyxDataStruct#validate(char)
   *         validated}</li>
   *     </ul></li>
   *   <li>For {@linkplain PTYPE#RANGE range} parameters, the method checks
   *     whether {@code midiValue} is in [{@link #minMidiValue}, {@link
   *     #maxMidiValue}]. For {@linkplain PTYPE enumerated} parameters, the
   *     method checks whether {@code midiValue} is contained in the
   *     {@linkplain #valueMap value map} or in the {@linkplain #valueNamesMap
   *     value names map}, respectively.</li>
   *   <li>Derived classes may override this method. They should invoke the
   *     base class implementation. If {@code init} is {@code true}, overrides
   *     should <em>not</em> check dependencies on other parameter values.
   *     This may fail because dependencies may not have been properly
   *     initialized yet.</li>
   * </ul></p>
   * 
   * @param midiValue
   *          The MIDI value
   * @param onInit
   *          {@code true} if the parameter value is being reset or initialized,
   *          {@code false} if the parameter value is being set or during model
   *          validation
   * @throws InvalidMidiDataException
   *          if {@code midiValue} is not permissible
   */
  public void validateMidiValue(int midiValue, boolean onInit)
  throws InvalidMidiDataException
  {
    int_validateMidiValueGeneric(midiValue);
  }

  /**
   * Validates a default MIDI value. If the default MIDI value is permissible,
   * the method will return normally. Otherwise the method will throw an {@link
   * InvalidMidiDataException}.
   * 
   * <p><b>Notes:</b>
   * <ul style="margin-bottom:0pt">
   *   <li>The method is invoked
   *     <ul style="margin-bottom:0pt">
   *       <li>while the {@link #getParent() SyxDataStruct} containing this
   *         parameter is {@linkplain SyxDataStruct#validateDefMidiValue(char)
   *         validated}, and</li>
   *       <li><em>after</em> the default parameter value has been retrieved.
   *         </li>
   *     </ul></li>
   *   <li>For {@linkplain PTYPE#RANGE range} parameters, the method checks
   *     whether {@code defMidiValue} is in [{@link #minMidiValue}, {@link
   *     #maxMidiValue}]. For {@linkplain PTYPE enumerated} parameters, the
   *     method checks whether {@code defMidiValue} is contained in the
   *     {@linkplain #valueMap value map} or in the {@linkplain #valueNamesMap
   *     value names map}, respectively.</li>
   *   <li>Derived classes may override this method. They should invoke the
   *     base class implementation.</li>
   * </ul></p>
   * 
   * @param defMidiValue
   *          The default MIDI value
   * @throws InvalidMidiDataException
   *          if {@code defaultMidiValue} is not permissible
   */
  public void validateDefMidiValue(int defMidiValue)
  throws InvalidMidiDataException
  {
    int_validateMidiValueGeneric(defMidiValue);
  }

  // -- Other API -------------------------------------------------------------

  /**
   * Converts a MIDI value to its model value.
   * 
   * @param midiValue
   *          The MIDI value
   * @return The respective model value
   * @throws InvalidMidiDataException
   *          if {@code midiValue} is not permissible
   */
  public final int midi2Model(int midiValue)
  throws InvalidMidiDataException
  {
    int_validateMidiValueGeneric(midiValue);
    if (this.valueMap!=null)
    {
      for (Map.Entry<Integer,Integer> e : this.valueMap.entrySet())
        if (e.getValue()==midiValue)
          return e.getKey();
      throw new InvalidMidiDataException();
    }
    else if (this.valueNamesMap!=null)
      return midiValue;
    else
      return midiValue+this.modelValueOffset;
  }

  /**
   * Converts a MIDI value to a string representation of its model value. For
   * {@linkplain PTYPE#ENUM_STR enumerated names parameters}, the method returns
   * the value name. For all other {@linkplain SyxDataStruct.PTYPE parameter types}, the 
   * method returns a string representation of the model value as obtained by
   * {@link #midi2Model(int) midi2Model}{@code (midiValue)}. 
   * 
   * @param midiValue
   *          The MIDI value
   * @return A string representation of the respective model value.
   * @throws InvalidMidiDataException
   *          if {@code midiValue} is not permissible
   * @see #valueNames
   */
  public final String midi2ModelAsString(int midiValue)
  throws InvalidMidiDataException
  {
    int_validateMidiValueGeneric(midiValue);

    if (this.valueNamesMap==null)
      return String.valueOf(midi2Model(midiValue));

    for (String key : this.valueNamesMap.keySet())
      if (this.valueNamesMap.get(key)==midiValue)
        return key;

    throw new InvalidMidiDataException();
  }

  /**
   * Converts a model value to its MIDI value.
   * 
   * @param modelValue The model value
   * @return The respective MIDI value
   * @throws InvalidMidiDataException
   *          if {@code modelValue} is not permissible
   */
  public final int model2Midi(int modelValue)
  throws InvalidMidiDataException
  {
    int midiValue;
    if (this.valueMap!=null)
    {
      Integer v = this.valueMap.get(modelValue);
      if (v==null)
        throw SYX.InvMdataExc("Invalid model value '%s'",modelValue);
      midiValue = v;
    }
    else if (this.valueNamesMap!=null)
      midiValue = modelValue;
    else
      midiValue = modelValue-this.modelValueOffset;

    int_validateMidiValueGeneric(midiValue);
    return midiValue;
  }

  /**
   * Converts a string representation of a model value to its MIDI value. The
   * MIDI value is obtained as follows:
   * <ol>
   *   <li>If {@code valueName} represents an integer, the method will return
   *   the respective MIDI value as obtained by {@linkplain #model2Midi(int)
   *   model2Midi}{@code (}{@link Integer}{@code .}{@link Integer#valueOf(int)
   *   valueof}{@code (valueName))}.</li>
   *   <li>For {@linkplain PTYPE#ENUM_STR enumerated names parameters}, the
   *   method returns the MIDI value for {@code valueName}.</li>
   *   <li>If both of the above fail, the method will throw a {@link
   *   InvalidMidiDataException}.</li>
   * </ol> 
   * 
   * @param valueName
   *          A string representation of the model value
   * @return The respective MIDI value
   * @throws IllegalArgumentException
   *          if {@code valueName} is {@code null} or empty
   * @throws InvalidMidiDataException
   *          if {@code valueName} is not permissible
   */
  public final int model2Midi(String valueName)
  throws IllegalArgumentException, InvalidMidiDataException
  {
    if (valueName==null || valueName.length()==0)
      throw new IllegalArgumentException();
    if (this.valueNamesMap!=null)
    { // Enumerated value names type: Lookup integer value for value name
      if (this.valueNamesMap.containsKey(valueName))
        return this.valueNamesMap.get(valueName);
      else
        throw new InvalidMidiDataException();
    }
    else
    { // All other types: Try to convert value name to integer
      try
      {
        return model2Midi(Integer.valueOf(valueName));
      }
      catch (NumberFormatException e)
      {
        throw new InvalidMidiDataException();
      }
    }
  }

  // -- API: Pretty-Printing --------------------------------------------------

  /**
   * Pretty-prints this parameter info to a string.
   * 
   * @param linePrefix
   *          A prefix string for printed lines (not applied to the first line)
   * @return The printed string
   */
  public String prettyPrint(String linePrefix)
  {
    String   lx = linePrefix;     // Actual line prefix
    String   s  = "";             // Output string
    int      dv = getDefaultMidiValue();
    int[]    vr = getMidiValueRange();
    int[]    vv = getMidiValues();
    int[]    vm = getModelValues();
    String[] vn = getValueNames();
    int      vo = getOffsValue();
    String   mf = getParamChangeMsgFormat();


    s += String.format("default : ");
    s += String.format(vr[1]<256 ? "0x%02X" : "0x%04X",dv);
    s += String.format("  %d",dv);
    if (vr[1]==127 && dv>=0x20 && dv<=0x7E)
      s += String.format("  '%c'",(char)dv);
    s += "\n";
    s += lx+String.format("UID     : '%s'\n",getUID());
    s += lx+String.format("descr   : '%s'\n",getDescr());
    s += lx+String.format("par.chg.: %s\n",mf!=null?mf:"(not assigned)");
    
    if (vv!=null)
    {
      s += lx+String.format("values  : ");
      for (int k=0; k<vv.length; k++)
      {
        if (vn!=null)
          s += String.format("0x%02X: '%s'",vv[k],vn[k]);
        else if (vm!=null)
          s += String.format("0x%02X: %5d",vv[k],vm[k]);
        else
          s += String.format("0x%02X",vv[k]);
        if (k<vv.length-1)
          s += "\n"+lx+" ".repeat(10);
      }
      s += "\n";
    }
    else
    {
      s += lx+String.format("val.rng.: [%d,%d]\n",vr[0],vr[1]);
      s += lx+String.format("val.ofs.: %d\n",vo);
    }

    return s;
  }

  /**
   * Pretty-prints the set of model values to a string.
   * 
   * @param linePrefix
   *          A prefix string for printed lines (not applied to the first line)
   * @param lineLength
   *          The maximal print line length (excluding the prefix string)
   * @return The printed string
   */
  public String prettyPrintModelValueSet(String linePrefix, int lineLength)
  {
    // Print model value set into a token list
    ArrayList<String> tokens = new ArrayList<String>();
    int[] minmax = getModelValueRange();
    if (minmax!=null)
    {
      tokens.add(String.format("[ %d, %d ]",minmax[0],minmax[1]));
    }
    else if (getType()==PTYPE.ENUM_INT)
    {
      tokens.add("{");
      int[] vv = getModelValues();
      for (int i=0; i<vv.length; i++)
        tokens.add(String.valueOf(vv[i])+(i<vv.length-1?",":""));
      tokens.add("}");
    }
    else if (getType()==PTYPE.ENUM_STR)
    {
      tokens.add("{");
      String[] vn = getValueNames();
      for (int i=0; i<vn.length; i++)
        tokens.add(vn[i]+(i<vn.length-1?",":""));
      tokens.add("}");
    }
    else
      // Cannot happen
      throw SYX.InternalError(SYX.ERR("Unknown paramter type '%s'",getType()));

    // Convert token list into multi-line string
    String s         = ""; // Output string
    String line      = "";
    String lastToken = "";
    for (String token : tokens)
    {
      if (line.length()+lastToken.length() < lineLength-1)
      { // No line break
        if (lastToken.length()>0)
          line += lastToken+" ";
      }
      else
      { // Line break
        s += line+"\n"+linePrefix;
        line = "  "+lastToken+" ";
      }
      lastToken = token;
    }
    s += line+lastToken;

    // Return the pretty-printed string
    return s;
  }

  // -- Workers ---------------------------------------------------------------

  /**
   * Checks this parameter info object.
   */
  protected void check()
  {
    SYX.doAssert(this.parent!=null,"parent is null");
    assert SyxDataStruct.isValidParamName(this.pname);
    try
    {
      checkUID(this.UID);
    }
    catch (IllegalArgumentException e)
    {
      SYX.doAssert(false,e.getMessage());
    }
    SYX.doAssert(this.defMidiValue>=0,"defMidiValue is negative");
    SYX.doAssert(this.minMidiValue>=0,"minMidiValue is negative");
    SYX.doAssert(this.maxMidiValue>=0,"maxMidiValue is negative");
    SYX.doAssert
    (
      this.maxMidiValue>=this.minMidiValue,
      "minMidiValue must be less than or equal maxMidiValue"
    );
    SYX.doAssert
    (
      this.valueMap==null || this.valueNamesMap==null,
      "Either SyxParamInfo.valueMap or SyxParamInfo.valueNamesMap must be null"
    );
  }

  /**
   * Checks if a unique parameter name is valid. Valid UIDs
   * <ul>
   *   <li>cannot be {@code null} or empty, and</li>
   *   <li>must contain only word characters and {@code '.'}</li>
   * </ul>
   * 
   * @param UID
   *          The unique parameter name to check
   * @throws IllegalArgumentException
   *          if {@code UID} is invalid
   */
  public static void checkUID(String UID)
  throws IllegalArgumentException
  {
    if (UID==null || UID.length()==0)
      throw SYX.IllArgExc("UIDs cannot be null or empty","UID");
    Pattern pattern = Pattern.compile("[a-zA-Z_0-9.]+");
    Matcher matcher = pattern.matcher(UID);
    if (!matcher.matches())
      throw SYX.IllArgExc("UIDs must contain word characters and '.' only");
  }

  /**
   * Returns the greatest MIDI value that can be stored for this parameter in
   * the {@link SyxDataStruct} that contains it.
   * 
   * @return {@code (1<<}{@link #parent}{@code .}{@link
   *         SyxDataStruct#getParamBitCount(char) getParamBitCount}{@code
   *         (}{@link #pname}{@code ))-1}
   */
  private final int int_getMaxStorageMidiValue()
  {
    return (1<<this.parent.getParamBitCount(this.pname))-1;
  }

}

// EOF