package de.btu.kt.syx.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.sound.midi.InvalidMidiDataException;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.SyxChecksum;
import de.btu.kt.syx.midi.SyxDataStruct;
import de.btu.kt.syx.midi.SyxMessage;

public class SyxMessageTest extends ASyxTestCase
{

  // -- Constants -------------------------------------------------------------

  // Test Names
  private static final String TEST_CREATEWITHVALUES
    = "SyxMessageTest: Create a SyxMessage with Parameter Values "
    + "(for sending SysEx)";

  private static final String TEST_PARSEDATA
    = "SyxMessageTest: Create a SyxMessage by Parsing a MIDI Data Buffer "
    + "(for receiving SysEx)";

  private static final String TEST_SERIALIZATION
    = "SyxMessageTest: Serialization and Deserialization";

  // -- Tests -----------------------------------------------------------------

  @Test
  @DisplayName(TEST_CREATEWITHVALUES)
  public void testCreateWithParameters() 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_CREATEWITHVALUES);
    logHrule();
    log("\n");

    SyxDataStruct sds = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BULK_REQUEST);
    sds.setName("Yamaha TG55 Multi Bulk Request");
    sds.setMidiValue('#',0        ); // Device ID
    sds.setMidiValue('s',(byte)'M'); // Bulk type, 1st character
    sds.setMidiValue('t',(byte)'U'); // Bulk type, 2nd character
    sds.setMidiValue('x',0x7F     ); // Memory type - edit buffer
    sds.setMidiValue('x',0x00     ); // Memory number - (not used)

    SyxMessage msg = new SyxMessage();
    msg.addParts(sds);

    log("SyxMessage(format,data)\n");
    log("- format: %s\n",TG55FormatSpecifiers.F_TG55_BULK_REQUEST);
    log(0,3,"-> MidiMessage:\n");
    log(SYX.prettyPrintMidiMessage(msg));
    log(0,-3,"\n\n");

    log(0,3,"-> SyxMessage:\n");
    log(msg.prettyPrint());
    log(0,-3,"\n");
  }

  @Test
  @DisplayName(TEST_PARSEDATA)
  public void testParseData() 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_PARSEDATA);
    logHrule();
    log("\n");

    // Create a SysEx message format and (meaningless) SysEx message data
    byte[] data = new byte[] {
      (byte)0xF0, // Status (system exclusive)
      0x43, 0x10, 0x35, 0x00, 0x00, 0x7E, 0x10, 0x01, 0x7F,
      (byte)0xF7  // EOX
    };

    // Create a SysEx message
    SyxMessage msg = new SyxMessage(TG55FormatSpecifiers.F_TG55_PARCNG1,data);
    msg.getParts()[0].setName("Yamaha TG55 Parameter Change (meaningless)");
    log("SyxMessage(format,data)\n");
    log("- format: %s\n",TG55FormatSpecifiers.F_TG55_PARCNG1);
    log("- data:   %s\n\n",SYX.prettyPrintByteArray(data," ".repeat(8)));
    log(0,3,"-> MidiMessage:\n");
    log(SYX.prettyPrintMidiMessage(msg));
    log(0,-3,"\n\n");

    log(0,3,"-> SyxMessage:\n");
    log(msg.prettyPrint());
    log(0,-3,"\n");
  }

  @Test
  @DisplayName(TEST_SERIALIZATION)
  public void testSerialization()
  throws ClassNotFoundException, IOException,
         IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_SERIALIZATION);
    logHrule();
    log("\n");

    // A TG55 bulk dump
    byte[] data = new byte[] 
    {
      (byte)0xF0, 
      0x43, 0x00, 0x7A, 0x01, 0x3A, 0x4C, 0x4D, 0x20,
      0x20, 0x38, 0x31, 0x30, 0x33, 0x4D, 0x55, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x7F, 0x00, 0x4D,
      0x69, 0x73, 0x73, 0x20, 0x47, 0x52, 0x20, 0x20,
      0x20, 0x00, 0x0A, 0x64, 0x3E, 0x1F, 0x32, 0x40,
      0x00, 0x22, 0x7F, 0x40, 0x34, 0x20, 0x64, 0x00,
      0x40, 0x00, 0x03, 0x7F, 0x40, 0x4C, 0x20, 0x64,
      0x00, 0x40, 0x00, 0x0D, 0x7F, 0x40, 0x40, 0x20,
      0x31, 0x00, 0x40, 0x00, 0x02, 0x7F, 0x40, 0x40,
      0x20, 0x64, 0x00, 0x40, 0x01, 0x01, 0x7F, 0x40,
      0x40, 0x20, 0x64, 0x00, 0x00, 0x00, 0x01, 0x7F,
      0x40, 0x40, 0x20, 0x00, 0x00, 0x00, 0x00, 0x02,
      0x7F, 0x40, 0x34, 0x20, 0x00, 0x00, 0x40, 0x00,
      0x3F, 0x28, 0x40, 0x40, 0x20, 0x18, 0x03, 0x40,
      0x00, 0x0A, 0x7F, 0x40, 0x40, 0x20, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x40, 0x40, 0x40, 0x20, 0x00,
      0x00, 0x40, 0x00, 0x0A, 0x7F, 0x40, 0x40, 0x20,
      0x00, 0x00, 0x40, 0x00, 0x22, 0x7F, 0x40, 0x40,
      0x20, 0x64, 0x00, 0x00, 0x00, 0x0B, 0x7F, 0x40, 
      0x40, 0x20, 0x64, 0x00, 0x00, 0x00, 0x07, 0x7F,
      0x40, 0x40, 0x20, 0x00, 0x00, 0x00, 0x01, 0x00,
      0x7F, 0x40, 0x40, 0x20, 0x00, 0x00, 0x00, 0x00,
      0x06, 0x7F, 0x40, 0x40, 0x20, 0x00, 0x00, 0x0F,
      (byte)0xF7
    };

    // Create temp file
    log("Create a temp file ...\n");
    File tempFile = File.createTempFile("SyxDataStructTest-", ".tmp");
    tempFile.deleteOnExit();
    log("- Name: \"%s\"\n",tempFile.getAbsolutePath());
    log("ok\n\n");
    
    //Crate a SyxMessage    
    log(0,2,"Create a SyxMessage ...\n");
    SyxMessage msg1 = new SyxMessage();
    msg1.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1 ,"Bulk header 1"));
    msg1.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER2 ,"Bulk header 2"));
    msg1.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BDMU_HEADER,"Multi header" ));
    msg1.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BDMU_EFFECT,"Effect"       ));
    for (int i=0; i<16; i++)
    {
      String name = String.format("Channel %d Voice",i+1);
      msg1.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BDMU_VOICE,name));
    }
    msg1.addParts(new SyxChecksum(SyxChecksum.ROLAND,1,-1));
    msg1.setMessage(data,data.length);
    log("%s\n",msg1.prettyPrint());
    log(0,-2,"ok\n\n");

    // Write SyxMessage to file
    log("Serialize data struct to temp file ... ");
    FileOutputStream fileOutputStream
      = new FileOutputStream(tempFile);
    ObjectOutputStream objectOutputStream 
      = new ObjectOutputStream(fileOutputStream);
    objectOutputStream.writeObject(msg1);
    objectOutputStream.flush();
    objectOutputStream.close();
    log("ok\n\n");
    
    // Read SyxMessage from temp file
    log(0,2,"Deserialize data struct from temp file ...\n");
    FileInputStream fileInputStream
      = new FileInputStream(tempFile);
    ObjectInputStream objectInputStream
      = new ObjectInputStream(fileInputStream);
    SyxMessage msg2 = (SyxMessage)objectInputStream.readObject();
    objectInputStream.close(); 
    log("%s\n",msg2.prettyPrint());
    log(0,-2,"ok\n\n");
    
    // Compare msg1 and msg2
    log("Compare deserialized message with original ... ");
    assertTrue(msg1.equals(msg2));
    log("ok\n");
  }

}

// EOF