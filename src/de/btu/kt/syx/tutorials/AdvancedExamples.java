package de.btu.kt.syx.tutorials;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.NotSupportedException;
import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.ISysexMessageListener;
import de.btu.kt.syx.midi.SyxChecksum;
import de.btu.kt.syx.midi.SyxDataStruct;
import de.btu.kt.syx.midi.SyxMessage;
import de.btu.kt.syx.midi.SyxParamInfo;
import de.btu.kt.syx.test.TG55FormatSpecifiers;
import de.btu.kt.syx.util.ILogger;
import de.btu.kt.syx.util.MidiInterface;
import de.btu.kt.syx.util.SysexRecorder;

/**
 * Coding examples of handling bulk data.
 * <hr>
 * <p>
 *   [{@linkplain de.btu.kt.syx.tutorials &lt;prev}]
 *   | Back to 
 *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
 *   | [{@linkplain AdvancedExamples#addParamInfo() next&gt;}]
 * </p>
 * 
 * <h2>Advanced Examples</h2>
 * <ol>
 *   <li>{@linkplain #addParamInfo() Adding Parameter Information}</li>
 *   <li>{@linkplain #fetchPatchName() Fetching a Patch Name}</li>
 *   <li>{@linkplain #syxFiles() Reading and Writing SysEx Files}</li>
 * </ol>
 * 
 * @author Matthias Wolff
 */
public class AdvancedExamples
{

  // -- Fields ----------------------------------------------------------------

  /**
   * Main thread {@linkplain ILogger logger}. Supplied by class {@link
   * AdvancedExamples} for pretty console print-outs.
   */
  static ILogger mainLog = new ILogger()
  {

    @Override
    public int getVerbose()
    {
      return 0;
    }

    @Override
    public String getLogID()
    {
      return "MAIN";
    }

  };

  /**
   * SysEx message receiver {@linkplain ILogger logger}. Supplied by class
   * {@link AdvancedExamples} for pretty console print-outs.
   */
  static ILogger rcvrLog = new ILogger()
  {

    @Override
    public int getVerbose()
    {
      return 0;
    }

    @Override
    public String getLogID()
    {
      return "SYX_RCVR";
    }

  };

  // -- Prevent for Instantiation ---------------------------------------------

  /**
   * This class cannot be instantiated
   */
  private AdvancedExamples()
  throws NotSupportedException
  {
    throw new NotSupportedException();
  }

  // -- Tutorials -------------------------------------------------------------

  /**
   * Demonstrates how to add parameter information.
   * <hr>
   * <p>
   *   [{@linkplain AdvancedExamples &lt;prev}]
   *   | Back to 
   *     [{@linkplain AdvancedExamples Advanced Examples}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain #fetchPatchName() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:0">Adding Parameter Information</h2>
   * <p>In most cases, the "MIDI value" of a parameter as stored in the SysEx
   * message does not equal the respective "model value" as displayed on the
   * device. There are also cases where not every MIDI value which can
   * potentially be stored in the respective parameter bits of the SysEx message
   * are permissible. For code clarity (and also for parameter validation) it is
   * desirable to work with the model values rather than with the MIDI values.
   * To this end, SYX features {@linkplain SyxParamInfo parameter information}.
   * </p>
   * 
   * <p>We will enhance the {@linkplain GettingStarted#receiveSysEx() last
   * example} by adding parameter information to the received error messages.
   * We use the same Java code and just modify the {@link
   * ISysexMessageListener#receiveSysexMsg(SysexMessage, long)
   * receiveSysexMsg}{@code (}&hellip;{@code )} method of the {@linkplain
   * ISysexMessageListener SysEx message listener}:</p>
   * <pre>  &#64;Override
   *  public void {@link ISysexMessageListener#receiveSysexMsg(SysexMessage, long) receiveSysexMsg}({@link SysexMessage} sxMsg, long timeStamp)
   *  throws {@link InvalidMidiDataException}
   *  {
   *    {@link SyxDataStruct} prt;
   *    {@link SyxParamInfo}  pi;</pre>
   * 
   * <p>If we use parameter information, each parameter in the SysEx message 
   * needs a unique identifier (UID). Therefore, we define such UIDs for the two
   * parameters, device number and error number:</p>
   * <pre>    final String UID_DEVICE_NUM = "DEVICE_NUM";
   *    final String UID_ERROR_MSG  = "ERROR_MSG";</pre>
   * 
   * <p>First, we create one {@linkplain SyxDataStruct message part } for the
   * content of the error message:</p>
   * <pre>    final String errMsgFormat = "43H 1#H 35H 7FH 00H[4] eeH";
   *    prt = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}(errMsgFormat,"Error Information");</pre>
   * 
   * <p>Now we add parameter information to the part. We start with the device
   * number '#'. The MIDI value is in the range
   * [&hairsp;0x00,&hairsp;0x0F&hairsp;]. The model is offset by one and takes
   * values in the range [&hairsp;1,&hairsp;16] The respective parameter
   * information is:</p>
   * <pre>    pi = new {@link SyxParamInfo#SyxParamInfo(SyxDataStruct, char, String, String) SyxParamInfo}(prt,'#',UID_DEVICE_NUM,"Device");
   *    pi.{@link SyxParamInfo#setValueRange(int, int, int) setValueRange}(0x00,0x0F,1);</pre>
   * 
   * <p>Don't forget to add the parameter information to the part:</p>
   * <pre>    prt.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(pi);</pre>
   * 
   * <p>Next we add information for the error number parameter 'e'. It has 7
   * bits, i.e., a potential MIDI value range of
   * [&hairsp;0x00,&hairsp;0x7F&hairsp;]. Actually, its permissible MIDI values
   * are in the range [&hairsp;0x01,&hairsp;0x20&hairsp;] (see [1], pp.
   * Add-16,17). Further, the parameter has plain-text model values. Hence, the
   * parameter information is:</p>
   * <pre>    int[]    errVals = {@link SYX}.{@link SYX#getIntRange(int, int) getIntRange}(0x01,0x20);
   *    String[] errMsgs = new String[]
   *    {
   *      "MIDI Buffer Full" /&#42;0x01&#42;/, "SEQ Buffer Full"  /&#42;0x02&#42;/,
   *      "MIDI Data"        /&#42;0x03&#42;/, "MIDI Check Sum"   /&#42;0x04&#42;/,
   *      "MIDI Device Off"  /&#42;0x05&#42;/, "MIDI Bulk Prot."  /&#42;0x06&#42;/,
   *      "No Data Card"     /&#42;0x07&#42;/, "Data Card Prot."  /&#42;0x08&#42;/,
   *      "Data Format"      /&#42;0x09&#42;/, "Illegal Data"     /&#42;0x0A&#42;/,
   *      "Verify Failed"    /&#42;0x0B&#42;/, "Internal Bat.Lo"  /&#42;0x0C&#42;/,
   *      "Data Card Bat.Lo" /&#42;0x0D&#42;/, "SEQ Memory Full"  /&#42;0x0E&#42;/,
   *      "SEQ Data Empty"   /&#42;0x0F&#42;/, "Now SEQ Running"  /&#42;0x10&#42;/,
   *      "Song Data Exist"  /&#42;0x11&#42;/, "Internal Bat.NG"  /&#42;0x12&#42;/,
   *      "Data Card Bat.NG" /&#42;0x13&#42;/, "ID Mismatch"      /&#42;0x14&#42;/,
   *      "No Wave Card"     /&#42;0x15&#42;/, "Wrong Wave Card"  /&#42;0x16&#42;/,
   *      "Now SEQ Runnning" /&#42;0x17&#42;/, "(not defined)"    /&#42;0x18&#42;/,
   *      "Voice Type"       /&#42;0x19&#42;/, "Song cleared"     /&#42;0x1A&#42;/,
   *      "(not error 0x1B)" /&#42;0x1B&#42;/, "(not error 0x1C)" /&#42;0x1C&#42;/,
   *      "(not error 0x1D)" /&#42;0x1D&#42;/, "Bulk Received"    /&#42;0x1E&#42;/,
   *      "Bulk Receiving"   /&#42;0x1F&#42;/, "Bulk Canceled"    /&#42;0x20&#42;/
   *    };
   *    pi = new {@link SyxParamInfo#SyxParamInfo(SyxDataStruct, char, String, java.util.SequencedMap, String) SyxParamInfo}(prt,'e',UID_ERROR_MSG,"Error Message");
   *    pi.{@link SyxParamInfo#setValueMap(int[], String[]) setValueMap}(errVals,errMsgs);
   *    prt.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(pi);</pre>
   * 
   *    <p>Now we can parse the received SysEx message:</p> 
   *    <pre>    byte[] syxData = sxMsg.{@link SysexMessage#getMessage() getMessage}();
   *    {@link SyxMessage} errMsg = new {@link SyxMessage#SyxMessage(SyxDataStruct...) SyxMessage}(prt);
   *    errMsg.{@link SyxMessage#setMessage(byte[], int) setMessage}(syxData,syxData.length);</pre>
   * 
   *    <p>Let's look what we've got. We print different views on the parsed
   *    SysEx message. First the message itself:</p>
   *    <pre>    System.out.println("Received error message from TG55:");
   *    System.out.println(errMsg.{@link SyxMessage#prettyPrint() prettyPrint}());
   * 
   *    > Received error message from TG55:
   *    > 0000 F0H (System exclusive)
   *    > Part   0 : 0001 'Error Information' (9 bytes) 
   *    > 0001 43H - 01000011 11111111
   *    > 0002 10H - 0001#### 11110000 : # ( 4 bits) =   0x00     0      Device = 1
   *    > 0003 35H - 00110101 11111111
   *    > 0004 7FH - 01111111 11111111
   *    > 0005 00H - 00000000 11111111
   *    > 0006 00H - 00000000 11111111
   *    > 0007 00H - 00000000 11111111
   *    > 0008 00H - 00000000 11111111
   *    > 0009 20H - 0eeeeeee 10000000 : e ( 7 bits) =   0x20    32  ' ' Error Message = Bulk Canceled
   *    > EOX
   *    > 0010 F7H (System Common - End of system exclusive)
   *    > 11 bytes</pre>
   * 
   *    <p>Then we print details of the data part:</p>
   *    <pre>    System.out.println("\nDetails of Part 0: "+prt.{@link SyxDataStruct#prettyPrint() prettyPrint}());
   *    
   *    > Details of Part 0: 'Error Information'
   *    > - Length: 9 bytes
   *    > - Data:   43 10 35 7F 00 00 00 00 20 
   *    >           01000011 00010000 00110101 01111111 00000000 00000000 00000000 00000000 
   *    >           00100000 
   *    > - Format: 01000011 0001#### 00110101 01111111 00000000 00000000 00000000 00000000
   *    >           0eeeeeee 
   *    > - Filter: 11111111 11110000 11111111 11111111 11111111 11111111 11111111 11111111 
   *    >           10000000 
   *    > - Params: # - length  : 4 bits
   *    >               value   : 0x00  0
   *    >               default : 0x00  0
   *    >               UID     : 'DEVICE_NUM'
   *    >               descr   : 'Device'
   *    >               par.chg.: (not assigned)
   *    >               val.rng.: [0,15]
   *    >               val.ofs.: 1
   *    >           e - length  : 7 bits
   *    >               value   : 0x20  32  ' ' = 'Bulk Canceled'
   *    >               default : 0x01  1
   *    >               UID     : 'ERROR_MSG'
   *    >               descr   : 'Error Message'
   *    >               par.chg.: (not assigned)
   *    >               values  : 0x01: 'MIDI Buffer Full'
   *    >                         0x02: 'SEQ Buffer Full'
   *    >                         0x03: 'MIDI Data'
   *    >                         0x04: 'MIDI Check Sum'
   *    >                         0x05: 'MIDI Device Off'
   *    >                         0x06: 'MIDI Bulk Prot.'
   *    >                         0x07: 'No Data Card'
   *    >                         0x08: 'Data Card Prot.'
   *    >                         0x09: 'Data Format'
   *    >                         0x0A: 'Illegal Data'
   *    >                         0x0B: 'Verify Failed'
   *    >                         0x0C: 'Internal Bat.Lo'
   *    >                         0x0D: 'Data Card Bat.Lo'
   *    >                         0x0E: 'SEQ Memory Full'
   *    >                         0x0F: 'SEQ Data Empty'
   *    >                         0x10: 'Now SEQ Running'
   *    >                         0x11: 'Song Data Exist'
   *    >                         0x12: 'Internal Bat.NG'
   *    >                         0x13: 'Data Card Bat.NG'
   *    >                         0x14: 'ID Mismatch'
   *    >                         0x15: 'No Wave Card'
   *    >                         0x16: 'Wrong Wave Card'
   *    >                         0x17: 'Now SEQ Runnning'
   *    >                         0x18: '(not defined)'
   *    >                         0x19: 'Voice Type'
   *    >                         0x1A: 'Song cleared'
   *    >                         0x1B: '(not error 0x1B)'
   *    >                         0x1C: '(not error 0x1C)'
   *    >                         0x1D: '(not error 0x1D)'
   *    >                         0x1E: 'Bulk Received'
   *    >                         0x1F: 'Bulk Receiving'
   *    >                         0x20: 'Bulk Canceled'</pre>
   * 
   *    <p>Finally, we print the message properties along with their values:</p>
   *    <pre>    System.out.println("Message properties:");
   *    String UIDs[] = errMsg.{@link SyxMessage#findParamUIDsSimple(String) findParamUIDsSimple}("*");
   *    for (String UID : UIDs)
   *    {
   *      String mVal = errMsg.{@link SyxMessage#getModelValueAsString(String) getModelValueAsString}(UID);
   *      String s = String.format("%-10s = %s",UID,mVal);
   *      System.out.println(s);
   *    }
   *    
   *    > Message properties:
   *    > DEVICE_NUM = 1
   *    > ERROR_MSG  = Bulk Canceled
   *    
   *  }</pre>
   */
  public static void addParamInfo()
  {
    
    final int     devNum = 0; // <- Your zero-based device number here
    final String  miName = "KOMPLETE KONTROL EXT - 1"; // <- Name of your MIDI interface here
    MidiInterface mi     = null;
    try
    {
      mi = new MidiInterface(miName);
      mi.open();
  
      mi.addSysExListener(new ISysexMessageListener()
      {
  
        @Override
        public void receiveSysexMsg(SysexMessage sxMsg, long timeStamp)
        throws InvalidMidiDataException
        {
          SyxDataStruct prt;
          SyxParamInfo  pi;
  
          // Define Parameter UIDs
          final String UID_DEVICE_NUM = "DEVICE_NUM";
          final String UID_ERROR_MSG  = "ERROR_MSG";
  
          // Create one SysEx message part
          final String errMsgFormat = "43H 1#H 35H 7FH 00H[4] eeH";
          prt = new SyxDataStruct(errMsgFormat,"Error Information");
  
          // Add information for parameter '#' (device number)
          pi = new SyxParamInfo(prt,'#',UID_DEVICE_NUM,"Device");
          pi.setValueRange(0x00,0x0F,1);
          prt.addParamInfo(pi);
  
          // Add information for parameter 'e' (error number)
          int[]    errVals = SYX.getIntRange(0x01,0x20);
          String[] errMsgs = new String[]
          {
            "MIDI Buffer Full" /*0x01*/, "SEQ Buffer Full"  /*0x02*/,
            "MIDI Data"        /*0x03*/, "MIDI Check Sum"   /*0x04*/,
            "MIDI Device Off"  /*0x05*/, "MIDI Bulk Prot."  /*0x06*/,
            "No Data Card"     /*0x07*/, "Data Card Prot."  /*0x08*/,
            "Data Format"      /*0x09*/, "Illegal Data"     /*0x0A*/,
            "Verify Failed"    /*0x0B*/, "Internal Bat.Lo"  /*0x0C*/,
            "Data Card Bat.Lo" /*0x0D*/, "SEQ Memory Full"  /*0x0E*/,
            "SEQ Data Empty"   /*0x0F*/, "Now SEQ Running"  /*0x10*/,
            "Song Data Exist"  /*0x11*/, "Internal Bat.NG"  /*0x12*/,
            "Data Card Bat.NG" /*0x13*/, "ID Mismatch"      /*0x14*/,
            "No Wave Card"     /*0x15*/, "Wrong Wave Card"  /*0x16*/,
            "Now SEQ Runnning" /*0x17*/, "(not defined)"    /*0x18*/,
            "Voice Type"       /*0x19*/, "Song cleared"     /*0x1A*/,
            "(not error 0x1B)" /*0x1B*/, "(not error 0x1C)" /*0x1C*/,
            "(not error 0x1D)" /*0x1D*/, "Bulk Received"    /*0x1E*/,
            "Bulk Receiving"   /*0x1F*/, "Bulk Canceled"    /*0x20*/
          };
          pi = new SyxParamInfo(prt,'e',UID_ERROR_MSG,"Error Message");
          pi.setValueMap(errVals,errMsgs);
          prt.addParamInfo(pi);
  
          // Try to parse as TG55 error message
          byte[] syxData = sxMsg.getMessage();
          SyxMessage errMsg = new SyxMessage(prt);
          errMsg.setMessage(syxData,syxData.length);
  
          // Print different views of the error message
          System.out.println("Received error message from TG55:");
          System.out.println(errMsg.prettyPrint());
  
          System.out.println("\nDetails of Part 0: "+prt.prettyPrint());
  
          System.out.println("Message properties:");
          String UIDs[] = errMsg.findParamUIDsSimple("*");
          for (String UID : UIDs)
          {
            String mVal = errMsg.getModelValueAsString(UID);
            String s = String.format("%-10s = %s",UID,mVal);
            System.out.println(s);
          }
        }
  
        @Override
        public int getDevNum()
        {
          return devNum;
        }
  
      });
  
      // Create nonsense bulk message
      // - Part 0: Bulk header 1
      SyxDataStruct prt0 = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1,"Bulk Header 1");
      prt0.setMidiValue('#',devNum); // Device no. 1
      prt0.setMidiValue('b',0x013A); // Payload size (number of bytes)
  
      // - Part 1: Bulk header 2
      SyxDataStruct prt1 = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER2,"Bulk Header 2");
      prt1.setMidiValue('s','M');  // Multi bulk type
      prt1.setMidiValue('t','U');  // Multi bulk type
      prt1.setMidiValue('x',0x7F); // Edit buffer
      prt1.setMidiValue('y',0x00); // Memory number
  
      // - Part 2: Nonsense Multi data (wrong size and invalid data)
      SyxDataStruct prt2 = new SyxDataStruct("00H[10]","Nonsense multi data");
  
      // - Part 3: Checksum
      SyxDataStruct prt3 = new SyxChecksum(SyxChecksum.ROLAND,1,-1);
  
      // Create nonsense bulk message
      SyxMessage blkMsg = new SyxMessage();
      blkMsg.addParts(prt0,prt1,prt2,prt3);
  
      // Send the message (will trigger a "Bulk Canceled" error)
      mi.send(blkMsg);
  
      // Wait a little for the response
      Thread.sleep(2000);
  
      // Push the [EXIT] button at TG55 (clears the error message)
      final String msgExitBtnFormat = "43H 1#H 35H 0DH 00H[2] 08H 00H 40H";
      SyxDataStruct prt = new SyxDataStruct(msgExitBtnFormat);
      prt.setMidiValue('#',devNum);
      mi.send(new SyxMessage(prt));
  
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (mi!=null)
        mi.close();
    }
  }

  /**
   * Demonstrates how to fetch a patch name from a device.
   * <hr>
   * <p>
   *   [{@linkplain #addParamInfo() &lt;prev}]
   *   | Back to 
   *     [{@linkplain AdvancedExamples Advanced Examples}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain #syxFiles() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:1em">Fetching a Patch Name</h2>
   * <p>We will (synchronously) fetch a patch name from a Yamaha TG55. You may
   * rewrite the example for your own device as an exercise. The device must
   * support patch bulk requests.</p>
   * 
   * <p>We create, open and close the MIDI interface as explained in {@linkplain
   * GettingStarted Getting Started}/{@linkplain GettingStarted#createMI()
   * Creating a MIDI Interface}.</p>
   * <pre>  1  final int     devNum = 0; // &lt;- Your zero-based device number here
   *  2  final String  miName = "KOMPLETE KONTROL EXT - 1"; // &lt;- Name of your MIDI interface here
   *  3  {@link MidiInterface} mi     = null;
   *  4 
   *  5  try
   *  6  {
   *  7    mi = new {@link MidiInterface#MidiInterface(String) MidiInterface}(miName);
   *  8    mi.{@link MidiInterface#open() open}();</pre>
   * 
   * <p>The first step is to create the TG55 voice bulk dump message we want to
   * receive (see [1], pp. Add-6,&hairsp;9-11). Fetching the voice bulk will be
   * synchronized on this object.</p>
   * <pre>  9    final {@link SyxMessage} blkDmp = new {@link SyxMessage#SyxMessage() SyxMessage}();</pre>
   * 
   * <p>Next we register a SysEx message listener with the MIDI interface:</p>
   * <pre> 10    mi.{@link MidiInterface#addSysExListener(ISysexMessageListener) addSysExListener}(new {@link ISysexMessageListener}()
   * 11    {
   * 12 
   * 13      &#64;Override
   * 14      public void {@link ISysexMessageListener#receiveSysexMsg(SysexMessage) receiveSysexMsg}(SysexMessage sxMsg, long timeStamp)
   * 15      throws {@link InvalidMidiDataException}
   * 16      {</pre>
   * 
   * <p>Upon receiving a SysEx message, we first just print the raw data:</p>
   * <pre> 17        {@link #rcvrLog}.{@link ILogger#log(int, int, String, Object...) log}(0,2,"SyxEx message received:\n");
   * 18        byte[] syxData = sxMsg.{@link SysexMessage#getMessage() getMessage}();
   * 19        {@link #rcvrLog}.{@link ILogger#log(String, Object...) log}({@link SYX}.{@link SYX#prettyPrintByteArray(byte[], String) prettyPrintByteArray}(syxData,""));
   * 20        {@link #rcvrLog}.{@link ILogger#log(int, int, String, Object...) log}(0,-2,"\n\n");</pre>
   * <p>({@link #rcvrLog} is an interface to SYX's  {@linkplain ILogger global
   * message logger} supplied by class {@link AdvancedExamples} to ease console
   * print-outs).</p>
   * 
   * <p>Now we detect whether the received SysEx message is a TG55 bulk dump (of
   * any type). To this end, we initially only parse the message header. If this
   * fails, an {@link InvalidMidiDataException} will be thrown indicating to the
   * {@linkplain MidiInterface SysEx message listener framework} that our
   * receiver does not understand and process the received message. This
   * preliminary step is necessary because TG55 voice bulk messages, which we
   * want to receive, contain a variable number of payload bytes depending on
   * the voice settings. We need the payload length in preparation for parsing
   * the voice bulk.</p>
   * <pre> 21        try
   * 22        {
   * 23          {@link SyxMessage} rcvMsg = new {@link SyxMessage#SyxMessage() SyxMessage}();
   * 24          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER1 F_BD_HEADER1},"Bulk Header 1"));
   * 25          rcvMsg.{@link SyxMessage#setMessage(byte[], int) setMessage}(sxMsg.{@link SysexMessage getMessage}(),rcvMsg.{@link SyxMessage#getLength() getLength}()/&#42;Header bytes only!&#42;/);
   * 26          {@link #rcvrLog}.{@link ILogger#log(int, int, String, Object...) log}(0,2,"TG55 bulk header 1 parsed successfully:\n");
   * 27          {@link #rcvrLog}.{@link ILogger#log(String, Object...) log}(rcvMsg.{@link SyxMessage#prettyPrint() prettyPrint}());
   * 28          {@link #rcvrLog}.{@link ILogger#log(int, int, String, Object...) log}(0,-2,"\n\n");</pre>
   * 
   * <p>The payload size is contained in parameter 'b' of the bulk header (see
   * {@linkplain TG55#F_BD_HEADER1 format specification} and [1] p. Add-6):</p>
   * <pre> 29          int bc = rcvMsg.{@link SyxMessage#getPart(int) getPart}(0).{@link SyxDataStruct#getMidiValue(char) getMidiValue}('b');</pre>
   * 
   * <p>Now we're all set to actually parse the received voice bulk:</p>
   * <pre> 30          rcvMsg.{@link SyxMessage#clear() clear}();
   * 31          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER1 F_BD_HEADER1} ,"Bulk Header 1"));
   * 32          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER2 F_BD_HEADER2} ,"Bulk Header 2"));
   * 33          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BDVC_HEADER F_BDVC_HEADER},"Voice Header" ));
   * 34          bc -= (rcvMsg.{@link SyxMessage#getPart(int) getPart}(1).{@link SyxDataStruct#getLength() getLength}() + rcvMsg.{@link SyxMessage#getPart(int) getPart}(2).{@link SyxDataStruct#getLength() getLength}());
   * 35          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}("**H ".repeat(bc) ,"(Other Data)"));
   * 36          rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(new {@link SyxChecksum#SyxChecksum(int, int, int) SyxChecksum}({@link SyxChecksum SyxChecksum}.{@link SyxChecksum#ROLAND ROLAND},1,-1));
   * 37          rcvMsg.{@link SyxMessage#setMessage(byte[], int) setMessage}(sxMsg.{@link SysexMessage#getMessage() getMessage}(),rcvMsg.{@link SyxMessage#getLength() getLength}());</pre>
   * <p>In lines 31&ndash;36 we have created the entire voice bulk message
   * structure. The part added in line 35 is a dummy just accepting all bytes of
   * the voice bulk except the voice header (see line 33 and [1] p. Add-9) which
   * contains the patch name. Only the size of the dummy part must be correct.
   * Hence, we used the playload size obtained in line 29 to make its
   * {@linkplain SyxDataStruct format specifier} (lines 34, 35). In line 37 we
   * parsed the received message. If this fails, an {@link
   * InvalidMidiDataException} will be thrown, again indicating to the framework
   * that our receiver does not understand and process this message.</p>
   * 
   * <p>So far, so good. In a last step, we have to check the device number, 
   * to make sure we got the correct bulk type, and to validate the checksum:
   * </p>
   * <pre> 38          int dn = rcvMsg.{@link SyxMessage#getPart(int) getPart}(0).{@link SyxDataStruct#getMidiValue(char) getMidiValue}('#');
   * 39          if (dn!=devNum)
   * 40            throw {@link SYX}.{@link SYX#InvMdataExc(String, Object...) InvMdataExc}("Wrong devive no. %d; must be %d",dn,devNum);
   * 41          String bid = rcvMsg.{@link SyxMessage#getPart(int) getPart}(1).{@link SyxDataStruct#readString(String) readString}("st");
   * 42          if (!"VC".equals(bid))
   * 43            throw {@link SYX}.{@link SYX#InvMdataExc(String, Object...) InvMdataExc}("Bad bulk ID '%s'; must be 'VC'",bid);
   * 44          rcvMsg.{@link SyxMessage#validateChecksum() validateChecksum}();
   * 45          {@link #rcvrLog}.{@link ILogger#log(String, Object...) log}("TG55 voice bulk dump parsed successfully\n");</pre>
   * 
   * <p>Finally, we synchronize on {@code blkDmp}, copy the parsed message into
   * it, and notify the main thread of having successfully received a TG55 voice
   * bulk:</p>
   * <pre> 46          {@link #rcvrLog}.{@link ILogger#log(String, Object...) log}("-&gt; Notifying MAIN\n\n");
   * 47          synchronized (blkDmp)
   * 48          {
   * 49            blkDmp.{@link SyxMessage#copy(SyxMessage) copy}(rcvMsg);
   * 50            blkDmp.notifyAll();
   * 51          }
   * 52        }</pre>
   * 
   * <p>The {@code try}-{@code catch} block starting in line 21 is not essential
   * to the function. It was just inserted to print any {@code
   * InvalidMidiDataException}s that might have occurred. After that, we just
   * re-throw the exception.</p>
   * <pre> 53        catch (InvalidMidiDataException e)
   * 54        {
   * 55          rcvrLog.errlog("Not a TG55 voice bulk (%s)",e.getMessage());
   * 56          throw e;
   * 57        }
   * 58      }
   * 59
   * 60      &#64;Override
   * 61      public int {@link ISysexMessageListener#getDevNum() getDevNum}()
   * 62      {
   * 63        return devNum;
   * 64      }
   * 65
   * 66    });</pre>
   * 
   * <p>Back in the main thread, we create a TG55 voice bulk request message:
   * </p>
   * <pre> 67    {@link SyxDataStruct} prt = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BULK_REQUEST F_BULK_REQUEST});
   * 68    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('#',devNum); // Device no.
   * 69    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('s','V'   ); // Voice bulk type
   * 70    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('t','C'   ); // Voice bulk type
   * 71    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('x',0x00  ); // Patch I01 (Bank no.)
   * 72    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('y',0x00  ); // Patch I01 (Patch no.)
   * 73    {@link SyxMessage} blkReq = new {@link SyxMessage#SyxMessage() SyxMessage}(prt);</pre>
   * 
   * <p>Then we clear the expected voice bulk dump, synchronize on it, send the
   * bulk request, and wait for the bulk dump to arrive:</p>
   * <pre> 74    long then = System.nanoTime();
   * 75    blkDmp.{@link SyxMessage#clear() clear}();
   * 76    synchronized (blkDmp)
   * 77    {
   * 78      {@link #mainLog}.{@link ILogger#log(String, Object...) log}("Requesting TG55 voice bulk dump (Patch I01)\n");
   * 79      mi.{@link MidiInterface#send(javax.sound.midi.MidiMessage) send}(blkReq);
   * 80      try
   * 81      {
   * 82        {@link #mainLog}.{@link ILogger#log(String, Object...) log}("Waiting for SYX_RCVR\n\n");
   * 83        blkDmp.wait(2000);
   * 84      }
   * 85      catch (InterruptedException e)
   * 86      { // Ignore
   * 87      }
   * 88    }</pre>
   * <p>({@link #mainLog} is an interface to SYX's  {@linkplain ILogger global
   * message logger} supplied by class {@link AdvancedExamples} to ease console
   * print-outs).</p>
   * 
   * <p>If we received a voice bulk dump in reply to the request, {@code blkDmp}
   * will be non-empty. In this case, we read-out and print the patch name. If
   * {@code blkDmp} is empty, the request timed out.</p>
   * <pre> 89    if (!blkDmp.{@link SyxMessage#isEmpty() isEmpty}())
   * 90    {
   * 91      {@link SyxDataStruct} vceHdr = blkDmp.{@link SyxMessage#getPart(int) getPart}(2);
   * 92      String patchName = vceHdr.{@link SyxDataStruct#readString(String) readString}("a-j");
   * 93      {@link #mainLog}.{@link ILogger#log(String, Object...) log}("Patch I01 - '%s'\n",patchName);
   * 94      long now = System.nanoTime();
   * 95      {@link #mainLog}.{@link ILogger#log(String, Object...) log}("(Turnaround time: %.0f ms)\n",(now-then)/1000000.);
   * 96    }
   * 97    else
   * 98      {@link #mainLog}.{@link ILogger#errlog(String, Object...) errlog}("TIME OUT!\n");
   * 99   }
   *100  catch (Exception e)
   *101  {
   *102    e.printStackTrace();
   *103  }
   *104  finally
   *105  {
   *106    if (mi!=null)
   *107      mi.{@link MidiInterface#close() close}();
   *108  }</pre>
   * 
   * <p style="margin-bottom:0em"><b>References:</b></p>
   * <table style="margin-top:0em; margin-bottom:0em">
   *   <tr><td>[1]</td><td>
   *     Yamaha Corp.: TG55 Operating Manual. <a href="https://data.yamaha.com/files/download/other_assets/9/316979/TG55G.pdf">Online</a>, retrieved May 12, 2026</td></tr>
   * </table>
   */
  public static void fetchPatchName()
  {
    final int     devNum = 0; // <- Your zero-based device number here
    final String  miName = "KOMPLETE KONTROL EXT - 1"; // <- Name of your MIDI interface here
    MidiInterface mi     = null;

    try
    {
      mi = new MidiInterface(miName);
      mi.open();

      // The bulk dump message to be received
      // - Bulk fetch is synchronized on this object
      final SyxMessage blkDmp = new SyxMessage();

      mi.addSysExListener(new ISysexMessageListener()
      {

        @Override
        public void receiveSysexMsg(SysexMessage sxMsg, long timeStamp)
        throws InvalidMidiDataException
        {
          // Print the raw SyxEx message
          rcvrLog.log(0,2,"SyxEx message received:\n");
          byte[] syxData = sxMsg.getMessage();
          rcvrLog.log(SYX.prettyPrintByteArray(syxData,""));
          rcvrLog.log(0,-2,"\n\n");

          // Try to parse as TG55 bulk message header
          // - Throws an InvalidMidiDataException if no TG55 bulk dump
          try
          {
            SyxMessage rcvMsg = new SyxMessage();
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1,"Bulk Header 1"));
            rcvMsg.setMessage(sxMsg.getMessage(),rcvMsg.getLength()/*Header bytes only!*/);
            rcvrLog.log(0,2,"TG55 bulk header 1 parsed successfully:\n");
            rcvrLog.log(rcvMsg.prettyPrint());
            rcvrLog.log(0,-2,"\n\n");
            int bc = rcvMsg.getPart(0).getMidiValue('b');

            // Parse voice bulk dump
            // - Throws an InvalidMidiDataException if no TG55 voice bulk dump
            rcvMsg.clear();
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1 ,"Bulk Header 1"));
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER2 ,"Bulk Header 2"));
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BDVC_HEADER,"Voice Header" ));
            bc -= (rcvMsg.getPart(1).getLength() + rcvMsg.getPart(2).getLength());
            rcvMsg.addParts(new SyxDataStruct("**H ".repeat(bc) ,"(Other Data)"));
            rcvMsg.addParts(new SyxChecksum(SyxChecksum.ROLAND,1,-1));
            rcvMsg.setMessage(sxMsg.getMessage(),rcvMsg.getLength());

            // Check voice bulk dump
            int dn = rcvMsg.getPart(0).getMidiValue('#');
            if (dn!=devNum)
              throw SYX.InvMdataExc("Wrong devive no. %d; must be %d",dn,devNum);
            String bid = rcvMsg.getPart(1).readString("st");
            if (!"VC".equals(bid))
              throw SYX.InvMdataExc("Bad bulk ID '%s'; must be 'VC'",bid);
            rcvMsg.validateChecksum();
            rcvrLog.log("TG55 voice bulk dump parsed successfully\n");

            // Notify main thread
            rcvrLog.log("-> Notifying MAIN\n\n");
            synchronized (blkDmp)
            {
              blkDmp.copy(rcvMsg);
              blkDmp.notifyAll();
            }
          }
          catch (InvalidMidiDataException e)
          {
            rcvrLog.errlog("Not a TG55 voice bulk (%s)",e.getMessage());
            throw e;
          }
        }

        @Override
        public int getDevNum()
        {
          return devNum;
        }

      });

      // Create bulk request message
      SyxDataStruct prt = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BULK_REQUEST);
      prt.setMidiValue('#',devNum); // Device no.
      prt.setMidiValue('s','V'   ); // Voice bulk type
      prt.setMidiValue('t','C'   ); // Voice bulk type
      prt.setMidiValue('x',0x00  ); // Patch I01 (Bank no.)
      prt.setMidiValue('y',0x00  ); // Patch I01 (Patch no.)
      SyxMessage blkReq = new SyxMessage(prt);

      // Send bulk request and wait for bulk dump
      long then = System.nanoTime();
      blkDmp.clear();
      synchronized (blkDmp)
      {
        mainLog.log("Requesting TG55 voice bulk dump (Patch I01)\n");
        mi.send(blkReq);
        try
        {
          mainLog.log("Waiting for SYX_RCVR\n\n");
          blkDmp.wait(2000);
        }
        catch (InterruptedException e)
        { // Ignore
        }
      }

      if (!blkDmp.isEmpty())
      {
        // Get and print patch name
        SyxDataStruct vceHdr = blkDmp.getPart(2);
        String patchName = vceHdr.readString("a-j");
        mainLog.log("Patch I01 - '%s'\n",patchName);
        long now = System.nanoTime();
        mainLog.log("(Turnaround time: %.0f ms)\n",(now-then)/1000000.);
      }
      else
        mainLog.errlog("TIME OUT!\n");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (mi!=null)
        mi.close();
    }
  }

  /**
   * Demonstrates reading and writing SYX files.
   * <hr>
   * <p>
   *   [{@linkplain #fetchPatchName() &lt;prev}]
   *   | Back to 
   *     [{@linkplain AdvancedExamples Advanced Examples}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [next&gt;]
   *   
   * <h2 style="margin-top:1em">Reading and Writing SysEx Files</h2>
   * <p>SYX provides a {@linkplain SysexRecorder SysEx recoder} which contains
   * a "tape" whose "tracks" are {@linkplain SysexMessage SysEx messages}. The
   * tape can be recorded, played back, and written to or read from a SYX or
   * TXT file.</p>
   * 
   * <p><b>Note</b>: This example requires the <a
   * href="https://github.com/matthias-wolff/SYX.Devices">SYX.Devices</a>
   * package!</p>
   * 
   * <p>Let's record a little SysEx tape:</p>
   * <pre> try
   * {
   *   {@link SysexRecorder} recorder = new {@link SysexRecorder#SysexRecorder() SysexRecorder}();
   *   recorder.{@link SysexRecorder#record(SysexMessage) record}({@link VZ1PatchAppc}.{@link VZ1PatchAppc#vz1Patch_Karamoon() vz1Patch_Karamoon}());
   *   recorder.{@link SysexRecorder#record(SysexMessage) record}({@link VZ1PatchAppc}.{@link VZ1PatchAppc#vz1Patch_NothingToFear() vz1Patch_NothingToFear}());</pre>
   * <p>The tape now contains two demo patches of the Casio VZ-1/10M synthesizer.</p>
   * 
   * <p>We save the patches into a temporary SYX file:</p>
   * <pre>   File tmpFile = File.createTempFile("syxtape",".syx");
   *   tmpFile.deleteOnExit();
   *   String fileName = tmpFile.getAbsolutePath();
   *   recorder.{@link SysexRecorder#writeSyxFile(String) writeSyxFile}(fileName);</pre>
   * 
   * <p>Then we clear the tape and reload it from the SYX file:</p>
   * <pre>   recorder.{@link SysexRecorder#reset() reset}();
   *   recorder.{@link SysexRecorder#readSyxFile(String) readSyxFile}(fileName);</pre>
   * 
   * <p>Now we play the reloaded tape back to a {@linkplain
   * ISysexMessageListener SysEx message listener} which we attach to the
   * recorder:</p>
   * <pre>   recorder.{@link SysexRecorder#addSysExListener(ISysexMessageListener) addSysExListener}(new {@link ISysexMessageListener}()
   *   {
   *
   *     &#64;Override
   *     public void {@link ISysexMessageListener#receiveSysexMsg(SysexMessage) receiveSysexMsg}(SysexMessage sxMsg, long timeStamp)
   *     throws {@link InvalidMidiDataException}
   *     {
   *       {@link VZ1Patch} vz1Patch = new {@link VZ1Patch#VZ1Patch(byte[]) VZ1Patch}(sxMsg.{@link SysexMessage#getData() getData}());
   *       System.out.println(vz1Patch.{@link VZ1Patch#prettyPrintModel() prettyPrintModel}());
   *     }
   *
   *     &#64;Override
   *     public int {@link ISysexMessageListener#getDevNum() getDevNum}()
   *     { // Not needed
   *       return 0;
   *     }
   *
   *   });
   *   recorder.{@link SysexRecorder#play() play}();
   * }
   * catch (Exception e)
   * {
   *   e.printStackTrace();
   * }</pre>
   */
  public static void syxFiles()
  {
//    try
//    {
//      // Record a SysEx tape
//      SysexRecorder recorder = new SysexRecorder();
//      recorder.record(VZ1PatchAppc.vz1Patch_Karamoon());
//      recorder.record(VZ1PatchAppc.vz1Patch_NothingToFear());
//
//      // Write tape to a temporary SYX file
//      File tmpFile = File.createTempFile("syxtape",".syx");
//      tmpFile.deleteOnExit();
//      String fileName = tmpFile.getAbsolutePath();
//      recorder.writeSyxFile(fileName);
//
//      // Re-read the tape from the SYX file
//      recorder.reset();
//      recorder.readSyxFile(fileName);
//
//      // Play the tape back
//      recorder.addSysExListener(new ISysexMessageListener()
//      {
//
//        @Override
//        public void receiveSysexMsg(SysexMessage sxMsg, long timeStamp)
//        throws InvalidMidiDataException
//        {
//          VZ1Patch vz1Patch = new VZ1Patch(sxMsg.getData());
//          System.out.println(vz1Patch.prettyPrintModel());
//        }
//
//        @Override
//        public int getDevNum()
//        { // Not needed
//          return 0;
//        }
//
//      });
//      recorder.play();
//    }
//    catch (Exception e)
//    {
//      e.printStackTrace();
//    }
  }

  // == MAIN ==================================================================

  public static void main(String[] args)
  {
    //HandlingBulkData.fetchPatchName();
    //HandlingBulkData.createSimplePatchBank();
    AdvancedExamples.syxFiles();
  }

}

// EOF