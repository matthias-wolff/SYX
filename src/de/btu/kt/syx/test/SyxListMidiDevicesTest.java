package de.btu.kt.syx.test;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.util.MidiInterface;

public class SyxListMidiDevicesTest extends ASyxTestCase
{
  // -- Constants -------------------------------------------------------------

  // Test Names
  private static final String TEST_NAME
    = "SyxListMidiDevicesTest: List MIDI Devices";

  // -- Tests -----------------------------------------------------------------
  
  @Test
  @DisplayName(TEST_NAME)
  public void test()
  {
    System.out.println();
    setLogIndent(0);

    log("%s @%s\n",TEST_NAME,SYX.getHostName());
    logHrule();
    log("\n");

    log(MidiInterface.printMidiDeviceList());
  }

}

// EOF