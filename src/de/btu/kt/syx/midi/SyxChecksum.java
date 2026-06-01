package de.btu.kt.syx.midi;

import de.btu.kt.syx.SYX;

/**
 * A system exclusive checksum {@linkplain SyxDataStruct data struct}.
 * 
 * @author Matthias Wolff
 * @see <a href="https://github.com/shingo45endo/sysex-checksum/blob/main/sysex_parser.js"
 *      >https://github.com/shingo45endo/sysex-checksum/blob/main/sysex_parser.js</a>
 * @see <a href="https://www.facebook.com/groups/370365849972076/posts/1294397107568941/"
 *      >https://www.facebook.com/groups/370365849972076/posts/1294397107568941/</a>
 * @see <a href="https://forum.morningstar.io/t/how-to-calculate-sysex-checksum/8020/6"
 *      >https://forum.morningstar.io/t/how-to-calculate-sysex-checksum/8020/6</a>
 * @see <a href="https://morningstarengineering.atlassian.net/wiki/spaces/MMS/pages/918519809/SysEx+Documentation+for+External+Applications#CheckSum"
 *      >https://morningstarengineering.atlassian.net/wiki/spaces/MMS/pages/918519809/SysEx+Documentation+for+External+Applications#CheckSum</a>
 */
public class SyxChecksum extends SyxDataStruct
{

  private static final long serialVersionUID = 1L;

  // -- Constants -------------------------------------------------------------

  /**
   * Check sum aggregation by sum.
   */
  public static final int ADD = 0;

  /**
   * Check sum aggregation by xor.
   */
  public static final int XOR = 1;

  /**
   * Check sum aggregation Roland style.
   */
  public static final int ROLAND = 2;

  /**
   * Check sum aggregation Casio VZ-1/VZ-10M style.
   */
  public static final int CASIO_VZ = 3;

  /**
   * Largest checksum type ID.
   */
  protected static final int MAXTYPE = 3; 

  // -- Attributes ------------------------------------------------------------

  /**
   * The check sum type, one of {@link #ADD},  {@link #XOR} or {@link #ROLAND}
   */
  int nType;

  /**
   * Zero-based index of the first message part to include in checksum
   * computation
   */
  int nFirstPart;

  /**
   * Number of parts to include in checksum computation, -1 to include all parts
   * from {@code nFirstPart} to the end of the message
   */
  int nParts;

  // -- Constructors ----------------------------------------------------------

  /**
   * Create a new checksum data struct. The checksum will be computed over the
   * data bytes of the message parts specified by {@code nFirstPart} and 
   * {@code nParts}. Checksum parts will be excluded from checksum computation.
   * 
   * @param nType
   *          The check sum type, one of {@link #ADD},  {@link #XOR}, {@link 
   *          #ROLAND}, or {@link #CASIO_VZ}
   * @param nFirstPart
   *          Zero-based index of the first message part to include in checksum
   *          computation
   * @param nParts
   *          Number of parts to include in checksum computation, -1 to include
   *          all parts from {@code nFirstPart} to the end of the message
   */
  public SyxChecksum(int nType, int nFirstPart, int nParts)
  {
    super("ccH");

    if (nType<0 || nType>MAXTYPE)
      throw new IllegalArgumentException
      (
       String.format("Illegal type ID %d",nType)
     );

    this.name       = "Checksum";
    this.nType      = nType;
    this.nFirstPart = nFirstPart;
    this.nParts     = nParts;
  }

  // -- API -------------------------------------------------------------------

  /**
   * Returns the checksum stored in this data struct.
   */
  public byte getCheckSum()
  {
    return SYX.i2b(getMidiValue('c'));
  }

  /**
   * Sets the checksum stored in this data struct.
   * 
   * @param cs
   *          The checksum
   */
  public void setCheckSum(byte cs)
  {
    try
    {
      setMidiValue('c',SYX.i2b(cs));
    }
    catch (Exception e) 
    { // Cannot happen
      throw SYX.InternalError(e);
    }
  }

  /**
   * Computes the checksum over parts of a system exclusive message.
   * 
   * @param sxMsg
   *          The system exclusive message
   * @throws IllegalArgumentException
   *           if {@code sxMsg} is {@code null}
   * @see #SyxChecksum(int, int, int)
   */
  public byte computeCheckSum(SyxMessage sxMsg)
  throws IllegalArgumentException
  {
    // Sanity checks
    if (sxMsg == null)
      throw SYX.IllArgExc("Argument 'sxMsg' must not be null");
    int np = sxMsg.getParts().length;
    if (this.nParts<0)
      this.nParts = np-this.nFirstPart;

    // Compute checksum
    int n0 = this.nFirstPart;
    int n1 = this.nFirstPart+this.nParts;
    int len = 0;
    for (int n = n0; n<n1; n++)
      if (!(sxMsg.getPart(n) instanceof SyxChecksum))
        len += sxMsg.getPart(n).getLength();

    byte[] data = new byte[len];
    int bid = 0;
    for (int n = n0; n<n1; n++)
    {
      SyxDataStruct part = sxMsg.getPart(n); 
      if (!(part instanceof SyxChecksum))
      {
        int l = part.getLength();
        System.arraycopy(part.getData(),0,data,bid,l);
        bid += l;
      }
    }

    switch (this.nType)
    {
      case ADD     : return computeChecksumAdd(data);
      case XOR     : return computeChecksumXor(data);
      case ROLAND  : return computeChecksumRoland(data);
      case CASIO_VZ: return computeChecksumCasioVz(data);
    }
    throw new IllegalStateException();
  }

  /**
   * Validates the checksum of {@code sxMsg}. The method compares the checksum
   * stored in this data struct with the checksum computed for {@code sxMsg}.
   * 
   * @return {@code true} if the checksums are equal, {@code false} otherwise
   */
  public boolean validateCheckSum(SyxMessage sxMsg)
  throws IllegalArgumentException
  {
    return getCheckSum()==computeCheckSum(sxMsg);
  }

  // -- Static API ------------------------------------------------------------

  /**
   * Computes the SysEx checksum for a byte array.
   * 
   * @param nType
   *          The check sum type, one of {@link #ADD},  {@link #XOR}, {@link 
   *          #ROLAND}, or {@link #CASIO_VZ}
   * @param data
   *          The byte array
   * @return The checksum
   */
  public static byte computeChecksum(int nType, byte[] data)
  {
    switch (nType)
    {
    case ADD     : return computeChecksumAdd(data);
    case XOR     : return computeChecksumXor(data);
    case ROLAND  : return computeChecksumRoland(data);
    case CASIO_VZ: return computeChecksumCasioVz(data);
    }
    throw new IllegalArgumentException
    (
      String.format("Illegal type ID %d",nType)
    );
  }

  /**
   * Computes the SysEx checksum for a byte array aggregating by summation.
   * 
   * @param data
   *          The byte array
   * @return The checksum
   */
  public static byte computeChecksumAdd(byte[] data)
  {
    int cs = 0;
    for (byte b : data) 
      cs += b;
    return (byte)(cs % 0x80);
  }

  /**
   * Computes the SysEx checksum for a byte array aggregating by XOR.
   * 
   * @param data
   *          The byte array
   * @return The checksum
   */
  public static byte computeChecksumXor(byte[] data)
  {
    int cs = 0;
    for (byte b : data) 
      cs ^= b;
    return (byte)cs;
  }

  /**
   * Computes the SysEx checksum for a byte array aggregating Roland style.
   * 
   * @param data
   *          The byte array
   * @return The checksum
   */
  public static byte computeChecksumRoland(byte[] data)
  {
    return (byte)((0x80 - computeChecksumAdd(data)) % 0x80);
  }

  /**
   * Computes the SysEx checksum for a byte array aggregating Casio VZ style.
   * 
   * @param data
   *          The byte array
   * @return The checksum
   */
  public static byte computeChecksumCasioVz(byte[] data)
  {
    int cs = 0;
    for (int i=0; i<data.length; i+=2)
      cs += data[i]*16 + data[i+1];
    return (byte)((0x80 - (byte)cs) % 0x80);
  }

}

// EOF