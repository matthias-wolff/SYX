package de.btu.kt.syx.util;

import java.util.ArrayList;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.ISysexMessageListener;

/**
 * A physical or virtual MIDI interface with a MIDI in and a MIDI out
 * {@linkplain MidiDevice device}.
 * 
 * @author Matthias Wolff</a>
 */
public class MidiInterface implements ILogger
{

  // -- Constants -------------------------------------------------------------

  protected static final String S_MIDIPORT_NOTFOUND
    = "MIDI %s '%s' not found";
  protected final String S_MIDIPORT_NOTOPEN
    = "MIDI %s port not open";
  protected static final String S_MIDIPORT_NOACCESS
    = "Cannot access MIDI %s '%s'";

  // -- Attributes ------------------------------------------------------------

  /**
   * Interface name.
   */
  private String name;
  
  /**
   * The MIDI output device.
   */
  private MidiDevice mdOut;
  
  /**
   * The MIDI input device.
   */
  private MidiDevice mdIn;

  /**
   * Registered MIDI system exclusive message listeners
   */
  private ArrayList<ISysexMessageListener> listeners;

  /**
   * Verbose level.
   */
  private int verbose;

  /**
   * {@link AMidiMonitor} to log MIDI input and output to.
   */
  private AMidiMonitor monitor;

  // -- Constructors ----------------------------------------------------------
  
  /**
   * Creates a new MIDI interface.
   * 
   * @param name    The device name
   * @param mdOut   The MIDI output device
   * @param mdIn    The MIDI input device
   * @param verbose The verbose level
   */
  public MidiInterface
  (
    String     name,
    MidiDevice mdOut,
    MidiDevice mdIn, 
    int        verbose
  )
  {
    this.name = name;
    this.verbose = verbose;
    log(2,"Creating MIDI interface '%s'\n",name);

    this.listeners = new ArrayList<ISysexMessageListener>();
    this.mdOut   = mdOut;
    this.mdIn    = mdIn;
    this.monitor = null;

    log(2,"MIDI out port at '%s'\n",name);
    log(3,printMidiDeviceInfo(this.mdOut.getDeviceInfo()));
    log(2,"MIDI in port at '%s'\n",name);
    log(3,printMidiDeviceInfo(this.mdIn.getDeviceInfo()));
  }
  
  /**
   * Creates a new MIDI interface. Invokes {@link #findMidiOutPort} and {@link 
   * #findMidiInPort} with argument {@code name} to assign the MIDI out and in 
   * ports.
   * 
   * @param  name
   *          The device name.
   * @param verbose
   *          The verbose level.
   * @throws MidiUnavailableException
   *          if the in and/or ports cannot be found or accessed.
   * 
   * @see #findMidiOutPort
   * @see #findMidiInPort
   * @see #printMidiDeviceList
   */
  public MidiInterface(String name, int verbose)
  throws MidiUnavailableException
  {
    this.listeners = new ArrayList<ISysexMessageListener>();
    this.name = name;
    this.verbose = verbose;
    log(2,"Creating MIDI interface '%s' ...\n",name);

    log(2,"- Searching MIDI out port at '%s' ...",name);
    this.mdOut = findMidiOutPort(name);
    log(2," ok\n",name);
    if (verbose>=2) printMidiDeviceInfo(this.mdOut.getDeviceInfo());

    log(2,"- Searching MIDI in port at '%s' ...",name);
    this.mdIn = findMidiInPort(name);
    log(2," ok\n");
    log(2,"ok\n");
    log(3,printMidiDeviceInfo(this.mdIn.getDeviceInfo()));
  }
  
  /**
   * Creates a new MIDI interface. Invokes {@link #findMidiOutPort} and {@link 
   * #findMidiInPort} with argument {@code name} to assign the MIDI out and in 
   * ports.
   * 
   * @param  name
   *          The device name.
   * @throws MidiUnavailableException
   *          if the in and/or ports cannot be found or accessed.
   * 
   * @see #findMidiOutPort
   * @see #findMidiInPort
   * @see #printMidiDeviceList
   */
  public MidiInterface(String name)
  throws MidiUnavailableException
  {
    this(name,0);
  }

  // -- API: Life Cycle -------------------------------------------------------

  /**
   * Opens the MIDI out and in ports.
   * 
   * @throws MidiUnavailableException
   *          if the port(s) cannot be opened due to resource restrictions.
   * @throws MidiUnavailableException 
   *          if the port(s) cannot be opened.
   * @throws SecurityException
   *          if the port(s) cannot be opened due to security restrictions.
   * @see #reset
   * @see #close
   * @see #openOutPort
   * @see #openInPort
   */
  public void open()
  throws MidiUnavailableException, MidiUnavailableException
  {
    openOutPort();
    openInPort();
  }

  /**
   * Closes the MIDI out and in ports.
   * 
   * @see #open
   * @see #reset
   * @see #closeOutPort
   * @see #closeInPort
   */
  public void close()
  {
    closeOutPort();
    closeInPort();
  }

  /**
   * Resets this MIDI interface.
   * 
   * @throws MidiUnavailableException
   *          if the port(s) cannot be opened due to resource restrictions.
   * @throws MidiUnavailableException 
   *          if the port(s) cannot be opened.
   * @throws SecurityException
   *          if the port(s) cannot be opened due to security restrictions.
   * @see #open
   * @see #close
   * @see #closeOutPort
   * @see #closeInPort
   */
  public void reset()
  throws MidiUnavailableException, MidiUnavailableException
  {
    log(1,2,"Resetting MIDI interface '%s' ...\n",this.name);
    close();
    open();
    log(1,-2,"ok\n");
  }
  
  /**
   * Opens the MIDI out port (only).
   * 
   * @throws MidiUnavailableException
   *          if the device cannot be opened due to resource restrictions.
   * @throws SecurityException
   *          if the device cannot be opened due to security restrictions.
   * @see #closeOutPort
   * @see #open
   */
  public void openOutPort()
  throws MidiUnavailableException
  {
    log(2,"Opening MIDI out port at '%s' ...",this.name);
    mdOut.open();
    log(2," ok\n");
  }

  /**
   * Closes the MIDI out port (only).
   * 
   * @see #openOutPort
   * @see #close
   */
  public void closeOutPort()
  {
    log(2,"Closing MIDI out port at '%s' ...",this.name);
    mdOut.close();
    log(2," ok\n");
  }

  /**
   * Opens the MIDI in port (only).
   * 
   * @throws MidiUnavailableException
   *          if the device cannot be opened due to resource restrictions.
   * @throws MidiUnavailableException
   *          if the device cannot be opened.
   * @throws SecurityException
   *          if the device cannot be opened due to security restrictions.
   * @see #closeInPort
   * @see #open
   */
  public void openInPort()
  throws MidiUnavailableException, MidiUnavailableException
  {
    log(2,"Opening MIDI in port at '%s' ...",this.name);
    mdIn.open();
    log(2," ok\n");
    listen();
  }

  /**
   * Closes the MIDI in port (only).
   * 
   * @see #openInPort
   * @see #close
   */
  public void closeInPort()
  {
    stopListening();
    log(2,"Closing MIDI in port at '%s' ...",this.name);
    mdIn.close();
    log(2," ok\n");
  }

  // -- API: MIDI Send/Receive ------------------------------------------------
  
  /**
   * Sends a MIDI message through the out port of this interface.
   * 
   * @param message The MIDI message to send.
   * @throws MidiUnavailableException if the out port is closed.
   */
  public void send(MidiMessage message)
  throws MidiUnavailableException
  {
    log(3,"Getting MIDI out receiver at '%s' ...",this.name);
    Receiver rcvr = mdOut.getReceiver();
    log(3," ok\n",this.name);
    log(3,"Sending MIDI message at '%s': ...\n",this.name);
    if (getVerbose()>=3)
      log(3,SYX.prettyPrintMidiMessage(message));
    if (monitor!=null)
    {
      monitor.midiOutLog(String.format("MIDI OUT at '%s':\n",this.name));
      monitor.midiOutLog(SYX.prettyPrintMidiMessage(message)+"\n\n");
    }
    rcvr.send(message,-1);
    log(3,"\n- ok\n");
  }

  /**
   * Adds a system exclusive message listener to this MIDI interface.
   * 
   * @param listener The listener.
   */
  public void addSysExListener(ISysexMessageListener listener)
  {
    this.listeners.add(listener);
  }

  /**
   * Removes a system exclusive message listener from this MIDI interface.
   * 
   * @param listener The listener.
   */
  public void removeSysExListener(ISysexMessageListener listener)
  {
    this.listeners.remove(listener);
  }
  
  /**
   * Starts listening for incoming MIDI messages at the MIDI in port.
   * 
   * @throws MidiUnavailableException
   *           if the MIDI in port is not open. 
   */
  protected void listen()
  throws MidiUnavailableException, MidiUnavailableException
  {
    log(2,"Listening for SysEx messages at '%s' ...",this.name);
    if (!isInPortOpen())
      throw new MidiUnavailableException(SYX.ERR(S_MIDIPORT_NOTOPEN,"in"));
    
    this.mdIn.getTransmitter().setReceiver(new Receiver()
    {
      @Override
      public void send(MidiMessage message, long timeStamp)
      {
        // Debug log
        log(3,"Received MIDI message at '%s':\n",name);
        log(3,SYX.prettyPrintMidiMessage(message,timeStamp));
        log(3,"\n");

        if (message instanceof SysexMessage)
        {
          if (monitor!=null)
          {
            monitor.midiInLog(String.format("MIDI IN at '%s':\n",name));
            monitor.midiInLog(SYX.prettyPrintMidiMessage(message)+"\n\n");
          }

          if (listeners.size()==0)
          {
            log(3,"no listeners, aborting\n");
            return;
          }

          // Dispatch SysEx message to listeners
          log(3,2,"Dispatching MIDI SysEx message to listeners ...\n");
          for (ISysexMessageListener listener : listeners)
          {
            log(3,2,"Listener %s#%02d ... ",
              listener.getClass().getName(),
              listener.getDevNum()
            );
            try
            {
              listener.receiveSysexMsg((SysexMessage)message,timeStamp);
              log(3,-2,"accepted\n");
            }
            catch (Exception e)
            {
              log(3,-2,"REJECTED\n");
            }
          }
          log(3,-2,"Dispatch complete\n");
        }
        else
          log(3,"Message ignored (not SysEx)\n");
      }
      
      @Override
      public void close()
      {
        // Nothing
      }
    });

    log(2," ok\n");
  }
  
  /**
   * Stops listening for incoming MIDI messages at the MIDI in port.
   */
  protected void stopListening()
  {
    log(2,"Stop listening for SysEx messages at '%s' ...",this.name);
    try
    {
      this.mdIn.getTransmitter().setReceiver(null);
      log(2," ok\n");
    }
    catch (MidiUnavailableException e)
    {
      e.printStackTrace();
    }
  }

  // -- API: Getters and Setters ----------------------------------------------

  /**
   * Returns the name of this MIDI interface.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Returns {@code true} if the MIDI out port is open, {@code false} otherwise.
   */
  public boolean isOutPortOpen()
  {
    return mdOut.isOpen();
  }

  /**
   * Returns {@code true} if the MIDI in port is open, {@code false} otherwise.
   */
  public boolean isInPortOpen()
  {
    return mdIn.isOpen();
  }

  /**
   * Sets a {@link AMidiMonitor} to log MIDI input and output to.
   * 
   * @param monitor
   *          The monitor, can be {@code null}
   */
  public void setMonitor(AMidiMonitor monitor)
  {
    this.monitor = monitor;
  }

  /**
   * Returns the MIDI in device of this interface.
   */
  public MidiDevice getMidiInDevice()
  {
    return this.mdIn;
  }

  /**
   * Returns the MIDI out device of this interface.
   */
  public MidiDevice getMidiOutDevice()
  {
    return this.mdOut;
  }

  // Suggested API
  //public SysexMessage fetch(SysexMessage request, SyxPattern responseFilter)

  // -- API: Static Utility Methods -------------------------------------------

  /**
   * Finds a MIDI out port by the MIDI device name.
   * 
   * @param name
   *          The name of the MIDI device.
   * @return The {@link MidiDevice} representing the MIDI out port.
   * @throws MidiUnavailableException if the port cannot be found or accessed.
   */
  public static MidiDevice findMidiOutPort(String name)
  throws MidiUnavailableException
  {
    return int_findMidiPort(name, true);
  }
  
  /**
   * Finds a MIDI in port by the MIDI device name.
   * 
   * @param name
   *          The name of the MIDI device.
   * @return The {@link MidiDevice} representing the MIDI in port.
   * @throws MidiUnavailableException if the port cannot be found or accessed.
   */
  public static MidiDevice findMidiInPort(String name)
  throws MidiUnavailableException
  {
    return int_findMidiPort(name, false);
  }

  /**
   * Prints MIDI device information to a string.
   * 
   * @param mdi
   *          The {@link MidiDevice.Info}
   * @return The printed string
   */
  public static String printMidiDeviceInfo(MidiDevice.Info mdi)
  {
    String s = String.format("MIDI device '%s'\n",mdi.getName());
    s += String.format("- vendor: %s\n",mdi.getVendor());
    s += String.format("- descr : %s\n",mdi.getDescription());
    try
    {
      MidiDevice md = MidiSystem.getMidiDevice(mdi);
      int xt = md.getMaxTransmitters();
      int xr = md.getMaxReceivers();
      s += String.format("- in    : %s\n",xt<0?"unlimited":String.format("%d",xt));
      s += String.format("- out   : %s\n",xr<0?"unlimited":String.format("%d",xr));
      s += String.format("- open  : %s\n",md.isOpen()?"yes":"no");
    }
    catch (MidiUnavailableException e)
    {
      s += String.format("- NO ACCESS\n");
    }
    return s;
  }

  /**
   * Lists available MIDI devices to a string.
   * 
   * @return The printed string
   */
  public static String printMidiDeviceList()
  {
    String s = String.format("Available MIDI devices at '%s'\n",SYX.getHostName());
    MidiDevice.Info[] mdi = MidiSystem.getMidiDeviceInfo();
    for (int i=0; i<mdi.length; i++)
    {
      s += String.format("%d\n",i);
      s += printMidiDeviceInfo(mdi[i]);
    }
    return s + "\n";
  }

  // -- Internal Utility Methods ----------------------------------------------
  
  /**
   * Finds a MIDI port by the MIDI device name.
   * 
   * @param name
   *          The name of the MIDI device.
   * @param receiver
   *          If true, find a MIDI out port. If false, find a MIDI in port.
   * @return The {@link MidiDevice} representing the port.
   * @throws MidiUnavailableException if the MIDI port cannot be found or accessed.
   */
  private static MidiDevice int_findMidiPort(String name, boolean receiver)
  throws MidiUnavailableException
  {
    String dt = receiver?"receiver":"transmitter";
    MidiDevice.Info[] mdi = MidiSystem.getMidiDeviceInfo();
    for (int i=0; i<mdi.length; i++)
      if (mdi[i].getName().equals(name))
        try
        {
          MidiDevice md = MidiSystem.getMidiDevice(mdi[i]);
          if (receiver && md.getMaxReceivers()!=0) return md;
          if (!receiver && md.getMaxTransmitters()!=0) return md;
        }
        catch (MidiUnavailableException e)
        {
          throw new 
            MidiUnavailableException
            (
              SYX.ERR(MidiInterface.S_MIDIPORT_NOACCESS,dt,name)
            );
        }
    throw new 
      MidiUnavailableException
      (
        SYX.ERR(MidiInterface.S_MIDIPORT_NOTFOUND,dt,name)
      );
  }

  // -- Implementation of ILooger ---------------------------------------------
  
  @Override
  public int getVerbose()
  {
    return this.verbose;
  }

  public void setVerbose(int verbose)
  {
    this.verbose = verbose;
  }

  @Override
  public String getLogID()
  {
    return "MI";
  }

}

// EOF
