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
import de.btu.kt.syx.test.TG55FormatSpecifiers;
import de.btu.kt.syx.util.MidiInterface;

/**
 * Basic coding examples.
 * <hr>
 * <p>
 *   [{@linkplain de.btu.kt.syx.tutorials &lt;prev}]
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
   *   | [next&gt;]
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