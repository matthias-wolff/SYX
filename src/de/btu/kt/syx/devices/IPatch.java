package de.btu.kt.syx.devices;

/**
 * Implementations represent one synthesizer patch.
 * 
 * @author Matthias Wolff
 */
public interface IPatch extends BankTree.IBankObject
{

  /**
   * Returns the name of the patch.
   * 
   * @see #setName(String)
   */
  @Override
  public String getName();

  /**
   * Sets the name of the patch.
   * 
   * @param name
   *          The name
   * 
   * @see #getName()
   */
  public void setName(String name);

}

// EOF