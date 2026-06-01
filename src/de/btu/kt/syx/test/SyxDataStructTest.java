package de.btu.kt.syx.test;

import static org.junit.Assert.assertTrue;

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
import de.btu.kt.syx.midi.SyxDataStruct;

public class SyxDataStructTest extends ASyxTestCase
{

  // -- Constants -------------------------------------------------------------

  // Test Names
  private static final String TEST_GETSETPARAMS
    = "SyxDataStructTest: Set/Get Parameters";
  private static final String TEST_SETDATA
    = "SyxDataStructTest: Set Data Bytes";
  private static final String TEST_CREATEWITHVALUES
    = "SyxDataStructTest: Create with Parameter Values";
  private static final String TEST_GETSETSTRINGPARAMS
    = "SyxDataStructTest: Set/Get Multiple Parameters from/to String";
  private static final String TEST_SERIALIZATION
    = "SyxDataStructTest: Serialization and Deserialization";

  // -- Tests -----------------------------------------------------------------

  @Test
  @DisplayName(TEST_GETSETPARAMS)
  public void testGetSetParams()
  throws InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_GETSETPARAMS);
    logHrule();
    log("\n");

    SyxDataStruct ds;
    
    log("SyxDataStruct(format)\n");
    log("- format: %s\n",TG55FormatSpecifiers.F_TG55_PARCNG2);
    ds = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_PARCNG2);
    ds.setMidiValue('#',0x00); // Device ID
    ds.setMidiValue('f',0x01); // Structure number - Filter 1
    ds.setMidiValue('e',0x02); // Structure number - Element 2
    ds.setMidiValue('c',0x00); // Structure number - (not used)
    ds.setMidiValue('p',0x15); // Parameter number - Cut-off level, scaling offset 1
    ds.setMidiValue('v',0xFF); // Parameter value 

    log(0,3,"\n-> SyxDataStruct:\n");
    log(ds.prettyPrint());
    log(0,-3,"\n");

    log("-> Struct parameters:\n");
    log("   - # = %3d\n",ds.getMidiValue('#'));
    log("   - f = %3d\n",ds.getMidiValue('f'));
    log("   - e = %3d\n",ds.getMidiValue('e'));
    log("   - c = %3d\n",ds.getMidiValue('c'));
    log("   - p = %3d\n",ds.getMidiValue('p'));
    log("   - v = %3d\n",ds.getMidiValue('v'));
  }

  @Test
  @DisplayName(TEST_SETDATA)
  public void testSetData() 
  throws IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_SETDATA);
    logHrule();
    log("\n");

    SyxDataStruct ds;
    byte[] data;

    log("SyxDataStruct(format,data)\n");
    log("- format: %s\n\n",TG55FormatSpecifiers.F_TG55_PARCNG1);
    ds = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_PARCNG1);
    data = new byte[] 
    {
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Dummy bytes
      0x43, 0x15, 0x35, 0x07, 0x00, 0x7E, 0x10, 0x01, // SysEx struct data (no actual meaning for TG55!)
      0x7F                                            // ...
    };
    int start = 8; // Start position of SysEx struct data in byte array
    log("SyxDataStruct.setData(data,start)\n");
    log("- data : %s\n",SYX.prettyPrintByteArray(data," ".repeat(9)));
    log("- start: %d\n",start);
    ds.setData(data,start);
    
    log(0,3,"\n-> SyxDataStruct:\n");
    log(ds.prettyPrint());
    log(0,-3,"\n");

    log("-> Struct parameters:\n");
    log("   - # = %d\n",ds.getMidiValue('#'));
    log("   - s = %d\n",ds.getMidiValue('s'));
    log("   - p = %d\n",ds.getMidiValue('p'));
    log("   - v = %d\n",ds.getMidiValue('v'));
}

  @Test
  @DisplayName(TEST_CREATEWITHVALUES)
  public void testCreateWithValues()
  throws IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_CREATEWITHVALUES);
    logHrule();
    log("\n");

    SyxDataStruct ds;

    log("SyxDataStruct(format,args)\n");
    log("- format: %s\n",TG55FormatSpecifiers.F_TG55_PARCNG1);
    log("- args  : %s\n","'#',0x00,'p',0x05,'v',(int)'>'");
    ds = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_PARCNG1,'#',0x00,'p',0x05,'v',(int)'>');

    log(0,3,"\n-> SyxDataStruct:\n");
    log(ds.prettyPrint());
    log(0,-3,"\n");
    log("-> Struct parameters:\n");
    log("   - # = %d\n",ds.getMidiValue('#'));
    log("   - s = %d\n",ds.getMidiValue('s'));
    log("   - p = %d\n",ds.getMidiValue('p'));
    log("   - v = %d ('%s')\n",ds.getMidiValue('v'),(char)ds.getMidiValue('v'));
  }

  @Test
  @DisplayName(TEST_GETSETSTRINGPARAMS)
  public void testGetSetStringParams()
  throws IllegalArgumentException, InvalidMidiDataException
  {
    System.out.println();
    setLogIndent(0);

    log("%s\n",TEST_GETSETSTRINGPARAMS);
    logHrule();
    log("\n");

    log("SyxDataStruct(format,data)\n");
    log("- format: %s\n\n",TG55FormatSpecifiers.F_TG55_BDMU_HEADER);
    SyxDataStruct ds = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BDMU_HEADER);
    ds.setName("Multi header");
    byte[]data = new byte[] 
    {
      0x4D, 0x69, 0x73, 0x73, 0x20, 0x47, 0x52, 0x20, 0x20, 0x20, 0x00
    };
    log("SyxDataStruct.setData(data)\n");
    log("- data : %s\n",SYX.prettyPrintByteArray(data," ".repeat(9)));
    ds.setData(data);

    log(0,3,"\n-> SyxDataStruct:\n");
    log(ds.prettyPrint());
    log("\n");
    log(ds.prettyPrintData(2,32));
    log(0,-3,"\n");
    
    log("SyxDataStruct.getParamsAsString(\"a-j\")\n");
    String multiName = ds.readString("a-j");
    log("- '%s' (Mutli name)\n\n",multiName);
    
    log("SyxDataStruct.setParamsAsString(\"a-j\",\"TEST NAME \")\n");
    ds.writeString("a-j","TEST NAME ");

    log(0,3,"-> SyxDataStruct:\n");
    log(ds.prettyPrint());
    log("\n");
    log(ds.prettyPrintData(2,32));
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

    //Create temp file
    log("Create a temp file ...\n");
    File tempFile = File.createTempFile("SyxDataStructTest-", ".tmp");
    tempFile.deleteOnExit();
    log("- Name: \"%s\"\n",tempFile.getAbsolutePath());
    log("ok\n\n");
    
    //Crate a SysEx data struct
    log("Create a SyxDataStruct ...\n");
    SyxDataStruct ds1 = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_PARCNG1);
    ds1.setMidiValues('#',0x00,'p',0x05,'v',(int)'>');
    log(ds1.prettyPrint());
    log("ok\n\n");

    // Write SysEx data struct to temp file
    log("Serialize data struct to temp file ... ");
    FileOutputStream fileOutputStream
      = new FileOutputStream(tempFile);
    ObjectOutputStream objectOutputStream 
      = new ObjectOutputStream(fileOutputStream);
    objectOutputStream.writeObject(ds1);
    objectOutputStream.flush();
    objectOutputStream.close();
    log("ok\n\n");
    
    // Read SysEx data struct from temp file
    log("Deserialize data struct from temp file ...\n");
    FileInputStream fileInputStream
      = new FileInputStream(tempFile);
    ObjectInputStream objectInputStream
      = new ObjectInputStream(fileInputStream);
    SyxDataStruct ds2 = (SyxDataStruct)objectInputStream.readObject();
    objectInputStream.close(); 
    log(ds2.prettyPrint());
    log("ok\n\n");
    
    // Compare ds1 and ds2
    log("Compare deserialized data struct with original ... ");
    assertTrue(ds1.equals(ds2));
    log("ok\n");
  }
  
}

// EOF