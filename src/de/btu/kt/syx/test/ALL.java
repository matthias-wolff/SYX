package de.btu.kt.syx.test;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

/**
 * Run JUnit test cases.
 * 
 * @author Matthias Wolff
 */
public class ALL
{

  // -- Tests -----------------------------------------------------------------

  public static void main(String[] args)
  {
    JUnitCore junit = new JUnitCore();
    junit.addListener(new TextListener(System.out));
    junit.run(
      SyxListMidiDevicesTest.class,
      SyxChecksumTest.class,
      SyxDataStructTest.class,
      SyxMessageTest.class
    );
  }
  
}

// EOF