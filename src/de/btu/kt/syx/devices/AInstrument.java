package de.btu.kt.syx.devices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeoutException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.ISysexMessageListener;
import de.btu.kt.syx.midi.SyxDataStruct;
import de.btu.kt.syx.util.ILogger;
import de.btu.kt.syx.util.MidiInterface;
import de.btu.kt.syx.util.SysexRecorder;

/**
 * Abstract device model which facilitates communication with synthesizes via
 * MIDI system exclusive messages.
 * 
 * @author Matthias Wolff
 */
public abstract class AInstrument 
implements Serializable, ISysexMessageListener, ILogger
{
  private static final long serialVersionUID = 1L;

  // Nested Classes -----------------------------------------------------------

  protected class Semaphor
  {
    SysexMessage sxMsg = null;
    SyxDataStruct rsFlt = null;
    long timeStamp = -1;
  }

  // -- Constants -------------------------------------------------------------

  /**
   * Request timeout in milliseconds
   * <p>
   * TODO: TIMEOUT should be an {@link ORK_GUI} setting.
   * </p>
   */
  public static int TIMEOUT = 500;

  /**
   * Checksum error message.
   */
  public static final String E_CHECKSUM
    = "Checksum validation failed";

  /**
   * Null-argument error message.
   */
  public static final String E_ARG_NULL
    = "Argument %s must not be null";

  /**
   * Argument out of range error message.
   */
  public static final String E_ARG_RANGE
    = "Argument devNum=%d is out of range [1...%d]";

  /**
   * Concurrent request error message.
   */
  public static final String E_RQ_CONCURRENT
    = "Another requst is pending";

  // -- Attributes ------------------------------------------------------------

  /**
   * Verbose level, 0: silence
   */
  protected int verbose;

  /**
   * The device number of the hardware instrument (1, ..., {@link 
   * #getMaxDevNum}{@code ()}). Device numbers identify multiple
   * instruments of the same type. 
   */
  protected int devNum;

  /**
   * The MIDI interface to which the hardware instrument is connected
   */
  protected transient MidiInterface mi;

  /**
   * Semaphore for synchronization of {@link
   * #fetchSysexMsg(SysexMessage, String)}. 
   */
  private transient Semaphor semaphor;
  
  // -- Constructors ---------------------------------------------------------

  /**
   * Creates a new hardware instrument wrapper.
   * 
   * @param devNum
   *          The {@linkplain #devNum device number} of the instrument (1, ...,
   *          {@link getMaxDeviceNumber}{@code ()})
   * @param verbose
   *          Verbose level for console log, 0: silence
   * @throws IllegalArgumentException
   *          if {@code mi} is {@code null}, or if {@code devNum} is out of range
   */
  public AInstrument(int devNum, int verbose)
  throws IllegalArgumentException
  {
    // Check arguments
    int maxDevNum = getMaxDevNum();
    if (devNum<1 || devNum>maxDevNum)
      throw SYX.IllArgExc(SYX.ERR(E_ARG_RANGE,devNum,maxDevNum));

    // Initialize attributes
    this.devNum   = devNum;
    this.verbose  = verbose;
    this.mi       = null; // Invoke setMidiInterface!
    this.semaphor = null; // Lazy initialization
  }

  /**
   * Creates a new hardware instrument wrapper.
   * 
   * @param devNum
   *          The {@linkplain #devNum device number} of the instrument (1, ...,
   *          {@link getMaxDeviceNumber}{@code ()})
   */
  public AInstrument(int devNum)
  {
    this(devNum,0);
  }

  // -- Getters and Setters ---------------------------------------------------

  /**
   * Returns the MIDI interface to which the hardware instrument is connected.
   */
  public MidiInterface getMidiInterface()
  {
    return this.mi;
  }

  /**
   * Sets the MIDI interface to which the hardware instrument is connected.
   * 
   * @param mi
   *          The MIDI interface to which the instrument is connected
   * @throws IllegalArgumentException
   *          if {@code mi} is {@code null}
   */
  public void setMidiIterface(MidiInterface mi)
  {
    if (mi==null)
      throw SYX.IllArgExc(SYX.ERR(E_ARG_NULL,"mi"));
    this.mi = mi;
    this.mi.addSysExListener(this);
  }

  // -- Hardware Instrument Properties ----------------------------------------

  /**
   * Returns the {@linkplain #devNum device number} of the hardware instrument.
   */
  @Override
  public int getDevNum()
  {
    return this.devNum;
  }
  
  /**
   * Returns the maximal number of hardware instruments of the same type 
   * (typically 16).
   */
  public int getMaxDevNum()
  {
    return 16;
  }
  
  // -- SysEx Operations ------------------------------------------------------

  /**
   * Sends a MIDI system exclusive message to the hardware instrument.
   *  
   * @param sxMsg The message to send
   * @throws MidiUnavailableException if the MIDI out port is closed.
   */
  public void sendSysexMsg(SysexMessage sxMsg)
  throws MidiUnavailableException
  {
    mi.send(sxMsg);
  }

  /**
   * Sends a MIDI system exclusive message to the hardware instrument and blocks
   * until a system exclusive message matching {@code pattern} has been received
   * from the instrument. Received messages not matching {@code pattern} will be 
   * dispatched normally. If the fetch attempt fails, the method will retry 10
   * times. 
   * 
   * @param rqMsg 
   *          The system exclusive message to send
   * @param format
   *          A {@linkplain SyxDataStruct message format specifier} defining the
   *          format of the expected response message
   * @return The system exclusive message received as response
   * @throws IllegalArgumentException
   *          if {@code pattern} is {@code null} or invalid
   * @throws MidiUnavailableException
   *           if the MIDI out port is closed or if all fetch attempts failed
   * @throws TimeoutException
   *           if no matching response has been received in time
   * @throws IllegalStateException
   *           if another request is pending
   * @throws InterruptedException
   *           if waiting for an response has been interrupted (should not 
   *           happen)
   */
  public SysexMessage fetchSysexMsg(SysexMessage rqMsg, String format)
  throws IllegalArgumentException, MidiUnavailableException, 
         IllegalStateException
  {
    int maxAttempts = 10;
    for (int attempt=1; attempt<=maxAttempts; attempt++)
      try
      {
        return fetchSysexMsgInt(rqMsg,format);
      }
      catch (TimeoutException e) 
      {
        log(1,"TIMOUT\n");
        mi.reset();
        if (attempt<=maxAttempts)
          log(1,"Attempt %d of %d ... \n",attempt+1,maxAttempts);
        else
          log(1,"%d attempts FAILED\n",maxAttempts);
      }
    throw new MidiUnavailableException();
  }

  protected SysexMessage fetchSysexMsgInt(SysexMessage rqMsg, String format)
  throws IllegalArgumentException, MidiUnavailableException, TimeoutException, 
         IllegalStateException
  {
    log(2,+2,"\nMIDI request to %s #%d ... ",getClass().getSimpleName(),devNum);

    // Check arguments and state
    if (this.semaphor==null)
      this.semaphor = new Semaphor();
    else if (isFetchPending())
      throw new IllegalStateException(E_RQ_CONCURRENT);
    
    long t = System.nanoTime();
    SysexMessage rsMsg = null;
    synchronized (this.semaphor)
    {
      // Set received SysEx message filter
      this.semaphor.rsFlt = new SyxDataStruct(format);
  
      // Send request
      sendSysexMsg(rqMsg);
      log(2,-2,"ok (%d ms)",(System.nanoTime()-t)/1000000);
      
      // Wait for response
      log(2,+2,", waiting for response ... ");
      try 
      {
        t = System.nanoTime();
        this.semaphor.wait(TIMEOUT);
      }
      catch (InterruptedException e)
      {
        // Ignore
      }
      rsMsg = this.semaphor.sxMsg;
      this.semaphor.rsFlt = null;
      this.semaphor.sxMsg = null;
    }
    if (rsMsg==null)
    {
      log(2,-2,"TIMEOUT (%d ms)\n",(System.nanoTime()-t)/1000000);
      throw new TimeoutException();
    }
    log(2,-2,"completed (%d ms)\n",(System.nanoTime()-t)/1000000);
    return rsMsg;
  }
  
  /**
   * Implementation of system exclusive message listener callback. The method 
   * synchronizes system exclusive data requests (see method {@link 
   * #fetchSysexMsg(SysexMessage, SyxDataStruct) fetchSysexMsg}. If the received
   * message is not a response to a pending request, the method passes
   * processing on to derived classes by invoking {@link
   *  #processSysexMsg(SysexMsg) processSysexMsg} with {@code xMsg}.
   * 
   * @param sxMsg
   *          The received system exclusive message
   * @param timeStamp
   *          TimeStamp the time-stamp for the message, in microseconds
   * @throws InvalidMidiDataException
   *           if this listener cannot process the message
   */
  @Override
  public void receiveSysexMsg(SysexMessage sxMsg, long timeStamp)
  throws InvalidMidiDataException
  {
    // Process response on request
    if (this.semaphor!=null && this.semaphor.rsFlt!=null)
      if (this.semaphor.rsFlt.match(sxMsg))
      {
        synchronized (this.semaphor)
        {
          this.semaphor.sxMsg = sxMsg;
          this.semaphor.timeStamp = timeStamp;
          this.semaphor.notifyAll();
        }
        return;
      }

    // Pass processing on to derived classes
    // XXX: Method receiveSysexMsg(...): Pass time stamp to processSysexMsg(...)
    processSysexMsg(sxMsg); 
  }

  /**
   * Called when an incoming MIDI system exclusive message is dispatched. 
   * Derived classes are expected
   * <ul>
   *   <li>either to update their internal model from the system exclusive 
   *   message <em>--or--</em></li>
   *   <li>to throw an {@link InvalidMidiDataException} if they cannot process
   *   the message.</or>
   * </ul>
   * 
   * @param sxMsg 
   *          The system exclusive message
   * @throws InvalidMidiDataException
   *          if the message cannot be processed
   */
  public abstract void processSysexMsg(SysexMessage sxMsg)
  throws InvalidMidiDataException;

  /**
   * Returns {@code true} if a system exclusive data fetch is pending.
   * 
   * @see #fetchSysexMsg(SysexMessage, String)
   */
  public boolean isFetchPending()
  {
    return this.semaphor.rsFlt!=null || this.semaphor.sxMsg!=null;
  }

  // -- Persistence -----------------------------------------------------------

  /**
   * Returns the absolute persistence file name of an {@link AInstrument}: 
   * "&lang;<i>path</i>&rang;/&lang;<i>class</i>&rang;#&lang;<i>nn</i>&rang;.state"
   * with </p>
   * <ul>
   *   <li><i>path</i>: {@code persistencePath},</li>
   *   <li><i>class</i>: {@code clazz.}{@link Class#getSimpleName() getSimpleName()},
   *     and</li>
   *   <li><i>nn</i>: {@code devNum} (01, 02, ..., 16)
   * </ul>
   * 
   * @param persistencePath
   *          Absolute path to persistence files
   * @param clazz
   *          The instrument class
   * @param devNum
   *          The device number of the instrument (1, ..., 16)
   * @throws IOException
   *          if the file name cannot be created
   * @see #saveState()
   * @see #restoreState(Class, int, int)
   */
  protected static String getStateFileName
  (
    String persistencePath,
    Class<? extends AInstrument> clazz,
    int devNum
  )
  throws IOException
  {
    String path = persistencePath;
    String devn = String.format("%02d",devNum);
    String fnam = clazz.getSimpleName()+"#"+devn+".state";
    File   file = new File(path+"/"+fnam);
    return file.getCanonicalPath();
  }

  /**
   * Saves the state of this instrument to its state file. 
   * 
   * @param persistencePath
   *          Absolute path to persistence files
   * @throws IOException
   *          if serialization failed
   * @see #restoreState(Class, int, int)
   * @see #getStateFileName(Class, int)
   */
  public void saveState(String persistencePath)
  throws IOException
  {
    log(1,"Saving device state ... ");
    try
    {
      Class<? extends AInstrument> clazz = this.getClass();
      String fname = getStateFileName(persistencePath,clazz,devNum);
      log(2,"\n- File: %s\n",fname);
  
      FileOutputStream   fos = new FileOutputStream(fname);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      oos.flush();
      oos.close();
      log(1,"ok\n");
    }
    catch (Exception e)
    {
      log(1," FAILED\n");
      if (this.verbose>=2)
        log(e);
      throw e;
    }
  }

  /**
   * Loads the state of an {@linkplain AInstrument instrument} from a state
   * file.
   * 
   * @param persistencePath
   *          Absolute path to persistence files
   * @param clazz
   *          The instrument class
   * @param devNum
   *          The device number of the instrument (1, ..., 16)
   * @param verbose
   *          Verbose level of log, ignored if {@logPrefix} is {@code null}
   * @return The restored {@linkplain AInstrument instrument} instance
   * @throws IOException
   *           if de-serialization failed
   * @throws ClassNotFoundException
   *           if {@code clazz} cannot be de-serialized
   * @see #saveState()
   * @see #getStateFileName(Class, int)
   */
  public static AInstrument restoreState
  (
    String persistencePath,
    Class<? extends AInstrument> clazz,
    int devNum,
    int verbose
  )
  throws IOException, ClassNotFoundException
  {
    try
    {
      String fname = getStateFileName(persistencePath,clazz,devNum);

      String lpfx = String.format("%s #%02d",clazz.getSimpleName(),devNum);
      if (verbose>=1)
        System.out.printf("%8s: Restoring device state ... ",lpfx);
      if (verbose>=2)
        System.out.printf("\n%8s: - File: %s\n%8s: ",lpfx,fname,lpfx);

      FileInputStream   fis = new FileInputStream(fname);
      ObjectInputStream ois = new ObjectInputStream(fis);
      AInstrument instrument = (AInstrument)ois.readObject();
      ois.close();

      if (verbose>=1)
        System.out.printf("ok\n");
      return instrument;
    }
    catch (Exception e)
    {
      if (verbose>=1)
        System.out.printf(" FAILED\n");
      throw e;
    }
  }

  // -- MIDI Files Support ----------------------------------------------------

  /**
   * Records the MIDI system exclusive data of this instrument wrapper on a 
   * {@link SysexRecorder}. The method is used when writing MIDI system 
   * exclusive data files.
   * 
   * @param recorder
   *          The recorder
   */
  public abstract void record(SysexRecorder recorder);

  // -- UI and Log Methods ----------------------------------------------------

  @Override
  public int getVerbose()
  {
    return verbose;
  }  

  /**
   * Pretty-prints the wrapper's internal state to a string.
   * 
   * @return The printed string
   */
  protected abstract String prettyPrint();

  /**
   * Pretty-prints the wrapper's internal data structure.
   * 
   * @return The printed string
   */
  protected abstract String prettyPrintDataStructure();

}

// EOF