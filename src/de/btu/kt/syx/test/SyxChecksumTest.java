package de.btu.kt.syx.test;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.SyxChecksum;
import de.btu.kt.syx.util.SysexRecorder;

public class SyxChecksumTest extends ASyxTestCase
{

  // -- Constants -------------------------------------------------------------

  // Test Names
  private static final String TEST_CHECKSUM
    = "SyxChecksum: General Test";

  // -- Tests -----------------------------------------------------------------

  @Test
  @DisplayName(TEST_CHECKSUM)
  public void testChecksum() throws IOException
  {
    URL    pUrl = SyxChecksumTest.class.getResource("SyxChecksumTest.class");
    String pStr = pUrl.toString();
    pStr = pStr.replace("file:/","");
    pStr = pStr.replace("/bin/","/src/");
    pStr = pStr.replace("/SyxChecksumTest.class","/resources");
    String fileName = pStr+"/SysExTest.txt"; int payLoadIdx = 6;
    //String fileName = pStr+"/SysExTest_ScDisplayLetter.txt"; int payLoadIdx = 4;

    SysexRecorder rec = new SysexRecorder();
    rec.readTxtFile(fileName);
    byte[] data = rec.getTape()[0].getData();

    int cs = data[data.length-2];
    System.out.println("Data /w checksum and EOX");
    System.out.println(SYX.prettyPrintByteArray(data,""));
    data = Arrays.copyOfRange(data,0,data.length-2);
    System.out.println("\nData w/o checksum and EOX");
    System.out.println(SYX.prettyPrintByteArray(data,""));
    byte[] payLoad = Arrays.copyOfRange(data,payLoadIdx,data.length);
    System.out.println("\nPayload");
    System.out.println(SYX.prettyPrintByteArray(payLoad,""));

    System.out.println(String.format("MESSAGE: 0x%02X",cs));
    System.out.println("SysEx data w/o checksum and EOX");
    cs = SyxChecksum.computeChecksum(SyxChecksum.ADD,data);
    System.out.println(String.format("- ADD     : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.XOR,data);
    System.out.println(String.format("- XOR     : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.ROLAND,data);
    System.out.println(String.format("- ROLAND  : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.CASIO_VZ,data);
    System.out.println(String.format("- CASIO VZ: 0x%02X",cs));
    System.out.println("SysEx payload");
    cs = SyxChecksum.computeChecksum(SyxChecksum.ADD,payLoad);
    System.out.println(String.format("- ADD     : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.XOR,payLoad);
    System.out.println(String.format("- XOR     : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.ROLAND,payLoad);
    System.out.println(String.format("- ROLAND  : 0x%02X",cs));
    cs = SyxChecksum.computeChecksum(SyxChecksum.CASIO_VZ,payLoad);
    System.out.println(String.format("- CASIO VZ: 0x%02X",cs));
  }

}
