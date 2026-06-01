package de.btu.kt.syx.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.util.MidiInterface;

/**
 * Classes implementing this interface can register with {@link MidiInterface}s
 * in order to receive system exclusive messages.
 * 
 * @author Matthias Wolff
 */
public interface ISysexMessageListener
{
  
  /**
   * Called when a MIDI system exclusive message has been received. 
   * Implementations may reject the message by throwing an exception.
   * 
   * @param sxMsg
   *          The message
   * @param timeStamp
   *          TimeStamp the time-stamp for the message, in microseconds
   * @throws InvalidMidiDataException
   *          if the message is rejected
   */
  public void receiveSysexMsg(SysexMessage sxMsg, long timeStamp) 
  throws InvalidMidiDataException;

  /**
   * Shorthand for {@link #receiveSysexMsg(SysexMessage, long) 
   * receiveSysexMsg}{@code (sxMsg,-1)}.
   *  
   * @param sxMsg
   *          The message
   * @throws InvalidMidiDataException
   *          if the message is rejected
   */
  public default void receiveSysexMsg(SysexMessage sxMsg) 
  throws InvalidMidiDataException
  {
    receiveSysexMsg(sxMsg,-1);
  }

  /**
   * Returns the device number of the hardware instrument associated with the
   * listener. If the hardware instrument does not support device numbers,
   * implementations should return 0.
   */
  public int getDevNum();

}

// EOF