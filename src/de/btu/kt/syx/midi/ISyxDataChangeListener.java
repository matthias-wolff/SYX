package de.btu.kt.syx.midi;

/**
 * Classes implementing this interface can register with {@link SyxDataStruct}s
 * in order to be notified when the system exclusive data have been changed by
 * {@link SyxDataStruct#setData(byte[], int)} or {@link
 * SyxDataStruct#setData(byte[])}. 
 * 
 * <p><b>Note:</b> The {@code setMidiValue}{@code (...)} and {@code
 * setModelValue}{@code (...)} methods of {@link SyxDataStruct} do <em>not</em>
 * notify the listeners.</p>
 * 
 * @author Matthias Wolff
 */
public interface ISyxDataChangeListener
{
  /**
   * Invoked when a {@linkplain SyxDataStruct system exclusive data struct} has
   * changed.
   * 
   * @param ds
   *          The data struct
   * @see SyxDataStruct#addChangeListener(ISyxDataChangeListener)
   */
  public void syxDataChanged(SyxDataStruct ds);
}

// EOF