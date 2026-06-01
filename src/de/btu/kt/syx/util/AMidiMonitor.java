package de.btu.kt.syx.util;

/**
 * Instances of this class can be registered with {@link MidiInterface}s in
 * order to receive a log of MIDI input and output.
 * 
 * @see MidiInterface#setMonitor(AMidiMonitor)
 * @author Matthias Wolff
 */
public abstract class AMidiMonitor
{

  /**
   *  Creates a new MIDI monitor.
   */
  public AMidiMonitor()
  { // Nothing to be done
  }

  /**
   * Called on MIDI input.
   * 
   * @param text
   *          The log text
   */
  public abstract void midiInLog(String text);

  /**
   * Called on MIDI output.
   * 
   * @param text
   *          The log text
   */
  public abstract void midiOutLog(String text);

  /**
   * Called for other log messages.
   * 
   * @param text
   *          The log text
   */
  public abstract void otherLog(String text);

}

// EOF