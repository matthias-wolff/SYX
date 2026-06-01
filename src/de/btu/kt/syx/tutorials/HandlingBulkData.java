package de.btu.kt.syx.tutorials;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.NotSupportedException;
import de.btu.kt.syx.SYX;
import de.btu.kt.syx.devices.BankTree;
import de.btu.kt.syx.devices.IPatch;
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
 *   [&lt;prev]
 *   | Back to 
 *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
 *   | [{@linkplain HandlingBulkData#fetchPatchName() next&gt;}]
 * </p>
 * 
 * <h2>Getting Started</h2>
 * <ol>
 *   <li>{@linkplain #fetchPatchName() Fetching a Patch Name}</li>
 *   <li>{@linkplain #createSimplePatchBank() Creating a Simple Patch Bank}</li>
 *   <li>{@linkplain #syxFiles() Reading and Writing SysEx Files}</li>
 * </ol>
 * 
 * @author Matthias Wolff
 */
public class HandlingBulkData
{

  // -- Fields ----------------------------------------------------------------

  /**
   * Main thread {@linkplain ILogger logger}. Supplied by class {@link
   * HandlingBulkData} for pretty console print-outs.
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
   * {@link HandlingBulkData} for pretty console print-outs.
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
  private HandlingBulkData()
  throws NotSupportedException
  {
    throw new NotSupportedException();
  }

  // -- Tutorials -------------------------------------------------------------

  /**
   * Demonstrates how to fetch a patch name from a device.
   * <hr>
   * <p>
   *   [{@linkplain HandlingBulkData &lt;prev}]
   *   | Back to 
   *     [{@linkplain HandlingBulkData Handling Bulk Data}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain HandlingBulkData#createSimplePatchBank() next&gt;}]
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
   * message logger} supplied by class {@link HandlingBulkData} to ease console
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
   * message logger} supplied by class {@link HandlingBulkData} to ease console
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
   * Demonstrates how to create simple a patch bank.
   * <hr>
   * <p>
   *   [{@linkplain HandlingBulkData#fetchPatchName() &lt;prev}]
   *   | Back to 
   *     [{@linkplain HandlingBulkData Handling Bulk Data}]
   *     [{@linkplain de.btu.kt.syx.tutorials Tutorials}]
   *   | [{@linkplain HandlingBulkData#syxFiles() next&gt;}]
   * </p>
   * 
   * <h2 style="margin-top:1em">Creating a Simple Patch Bank</h2>
   * <p>We extend the {@linkplain #fetchPatchName() previous example} by
   * fetching <em>all</em> patches from TG55 and collecting them in a
   * {@linkplain BankTree patch bank}. As most of the Java code is identical, we
   * will only look at the modifications:</p>
   * <ol>
   *   <li style="margin-bottom:1em">We remove the lengthy console print-outs.</p>
   *   <li>Before line 5 in the {@linkplain #fetchPatchName() previous example},
   *     we create value maps for the 'x' and 'y' parameters of {@linkplain
   *     TG55#F_BD_HEADER2 bulk header #2} (see [1], p. Add-6):
   *     <pre> final int[]    bankNums  = new int[] {0x00, 0x02};
   * final String[] bankIDs   = new String[] {"I","P"};
   * final int[]    patchNums = {@link SYX}.{@link SYX#getIntRange(int, int) getIntRange}(0x00,0x3F);
   * final String[] patchIDs  = new String[0x40];
   * for (int i=0x00; i&lt;=0x3F; i++)
   *   patchIDs[i] = String.format("%02d",i+1);</pre></li>
   *   <li style="margin-bottom:1em">At the same location we create a patch bank object:
   *     <pre> {@link BankTree}&lt;{@link BankTree.IBankObject}&gt; patchBank
   *   = new {@link BankTree#BankTree(String, String) BankTree}&lt;{@link BankTree.IBankObject}&gt;("Patch","Patch Bank");
   * patchBank.{@link BankTree#addBank(String, String) addBank}(bankIDs[0],"Internal");
   * patchBank.{@link BankTree#addBank(String, String) addBank}(bankIDs[1],"Preset");</pre>
   *    In this example we create a simple patch bank which will only contain
   *    the patch names. Patch banks can also contain {@linkplain IPatch patch
   *    data} (see {@link VZ1PatchBankAppc} for an advanced example).</li>
   *   <li style="margin-bottom:1em">We replace line 32 in the {@linkplain 
   *     ISysexMessageListener#receiveSysexMsg(SysexMessage) SysEx message
   *     listener} of the {@linkplain #fetchPatchName() previous example} by:
   *     <pre> {@link SyxDataStruct} blkHdr2;
   * {@link SyxParamInfo} pi;
   * blkHdr2 = new {@link SyxDataStruct#SyxDataStruct(String, String) SyxDataStruct}({@link TG55}.{@link TG55#F_BD_HEADER2 F_BD_HEADER2},"Bulk Header 2");
   * pi = new {@link SyxParamInfo#SyxParamInfo(SyxDataStruct, char, String, String) SyxParamInfo}(blkHdr2,'x',"BANK_ID","Bank ID");
   * pi.{@link SyxParamInfo#setValueMap(int[], String[]) setValueMap}(bankNums,bankIDs);
   * blkHdr2.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(pi);
   * pi = new {@link SyxParamInfo#SyxParamInfo(SyxDataStruct, char, String, String) SyxParamInfo}(blkHdr2,'y',"PATCH_ID","Patch ID");
   * pi.{@link SyxParamInfo#setValueMap(int[], String[]) setValueMap}(patchNums,patchIDs);
   * blkHdr2.{@link SyxDataStruct#addParamInfo(SyxParamInfo) addParamInfo}(pi);
   * rcvMsg.{@link SyxMessage#addParts(SyxDataStruct...) addParts}(blkHdr2);</pre>
   *     which assigns the value maps created in 2. to the parameters 'x' and
   *     'y' in the second bulk header of the received bulk dump (see {@linkplain
   *     GettingStarted Getting Started}/{@linkplain GettingStarted#addParamInfo()
   *     Adding Parameter Information}). This allows us to retrieve plain-text
   *     bank and patch IDs (as shown on the synthesizer) from the voice bulks
   *     we are going to fetch.</li>
   *   <li>We replace lines 67&ndash;99 in the {@linkplain #fetchPatchName()
   *     previous example} by nested loops over the bank patch numbers from 2.
   *     <pre> {@link #mainLog}.{@link ILogger#log(int, int, String, Object...) log}(0,2,"Fetching patch names");
   * for (int bankNum : bankNums)
   *   for (int patchNum : patchNums)
   *   {</pre>
   *      Create voice bulk request for bank {@code bankNum} and patch {@code
   *      patchNum}:
   *    <pre>     {@link SyxDataStruct} prt = new {@link SyxDataStruct#SyxDataStruct(String) SyxDataStruct}(TG55FormatSpecifiers.F_TG55_BULK_REQUEST);
   *     prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('#',devNum  ); // Device no.
   *     prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('s','V'     ); // Voice bulk type
   *     prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('t','C'     ); // Voice bulk type
   *     prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('x',bankNum ); // Bank no.
   *     prt.{@link SyxDataStruct#setMidiValue(char, int) setMidiValue}('y',patchNum); // Patch no.
   *    {@link SyxMessage} blkReq = new {@link SyxMessage#SyxMessage(SyxDataStruct...) SyxMessage}(prt);</pre>
   *      Send bulk request and wait for voice bulk dump:
   *    <pre>     blkDmp.{@link SyxMessage#clear() clear}();
   *     synchronized (blkDmp)
   *     {
   *       mi.{@link MidiInterface#send(javax.sound.midi.MidiMessage) send}(blkReq);
   *       try
   *       {
   *         blkDmp.wait(2000);
   *       }
   *       catch (InterruptedException e)
   *       { // Ignore
   *       }
   *     }</pre>
   *      If we received a bulk dump, read the bank and patch IDs, and the patch
   *      name. Then create a patch bank entry:
   *     <pre>
   *     if (!blkDmp.{@link SyxMessage#isEmpty() isEmpty}())
   *     {
   *       if (patchNum==0)
   *         {@link #mainLog}.{@link ILogger#log(String, Object...) log}("\n");
   *       {@link #mainLog}.{@link ILogger#log(String, Object...) log}(".");
   * 
   *       // Get and print patch name
   *       {@link SyxDataStruct} blkHdr2 = blkDmp.{@link SyxMessage#getPart(int) getPart}(1);
   *       {@link SyxDataStruct} vceHdr  = blkDmp.{@link SyxMessage#getPart(int) getPart}(2);
   *       String bankID    = blkHdr2.{@link SyxDataStruct#getModelValueAsString(char) getModelValueAsString}('x');
   *       String patchID   = blkHdr2.{@link SyxDataStruct#getModelValueAsString(char) getModelValueAsString}('y');
   *       String patchName = vceHdr.{@link SyxDataStruct#readString(String) readString}("a-j");
   *       patchBank.{@link BankTree#addBankObject(String, String, String) addBankObject}(bankID,patchID,patchName);
   *     }
   *     else
   *       {@link #mainLog}.{@link ILogger#errlog(String, Object...) errlog}(".");
   *   }
   *   
   * {@link #mainLog}.{@link ILogger#log(int, int, String, Object...) log}(0,-2,"\n");
   * {@link #mainLog}.{@link ILogger#log(String, Object...) log}("Done\n\n");
   *
   * > MAIN    : Fetching patch names
   * > MAIN    :   ................................................................
   * > MAIN    :   ................................................................
   * > MAIN    : Done</pre>
   *      Finally, we pretty-print the patch bank:
   *     <pre> {@link #mainLog}.{@link ILogger#log(String, Object...) log}(patchBank.{@link BankTree#prettyPrint() prettyPrint}());</pre>
   *      On my TG55 it looks as follows:
   * <pre> > MAIN    : ------------------------------
   * > MAIN    : Patch Bank      UID Name         
   * > MAIN    : ------------------------------
   * > MAIN    : Bank Tree     :     Patch Bank
   * > MAIN    : |- Patch Bank : I   Internal
   * > MAIN    : |- |- Patch   : 01  ZenAirBell
   * > MAIN    : |- |- Patch   : 02  GenChorus 
   * > MAIN    : |- |- Patch   : 03  GB-Chorus 
   * > MAIN    : |- |- Patch   : 04  SP.PanPipe
   * > MAIN    : |- |- Patch   : 05  INIT VOICE
   * > MAIN    : |- |- Patch   : 06  SP:DreamPd
   * > MAIN    : |- |- Patch   : 07  SC.Dazling
   * > MAIN    : |- |- Patch   : 08  SP:Lonely 
   * > MAIN    : |- |- Patch   : 09  SP*Synfony
   * > MAIN    : |- |- Patch   : 10  SE:Fredy  
   * > MAIN    : |- |- Patch   : 11  ME*SynWave
   * > MAIN    : |- |- Patch   : 12  ~ A Bow  
   * > MAIN    : |- |- Patch   : 13  ClassPiano
   * > MAIN    : |- |- Patch   : 14  Rock Piano
   * > MAIN    : |- |- Patch   : 15  SE:"Hit!!"
   * > MAIN    : |- |- Patch   : 16  ST:StrDust
   * > MAIN    : |- |- Patch   : 17  GX Dream  
   * > MAIN    : |- |- Patch   : 18  HardAtckGX
   * > MAIN    : |- |- Patch   : 19  Deep Organ
   * > MAIN    : |- |- Patch   : 20  Warm Organ
   * > MAIN    : |- |- Patch   : 21  Trumpet   
   * > MAIN    : |- |- Patch   : 22  BA:Rezo   
   * > MAIN    : |- |- Patch   : 23  Big Band  
   * > MAIN    : |- |- Patch   : 24  Orch Brass
   * > MAIN    : |- |- Patch   : 25  SynthBrass
   * > MAIN    : |- |- Patch   : 26  Flute     
   * > MAIN    : |- |- Patch   : 27  Saxophone 
   * > MAIN    : |- |- Patch   : 28  FolkGuitar
   * > MAIN    : |- |- Patch   : 29  12 String 
   * > MAIN    : |- |- Patch   : 30  MuteGuitar
   * > MAIN    : |- |- Patch   : 31  SingleCoil
   * > MAIN    : |- |- Patch   : 32  Pick Bass 
   * > MAIN    : |- |- Patch   : 33  Thumb Bass
   * > MAIN    : |- |- Patch   : 34  SynBadBass
   * > MAIN    : |- |- Patch   : 35  VCO Bass  
   * > MAIN    : |- |- Patch   : 36  Violin    
   * > MAIN    : |- |- Patch   : 37  ChamberStr
   * > MAIN    : |- |- Patch   : 38  VCF String
   * > MAIN    : |- |- Patch   : 39  Nova Quire
   * > MAIN    : |- |- Patch   : 40  Vibraphone
   * > MAIN    : |- |- Patch   : 41  Takerimba 
   * > MAIN    : |- |- Patch   : 42  ShortBell 
   * > MAIN    : |- |- Patch   : 43  WarmBell  
   * > MAIN    : |- |- Patch   : 44  PC:ItoChim
   * > MAIN    : |- |- Patch   : 45  VCO Lead  
   * > MAIN    : |- |- Patch   : 46  Spirit VCF
   * > MAIN    : |- |- Patch   : 47  OZ Lead   
   * > MAIN    : |- |- Patch   : 48  Get Lucky 
   * > MAIN    : |- |- Patch   : 49  Gamma Band
   * > MAIN    : |- |- Patch   : 50  Metal Reed
   * > MAIN    : |- |- Patch   : 51  Modomatic 
   * > MAIN    : |- |- Patch   : 52  Gently    
   * > MAIN    : |- |- Patch   : 53  Mystichoir
   * > MAIN    : |- |- Patch   : 54  St.Michael
   * > MAIN    : |- |- Patch   : 55  Sharpy    
   * > MAIN    : |- |- Patch   : 56  FreeThem  
   * > MAIN    : |- |- Patch   : 57  Hollow Pad
   * > MAIN    : |- |- Patch   : 58  SatinGlass
   * > MAIN    : |- |- Patch   : 59  SatinGlass
   * > MAIN    : |- |- Patch   : 60  Saeg es   
   * > MAIN    : |- |- Patch   : 61  Revelation
   * > MAIN    : |- |- Patch   : 62  WdBass Duo
   * > MAIN    : |- |- Patch   : 63  DR Electry
   * > MAIN    : |- '- Patch   : 64  DR Dance  
   * > MAIN    : '- Patch Bank : P   Preset
   * > MAIN    :   |- Patch    : 01  Piano     
   * > MAIN    :   |- Patch    : 02  Voyager   
   * > MAIN    :   |- Patch    : 03  Pro55Brass
   * > MAIN    :   |- Patch    : 04  Elektrodes
   * > MAIN    :   |- Patch    : 05  Zaratustra
   * > MAIN    :   |- Patch    : 06  DawnChorus
   * > MAIN    :   |- Patch    : 07  GX Dream  
   * > MAIN    :   |- Patch    : 08  GrooveKing
   * > MAIN    :   |- Patch    : 09  DistGuitar
   * > MAIN    :   |- Patch    : 10  ZenAirBell
   * > MAIN    :   |- Patch    : 11  FullString
   * > MAIN    :   |- Patch    : 12  Jazz Man  
   * > MAIN    :   |- Patch    : 13  ClassPiano
   * > MAIN    :   |- Patch    : 14  Rock Piano
   * > MAIN    :   |- Patch    : 15  DX E.Piano
   * > MAIN    :   |- Patch    : 16  Hard EP   
   * > MAIN    :   |- Patch    : 17  Cry Clav  
   * > MAIN    :   |- Patch    : 18  Funky Clav
   * > MAIN    :   |- Patch    : 19  Deep Organ
   * > MAIN    :   |- Patch    : 20  Warm Organ
   * > MAIN    :   |- Patch    : 21  Trumpet   
   * > MAIN    :   |- Patch    : 22  Stab Brass
   * > MAIN    :   |- Patch    : 23  Big Band  
   * > MAIN    :   |- Patch    : 24  Orch Brass
   * > MAIN    :   |- Patch    : 25  SynthBrass
   * > MAIN    :   |- Patch    : 26  Flute     
   * > MAIN    :   |- Patch    : 27  Saxophone 
   * > MAIN    :   |- Patch    : 28  FolkGuitar
   * > MAIN    :   |- Patch    : 29  12 String 
   * > MAIN    :   |- Patch    : 30  MuteGuitar
   * > MAIN    :   |- Patch    : 31  SingleCoil
   * > MAIN    :   |- Patch    : 32  Pick Bass 
   * > MAIN    :   |- Patch    : 33  Thumb Bass
   * > MAIN    :   |- Patch    : 34  SynBadBass
   * > MAIN    :   |- Patch    : 35  VCO Bass  
   * > MAIN    :   |- Patch    : 36  Violin    
   * > MAIN    :   |- Patch    : 37  ChamberStr
   * > MAIN    :   |- Patch    : 38  VCF String
   * > MAIN    :   |- Patch    : 39  Nova Quire
   * > MAIN    :   |- Patch    : 40  Vibraphone
   * > MAIN    :   |- Patch    : 41  Takerimba 
   * > MAIN    :   |- Patch    : 42  Glocken   
   * > MAIN    :   |- Patch    : 43  DigiBell  
   * > MAIN    :   |- Patch    : 44  Oriental  
   * > MAIN    :   |- Patch    : 45  VCO Lead  
   * > MAIN    :   |- Patch    : 46  Spirit VCF
   * > MAIN    :   |- Patch    : 47  OZ Lead   
   * > MAIN    :   |- Patch    : 48  Get Lucky 
   * > MAIN    :   |- Patch    : 49  Gamma Band
   * > MAIN    :   |- Patch    : 50  Metal Reed
   * > MAIN    :   |- Patch    : 51  Modomatic 
   * > MAIN    :   |- Patch    : 52  DataStream
   * > MAIN    :   |- Patch    : 53  Mystichoir
   * > MAIN    :   |- Patch    : 54  St.Michael
   * > MAIN    :   |- Patch    : 55  Scatter   
   * > MAIN    :   |- Patch    : 56  Triton    
   * > MAIN    :   |- Patch    : 57  Amazon    
   * > MAIN    :   |- Patch    : 58  SatinGlass
   * > MAIN    :   |- Patch    : 59  BrassChime
   * > MAIN    :   |- Patch    : 60  Piano Mist
   * > MAIN    :   |- Patch    : 61  Xanadu    
   * > MAIN    :   |- Patch    : 62  WdBass Duo
   * > MAIN    :   |- Patch    : 63  Drum Set 1
   * > MAIN    :   '- Patch    : 64  Drum Set 2
   * > MAIN    : ------------------------------
   * > MAIN    : * Patch data available</pre></li>
   * </ol>
   * 
   * <p style="margin-bottom:0em"><b>References:</b></p>
   * <table style="margin-top:0em; margin-bottom:0em">
   *   <tr><td>[1]</td><td>
   *     Yamaha Corp.: TG55 Operating Manual. <a href="https://data.yamaha.com/files/download/other_assets/9/316979/TG55G.pdf">Online</a>, retrieved May 12, 2026</td></tr>
   * </table>
   */
  public static void createSimplePatchBank()
  {
    final int     devNum = 0; // <- Your zero-based device number here
    final String  miName = "KOMPLETE KONTROL EXT - 1"; // <- Name of your MIDI interface here
    MidiInterface mi     = null;

    // Value maps for parameters 'x' and 'y' in voice bulk header 2
    final int[]    bankNums  = new int[] {0x00, 0x02};
    final String[] bankIDs   = new String[] {"I","P"};
    final int[]    patchNums = SYX.getIntRange(0x00,0x3F);
    final String[] patchIDs  = new String[0x40];
    for (int i=0x00; i<=0x3F; i++)
      patchIDs[i] = String.format("%02d",i+1);

    // Create a patch bank
    BankTree<BankTree.IBankObject> patchBank
      = new BankTree<BankTree.IBankObject>("Patch","Patch Bank");
    patchBank.addBank(bankIDs[0],"Internal");
    patchBank.addBank(bankIDs[1],"Preset");

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
          try
          {
            // Try to parse as TG55 bulk message header
            // - Throws an InvalidMidiDataException if no TG55 bulk dump
            SyxMessage rcvMsg = new SyxMessage();
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1,"Bulk Header 1"));
            rcvMsg.setMessage(sxMsg.getMessage(),rcvMsg.getLength()/*Header bytes only!*/);
            int bc = rcvMsg.getPart(0).getMidiValue('b');

            // Parse voice bulk dump
            // - Throws an InvalidMidiDataException if no TG55 voice bulk dump
            rcvMsg.clear();
            rcvMsg.addParts(new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER1 ,"Bulk Header 1"));

            SyxDataStruct blkHdr2;
            SyxParamInfo  pi;
            blkHdr2 = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BD_HEADER2 ,"Bulk Header 2");
            pi = new SyxParamInfo(blkHdr2,'x',"BANK_ID","Bank ID");
            pi.setValueMap(bankNums,bankIDs);
            blkHdr2.addParamInfo(pi);
            pi = new SyxParamInfo(blkHdr2,'y',"PATCH_ID","Patch ID");
            pi.setValueMap(patchNums,patchIDs);
            blkHdr2.addParamInfo(pi);
            rcvMsg.addParts(blkHdr2);

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

            // Notify main thread
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

      mainLog.log(0,2,"Fetching patch names");
      for (int bankNum : bankNums)
        for (int patchNum : patchNums)
        {
          // Create bulk request message
          SyxDataStruct prt = new SyxDataStruct(TG55FormatSpecifiers.F_TG55_BULK_REQUEST);
          prt.setMidiValue('#',devNum  ); // Device no.
          prt.setMidiValue('s','V'     ); // Voice bulk type
          prt.setMidiValue('t','C'     ); // Voice bulk type
          prt.setMidiValue('x',bankNum ); // Bank no.
          prt.setMidiValue('y',patchNum); // Patch no.
          SyxMessage blkReq = new SyxMessage(prt);

          // Send bulk request and wait for bulk dump
          blkDmp.clear();
          synchronized (blkDmp)
          {
            mi.send(blkReq);
            try
            {
              blkDmp.wait(2000);
            }
            catch (InterruptedException e)
            { // Ignore
            }
          }

          if (!blkDmp.isEmpty())
          {
            if (patchNum==0)
              mainLog.log("\n");
            mainLog.log(".");

            // Get and print patch name
            SyxDataStruct blkHdr2 = blkDmp.getPart(1);
            SyxDataStruct vceHdr  = blkDmp.getPart(2);
            String bankID    = blkHdr2.getModelValueAsString('x');
            String patchID   = blkHdr2.getModelValueAsString('y');
            String patchName = vceHdr.readString("a-j");
            patchBank.addBankObject(bankID,patchID,patchName);
          }
          else
            mainLog.errlog(".");
        }
      mainLog.log(0,-2,"\n");
      mainLog.log("Done\n\n");
      mainLog.log(patchBank.prettyPrint());
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
   *   [{@linkplain HandlingBulkData#createSimplePatchBank() &lt;prev}]
   *   | Back to 
   *     [{@linkplain HandlingBulkData Handling Bulk Data}]
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
    HandlingBulkData.syxFiles();
  }

}

// EOF