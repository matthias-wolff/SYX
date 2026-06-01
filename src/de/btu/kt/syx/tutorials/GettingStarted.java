package de.btu.kt.syx.tutorials;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.NotSupportedException;
import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.ISysexMessageListener;
import de.btu.kt.syx.midi.SyxChecksum;
import de.btu.kt.syx.midi.SyxDataStruct;
import de.btu.kt.syx.midi.SyxMessage;
import de.btu.kt.syx.midi.SyxParamInfo;
import de.btu.kt.syx.test.TG55FormatSpecifiers;
import de.btu.kt.syx.util.MidiInterface;

/**
 * Basic coding examples.
 * <hr>
 * <p>
 *   [&lt;prev]
 *   | Back to 
 *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
 *   | [{@linkplain GettingStarted#createMI() next&gt;}]
 * </p>
 * 
 * <h2>Getting Started</h2>
 * <ol>
 *   <li>{@linkplain #createMI() Creating a MIDI Interface}</li>
 *   <li>{@linkplain #sendSysEx() Sending a System Exclusive Message}</li>
 *   <li>{@linkplain #receiveSysEx() Receiving a System Exclusive Message}</li>
 *   <li>{@linkplain #addParamInfo() Adding Parameter Information}</li>
 * </ol>
 */
public class GettingStarted
{
  // -- Prevent for Instantiation ---------------------------------------------

  /**
   * This class cannot be instantiated
   */
  private GettingStarted()
  throws NotSupportedException
  {
    throw new NotSupportedException();
  }

  // -- Tutorials -------------------------------------------------------------

  /**
   * Demonstrates how to create a SYX MIDI interface.
   * <hr>
   * <p>
   *   [{@linkplain GettingStarted &lt;prev}]
   *   | Back to 
   *     [{@linkplain GettingStarted Getting Started}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain GettingStarted#sendSysEx() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:1em">Creating a MIDI Interface</h2>
   * <p>Before sending and receiving MIDI system exclusive messages with SYX you
   * must instantiate and open a MIDI interface:</p>
   * <pre>  final String miName = "KOMPLETE KONTROL EXT - 1"; // &lt;- Name of your MIDI interface here
   *  {@link MidiInterface} mi = null;
   *  try
   *  {
   *    mi = new {@link MidiInterface#MidiInterface(String) MidiInterface}(miName);
   *    mi.{@link MidiInterface#open() open}();
   *
   *    // Use MIDI interface...
   *    {@link MidiDevice.Info} mdiIn  = mi.{@link MidiInterface#getMidiInDevice() getMidiInDevice}().{@link MidiDevice#getDeviceInfo() getDeviceInfo}();
   *    {@link MidiDevice.Info} mdiOut = mi.{@link MidiInterface#getMidiOutDevice() getMidiOutDevice}().{@link MidiDevice#getDeviceInfo() getDeviceInfo}();
   *    System.out.println(MidiInterface.{@link MidiInterface#printMidiDeviceInfo(javax.sound.midi.MidiDevice.Info) printMidiDeviceInfo}(mdiIn));
   *    System.out.println(MidiInterface.{@link MidiInterface#printMidiDeviceInfo(javax.sound.midi.MidiDevice.Info) printMidiDeviceInfo}(mdiOut));
   *  }
   *  catch (Exception e)
   *  {
   *    e.printStackTrace();
   *  }
   *  finally
   *  {
   *    if (mi!=null)
   *      mi.{@link MidiInterface#close() close}();
   *  }</pre>
   * <p>Please make sure to close the MIDI interface in any case! You can list the
   * names of available MIDI interfaces as follows:</p>
   * <pre>  System.out.println(MidiInterface.{@link MidiInterface#printMidiDeviceList() printMidiDeviceList()});</pre>
   */
  public static void createMI()
  {
    final String miName = "KOMPLETE KONTROL EXT - 1"; // <- Name of your MIDI interface here
    MidiInterface mi = null;
    try
    {
      mi = new MidiInterface(miName);
      mi.open();

      // Use MIDI interface...
      MidiDevice.Info mdiIn  = mi.getMidiInDevice().getDeviceInfo();
      MidiDevice.Info mdiOut = mi.getMidiOutDevice().getDeviceInfo();
      System.out.println(MidiInterface.printMidiDeviceInfo(mdiIn));
      System.out.println(MidiInterface.printMidiDeviceInfo(mdiOut));
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
   * Demonstrates how to send a system exclusive message.
   * <hr>
   * <p>
   *   [{@linkplain GettingStarted#createMI() &lt;prev}]
   *   | Back to 
   *     [{@linkplain GettingStarted Getting Started}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain GettingStarted#receiveSysEx() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:1em">Sending a System Exclusive Message</h2>
   * <p>I have a Yamaha TG55 synthesizer. It allows remotely pushing the buttons
   * on its front panel through system exclusive messages. Let's push a button.
   * </p>
   * 
   * <p>We start as in the {@linkplain #createMI() first example} by creating
   * a MIDI interface:</p>
   * <pre>  final String miName = "KOMPLETE KONTROL EXT - 1"; // &lt;- Name of your MIDI interface here
   *  {@link MidiInterface} mi = null;
   *  try
   *  {
   *    mi = new {@link MidiInterface#MidiInterface(String) MidiInterface}(miName);
   *    mi.{@link MidiInterface#open() open}();</pre>
   * 
   * <p>Next we create the push-button message format (see [1], p. Add-15):</p>
   * <pre>    final String msgFormat = "43H 1#H 35H 0DH 00H 00H nnH 00H vvH";</pre>
   *
   * <p>As SysEx messages can be large, SYX subdivides them into parts. The
   * respective class is {@link SyxDataStruct}. For the push-button message, one
   * part suffices:</p>
   * <pre>    {@link SyxDataStruct} prt = new {@link SyxDataStruct#SyxDataStruct(String) SyxDataStruct}(msgFormat);</pre>
   *
   * <p>To push the right button on the right device, we need to initialize the part parameters:</p>
   * <pre>    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('#',0x00); // Device no. 1
   *    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('n',0x03); // [VOICE] button
   *    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('v',0x40); // Switch on</pre>
   *
   * <p>Now we create the system exclusive message:</p>
   * <pre>    {@link SyxMessage} msg = new {@link SyxMessage#SyxMessage(SyxDataStruct...) SyxMessage}(prt);</pre>
   *
   * <p>Let's look what we have got:</p>
   * <pre>    System.out.println(msg.{@link SyxMessage#prettyPrint() prettyPrint}());
   * 
   *    > 0000 F0H (System exclusive)
   *    > Part   0 : 0001 (9 bytes) 
   *    > 0001 43H - 01000011 11111111
   *    > 0002 10H - 0001#### 11110000 : # ( 4 bits) =   0x00     0      (no parameter info)
   *    > 0003 35H - 00110101 11111111
   *    > 0004 0DH - 00001101 11111111
   *    > 0005 00H - 00000000 11111111
   *    > 0006 00H - 00000000 11111111
   *    > 0007 03H - 0nnnnnnn 10000000 : n ( 7 bits) =   0x03     3      (no parameter info)
   *    > 0008 00H - 00000000 11111111
   *    > 0009 40H - 0vvvvvvv 10000000 : v ( 7 bits) =   0x40    64  '@' (no parameter info)
   *    > EOX
   *    > 0010 F7H (System Common - End of system exclusive)
   *    > 11 bytes</pre>
   * 
   * <p>Looks good. We can send the message:</p>
   * <pre>    mi.{@link MidiInterface#send(javax.sound.midi.MidiMessage) send}(msg);</pre>
   *
   * <p>Finally we catch all exceptions and close the MIDI interface:</p>
   * <pre>  }
   *  catch (Exception e)
   *  {
   *    e.printStackTrace();
   *  }
   *  finally
   *  {
   *    if (mi!=null)
   *      mi.{@link MidiInterface#close() close}();
   *  }</pre>
   * 
   * <p><b>Note:</b> You can safely run this example even if you don't have a
   * TG55. The message will be ignored by all other devices.</p>
   * 
   * <p style="margin-bottom:0em"><b>References:</b></p>
   * <table style="margin-top:0em; margin-bottom:0em">
   *   <tr><td>[1]</td><td>
   *     Yamaha Corp.: TG55 Operating Manual. <a href="https://data.yamaha.com/files/download/other_assets/9/316979/TG55G.pdf">Online</a>, retrieved May 12, 2026</td></tr>
   * </table>
   */
  public static void sendSysEx()
  {
    final String miName = "KOMPLETE KONTROL EXT - 1"; // <- Name of your MIDI interface here
    MidiInterface mi = null;
    try
    {
      mi = new MidiInterface(miName);
      mi.open();

      final String msgFormat = "43H 1#H 35H 0DH 00H 00H nnH 00H vvH";
      SyxDataStruct prt = new SyxDataStruct(msgFormat);
      prt.setMidiValue('#',0x00); // Device no. 1
      prt.setMidiValue('n',0x03); // [VOICE] button
      prt.setMidiValue('v',0x40); // Switch on
      SyxMessage msg = new SyxMessage(prt);
      System.out.println(msg.prettyPrint());
      mi.send(msg);
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
   * Demonstrates how to receive a system exclusive message.
   * <hr>
   * <p>
   *   [{@linkplain GettingStarted#sendSysEx() &lt;prev}]
   *   | Back to 
   *     [{@linkplain GettingStarted Getting Started}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain GettingStarted#addParamInfo() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:1em">Receiving a System Exclusive Message</h2>
   * <p>We will trigger an error message at the Yamaha TG55 and receive it. To
   * this end, we send a nonsense data bulk.</p>
   * 
   * <p>As in the previous examples, we start with creating a MIDI interface:
   * </p>
   * <pre>  final int     devNum = 0; // &lt;- Your zero-based device number here
   *  final String  miName = "KOMPLETE KONTROL EXT - 1"; // &lt;- Name of your MIDI interface here
   *  {@link MidiInterface} mi     = null;
   *  try
   *  {
   *    mi = new {@link MidiInterface#MidiInterface(String) MidiInterface}(miName);
   *    mi.{@link MidiInterface#open() open}();</pre>
   * 
   * <p>In order to receive SysEx messages, we must register a SyxEx message
   * listener with the MIDI interface:</p>
   * <pre>    mi.{@link MidiInterface#addSysExListener(ISysexMessageListener) addSysExListener}(new {@link ISysexMessageListener}()
   *    {</pre>
   *
   * <p>A SyxEx message listener must implement two methods. The first method,
   * {@link ISysexMessageListener#receiveSysexMsg(SysexMessage,long)
   * receiveSysexMsg}{@code (}&hellip;{@code )}, is invoked on incoming SyxEx
   * messages. If a message listener does not process a particular SysEx
   * message, it must throw an {@link InvalidMidiDataException}. We will receive
   * (only) TG55 error messages (see [1], pp. Add-16,17), then parse them and
   * print the data content:</p>
   * <pre>      &#64;Override
   *      public void {@link ISysexMessageListener#receiveSysexMsg(SysexMessage,int) receiveSysexMsg}({@link SysexMessage} sxMsg, long timeStamp)
   *      throws {@link InvalidMidiDataException}
   *      {
   *        // Print the raw SyxEx message
   *        System.out.print("SyxEx message received: ");
   *        byte[] syxData = sxMsg.{@link SysexMessage#getMessage() getMessage}();
   *        System.out.println({@link SYX}.{@link SYX#prettyPrintByteArray(byte[], String) prettyPrintByteArray}(syxData,"  "));
   *
   *        > SyxEx message received: F0 43 10 35 7F 00 00 00 00 20 F7 
   *
   *        // Try to parse as TG55 error message
   *        final String errMsgFormat = "43H 1#H 35H 7FH 00H[4] eeH";
   *        {@link SyxMessage} errMsg = new {@link SyxMessage#SyxMessage(String, byte[]) SyxMessage}(errMsgFormat,syxData); // 1)
   *        System.out.println("\nTG55 error message parsed successfully:");
   *        System.out.println(errMsg.{@link SyxMessage#prettyPrint() prettyPrint}());
   *
   *        > TG55 error message parsed successfully:
   *        > 0000 F0H (System exclusive)
   *        > Part   0 : 0001 (9 bytes) 
   *        > 0001 43H - 01000011 11111111
   *        > 0002 10H - 0001#### 11110000 : # ( 4 bits) =   0x00     0      (no parameter info)
   *        > 0003 35H - 00110101 11111111
   *        > 0004 7FH - 01111111 11111111
   *        > 0005 00H - 00000000 11111111
   *        > 0006 00H - 00000000 11111111
   *        > 0007 00H - 00000000 11111111
   *        > 0008 00H - 00000000 11111111
   *        > 0009 20H - 0eeeeeee 10000000 : e ( 7 bits) =   0x20    32  ' ' (no parameter info)
   *        > EOX
   *        > 0010 F7H (System Common - End of system exclusive)
   *        > 11 bytes
   *
   *        // Read and print the error message's parameters
   *        int dn = errMsg.{@link SyxMessage#getPart(int) getPart}(0).{@link SyxDataStruct#getMidiValue(char) getMidiValue}('#');
   *        int en = errMsg.{@link SyxMessage#getPart(int) getPart}(0).{@link SyxDataStruct#getMidiValue(char) getMidiValue}('e');
   *        String sdn = String.format("Device no.: 0x%02X",dn);
   *        String sen = String.format("Error code: 0x%02X",en);
   *        System.out.println("\n"+sdn+"\n"+sen);
   * 
   *        > Device no.: 0x00
   *        > Error code: 0x20
   * 
   *        // 1) Will throw an {@link InvalidMidiDataException} if the message is not a TG55 error message
   *      }</pre>
   * 
   * <p>The second method must return the zero-base device number:</p>
   * <pre>      &#64;Override
   *      public int getDevNum()
   *      {
   *        return devNum;
   *      }
   *    });</pre>
   * 
   * <p>Now we trigger an error at TG55 by sending a nonsense Multi bulk
   * message. A TG55 Multi bulk consists of four parts: two bulk headers, the
   * actual bulk data, and a checksum (see [1], pp. Add-6,8,9).</p>
   * <pre>    // Create nonsense bulk message
   *    // - Part 0: Bulk header 1
   *    {@link SyxDataStruct} prt0 = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER1 F_BD_HEADER1},"Bulk Header 1");
   *    prt0.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('#',devNum); // Device no. 1
   *    prt0.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('b',0x013A); // Payload size (number of bytes)
   * 
   *    // - Part 1: Bulk header 2
   *    {@link SyxDataStruct} prt1 = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER2 F_BD_HEADER2},"Bulk Header 2");
   *    prt1.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('s','M');  // Multi bulk type
   *    prt1.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('t','U');  // Multi bulk type
   *    prt1.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('x',0x7F); // Edit buffer
   *    prt1.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('y',0x00); // Memory number
   * 
   *    // - Part 2: Nonsense Multi data (wrong size and invalid data)
   *    {@link SyxDataStruct} prt2 = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}("00H[10]","Nonsense multi data");
   * 
   *    // - Part 3: Checksum
   *    {@link SyxDataStruct} prt3 = new {@link SyxChecksum#SyxChecksum(int, int, int) SyxChecksum}({@link SyxChecksum}.{@link SyxChecksum#ROLAND ROLAND},1,-1);
   * 
   *    // Create nonsense bulk message
   *    {@link SyxMessage} blkMsg = new {@link SyxMessage#SyxMessage() SyxMessage}();
   *    blkMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(prt0,prt1,prt2,prt3);
   * 
   *    // Send the message (will trigger a "Bulk Canceled" error)
   *    mi.{@link MidiInterface#send(javax.sound.midi.MidiMessage) send}(blkMsg);</pre>
   * 
   * <p>After sending the bogus bulk, we wait a little for the device's reply
   * which will be dispatched to our SysEx message listener:</p>
   * <pre>    Thread.sleep(2000);</pre>
   * 
   * <p>We clear the error message displayed at the device by remotely pressing
   * the [EXIT] button.</p>
   * <pre>    final String msgExitBtnFormat = "43H 1#H 35H 0DH 00H[2] 08H 00H 40H";
   *    {@link SyxDataStruct} prt = new {@link SyxDataStruct#SyxDataStruct(String) SyxDataStruct}(msgExitBtnFormat);
   *    prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('#',devNum);
   *    mi.{@link MidiInterface#send(javax.sound.midi.MidiMessage) send}(new {@link SyxMessage#SyxMessage(SyxDataStruct...) SyxMessage}(prt));</pre>
   * 
   * <p>Finally we catch all exceptions and close the MIDI interface:</p>
   * <pre>  }
   *  catch (Exception e)
   *  {
   *    e.printStackTrace();
   *  }
   *  finally
   *  {
   *    if (mi!=null)
   *      mi.{@link MidiInterface#close() close}();
   *  }</pre>
   * 
   * <p style="margin-bottom:0em"><b>References:</b></p>
   * <table style="margin-top:0em; margin-bottom:0em">
   *   <tr><td>[1]</td><td>
   *     Yamaha Corp.: TG55 Operating Manual. <a href="https://data.yamaha.com/files/download/other_assets/9/316979/TG55G.pdf">Online</a>, retrieved May 12, 2026</td></tr>
   * </table>
   */
  public static void receiveSysEx()
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
          // Print the raw SyxEx message
          System.out.print("SyxEx message received: ");
          byte[] syxData = sxMsg.getMessage();
          System.out.println(SYX.prettyPrintByteArray(syxData,"  "));

          // Try to parse as TG55 error message
          final String errMsgFormat = "43H 1#H 35H 7FH 00H[4] eeH";
          SyxMessage errMsg = new SyxMessage(errMsgFormat,syxData); // 1)
          System.out.println("\nTG55 error message parsed successfully:");
          System.out.println(errMsg.prettyPrint());

          // Read and print the error message's parameters
          int dn = errMsg.getPart(0).getMidiValue('#');
          int en = errMsg.getPart(0).getMidiValue('e');
          String sdn = String.format("Device no.: 0x%02X",dn);
          String sen = String.format("Error code: 0x%02X",en);
          System.out.println("\n"+sdn+"\n"+sen);

         // 1) Will throw an InvalidMidiDataException if the message is not a TG55 error message
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

      // - Part 1: Bulk header
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
   * Demonstrates how to add parameter information.
   * <hr>
   * <p>
   *   [{@linkplain GettingStarted#receiveSysEx() &lt;prev}]
   *   | Back to 
   *     [{@linkplain GettingStarted Getting Started}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [next&gt;]
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

  // == MAIN ==================================================================

  public static void main(String[] args)
  {
    //GettingStarted.createMI();
    //GettingStarted.sendSysEx();
    GettingStarted.receiveSysEx();
    //GettingStarted.addParamInfo();
  }

}

// EOF