package de.btu.kt.syx.devices;

import javax.sound.midi.InvalidMidiDataException;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.NotSupportedException;

/**
 * Implementations represent one synthesizer multi. A multi features a number of
 * parts to which {@linkplain IPatch patches} can be assigned.
 *
 * @author Matthias Wolff
 */
public interface IMulti extends BankTree.IBankObject
{

  // -- API: General ---------------------------------------------------------- 

  /**
   * Returns the name of the multi.
   * 
   * @see #setName(String)
   */
  @Override
  public String getName();

  /**
   * Sets the name of the multi.
   * 
   * @param name
   *          The name
   * 
   * @see #getName()
   */
  void setName(String name);

  /**
   * Returns the number of multi parts.
   */
  public int getNumberOfParts();

  // -- API: Part Patches -----------------------------------------------------

  /**
   * Returns the unique patch bank identifier of the {@linkplain
   * #getPatchUID(int) patch assigned} to a multi part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The unique patch bank identifier
   * @throws IllegalArgumentException
   *          if the argument is out of range
   * 
   * @see #getPatchUID(int)
   * @see #setPatch(int, String, String)
   */
  public String getPatchBankUID(int part)
  throws IllegalArgumentException;

  /**
   * Returns the unique identifier of the patch assigned to a multi part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The unique identifier of the patch in {@linkplain
   *         #getPatchBankUID(int) its patch bank}
   * @throws IllegalArgumentException
   *          if the argument is out of range
   * @see #getPatchBankUID(int)
   * @see #setPatch(int, String, String)
   */
  public String getPatchUID(int part)
  throws IllegalArgumentException;

  /**
   * Assigns a patch to a multi part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param bankUID
   *          The unique patch bank identifier of the patch to set
   * @param patchUID
   *          The unique identifier of the patch to set in the patch bank
   *          identified by {@code bankUID}
   * @throws IllegalArgumentException if
   *          <ul style="margin-top:0">
   *            <li>if {@code part} is out of range, or</li>
   *            <li>if {@code bankUID} is {@code null} or empty, or if no such
   *              bank exists, or</li>
   *            <li>if {@code patchUID} is {@code null} or empty, or if no such
   *              patch exists in the patch bank identified by {@code bankUID}
   *              </lu>
   *          </ul>
   * 
   * @see #getPatchBankUID(int)
   * @see #getPatchUID(int)
   */
  public void setPatch(int part, String bankUID, String patchUID)
  throws IllegalArgumentException;

  // -- API: Part On/Off ------------------------------------------------------

  /**
   * Determines whether the synthesizer supports switching multi parts off.
   * 
   * @see #isPartActive(int)
   * @see #setPartActive(int, boolean)
   */
  public boolean supportsSetPartActive();

  /**
   * Determines whether a multi part is on or off.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @throws IllegalArgumentException
   *          If the argument is out of range
   * 
   * @see #supportsSetPartActive()
   * @see #setPartActive(int, boolean)
   */
  public boolean isPartActive(int part)
  throws IllegalArgumentException;

  /**
   * Switches a multi part on or off.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param active
   *          {@code true} to switch the part on, {@code false} to switch the
   *          part off
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsSetPartActive()
   *          support switching parts off}
   * @throws IllegalArgumentException
   *          if {@code part} is out of range
   * 
   * @see #supportsSetPartActive()
   * @see #isPartActive(int)
   */
  public default void setPartActive(int part, boolean active)
  throws NotSupportedException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  // -- API: Part MIDI Channels -----------------------------------------------

  /**
   * Determines whether the synthesizer supports assigning a MIDI channel to
   * each multi part.
   * 
   * @see #getMidiChannel(int)
   * @see #setMidiChannel(int, int)
   */
  public boolean supportsSetMidiChannel();

  /**
   * Returns the MIDI channel assigned to a multi part.
   *
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The one-based MIDI channel &isin; [1,16]
   * @throws IllegalArgumentException
   *          if the argument is out of range
   * 
   * @see #supportsSetMidiChannel()
   * @see #setMidiChannel(int, int)
   */
  public int getMidiChannel(int part)
  throws IllegalArgumentException;

  /**
   * Sets the MIDI channel assigned to a multi part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param channel
   *          The one-based MIDI channel &isin; [1,16]
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsSetMidiChannel()
   *          support assigning a MIDI channel to each multi part}
   * @throws IllegalArgumentException
   *          if either argument is out of range
   * 
   * @see #supportsSetMidiChannel()
   * @see #getMidiChannel(int)
   */
  public default void setMidiChannel(int part, int channel)
  throws NotSupportedException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  // -- API: Part Polyphony & Reserve Notes -----------------------------------

  /**
   * Determines whether the synthesizer supports fixed voice allocation. Fixed
   * voice allocation&mdash;in contrast to {@linkplain #supportsReserveNotes()
   * dynamic voice allocation}&mdash;allows to assign a fix number of voices to
   * a multi part.
   * 
   * @see #getPolyphony(int)
   * @see #setPolyphony(int, int)
   * @see #supportsReserveNotes()
   */
  public boolean supportsPolyphony();

  /**
   * Returns the number of voices fixedly assigned to a multi part. The default
   * implementation throws a {@link NotSupportedException}.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The number of voices assigned to {@code part}
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsPolyphony()
   *          support fixed voice allocation}
    * @throws IllegalArgumentException
   *          if the argument is out of range
   * 
   * @see #supportsPolyphony()
   * @see #setPolyphony(int, int)
   */
  public default int getPolyphony(int part)
  throws NotSupportedException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  /**
   * Sets the the number of voices fixedly assigned to a multi part. The default
   * implementation throws a {@link NotSupportedException}.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param polyphony
   *          The number of voices to assign to the part. Note that the
   *          greatest permissible value typically depends on the polyphony
   *          settings of the other parts.
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsPolyphony()
   *          support fixed voice allocation}
   * @throws InvalidMidiDataException
   *          if the number of voice specified by {@code polyphony} cannot be
   *          assigned
   * @throws IllegalArgumentException
   *          if either argument is out of range.
   *          
   * @see #supportsPolyphony()
   * @see #getPolyphony(int)
   */
  public default void setPolyphony(int part, int polyphony)
  throws NotSupportedException, InvalidMidiDataException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  /**
   * Determines whether the synthesizer supports reserving a number of voices
   * for each multi part. Synthesizers allowing reserving notes feature dynamic
   * voice allocation, i.e., they can automatically assign voices to multi parts
   * as needed.
   * 
   * @see #getReserveNotes(int)
   * @see #setReserveNotes(int, int)
   * @see #supportsPolyphony()
   */
  public boolean supportsReserveNotes();

  /**
   * Returns the number of voices reserved for a multi part. The default
   * implementation throws a {@link NotSupportedException}.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The number of voices reserved for {@code part}. The return value
   *         0 typically indicates automatic voice assignment. 
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsReserveNotes()
   *          support reserving notes}
   * @throws IllegalArgumentException
   *          if the argument is out of range
   * 
   * @see #supportsReserveNotes()
   * @see #setReserveNotes(int, int)
   */
  public default int getReserveNotes(int part)
  throws NotSupportedException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  /**
   * Sets the number of voices reserved for a multi part. The default
   * implementation throws a {@link NotSupportedException}.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param reserveNotes
   *         The number of voices to reserve for {@code part}. The value 0
   *         typically indicates automatic voice assignment.
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsReserveNotes()
   *          support reserving notes}
   * @throws InvalidMidiDataException
   *          if the number of voice specified by {@code reserveNotes} cannot be
   *          reserved
   * @throws IllegalArgumentException
   *          if either argument is out of range.
   * 
   * @see #supportsReserveNotes()
   * @see #getReserveNotes(int)
   */
  public default void setReserveNotes(int part, int reserveNotes)
  throws NotSupportedException, InvalidMidiDataException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

  // -- API: Part Volume ------------------------------------------------------

  /**
   * Gets the volume of a part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The volume &isin; [0,1]
   * @throws IllegalArgumentException
   *          if the argument is out of range
   */
  public float getVolume(int part)
  throws IllegalArgumentException;

  /**
   * Sets the volume of a part.
   * 
   * @param part
   *          The zero-based part number &isin; {@code [0,}{@link
   *          #getNumberOfParts()}{@code -1]}
   * @param volume
   *          The volume &isin; [0,1]
   * @throws IllegalArgumentException
   *           if either argument is out of range
   */
  public void setVolume(int part, float volume)
  throws IllegalArgumentException;

  // -- API: Part Detune ------------------------------------------------------

  /**
   * Gets the detuning of a part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The detuning in semitones. The decimal places denote fine detuning
   *         in cents. Detuning can be positive or negative; zero means no
   *         detuning.
   * @throws IllegalArgumentException
   *          if the argument is out of range
   * 
   * @see SYX#encodeDetune(String, int, int)
   */
  public float getDetune(int part)
  throws IllegalArgumentException;

  /**
   * Sets the detuning of a part.
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param detune
   *          The detuning in semitones. The decimal places denote fine detuning
   *          in cents. Detuning can be positive or negative; zero means no
   *          detuning.
   * @throws IllegalArgumentException
   *          if either argument is out of range. Note that the permissible
   *          detuning range depends on the synthesizer.
   * 
   * @see SYX#encodeDetune(String, int, int)
   */
  public void setDetune(int part, float detune)
  throws IllegalArgumentException;

  // -- API: Part Pan ---------------------------------------------------------

  /**
   * Determines whether the synthesizer supports panning.
   */
  public boolean supportsPan();

  /**
   * Gets the panning of a part
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @return The panning &isin; [-1,+1] where -1 means maximum left and +1
   *         denotes maximum right
   * @throws IllegalArgumentException
   *          if the argument is out of range
   */
  public float getPan(int part)
  throws IllegalArgumentException;

  /**
   * Gets the panning of a part
   * 
   * @param part
   *          The zero-based part number &isin; [0,{@link
   *          #getNumberOfParts()}-1]
   * @param pan
   *          The panning &isin; [-1,+1] where -1 means maximum left and +1
   *          denotes maximum right
   * @throws NotSupportedException
   *          if the synthesizer does not {@linkplain #supportsPan() support
   *          panning}
   * @throws IllegalArgumentException
   *          if either argument is out of range
   */
  public default void setPan(int part, float pan)
  throws NotSupportedException, IllegalArgumentException
  {
    throw new NotSupportedException();
  }

}

// EOF