package de.btu.kt.syx.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.midi.ISysexMessageListener;

/**
 * Recorder for MIDI system exclusive messages and SYX file support.
 *  
 * @author Matthias Wolff
 */
public class SysexRecorder implements ILogger
{
  
  // -- Fields ----------------------------------------------------------------

  /**
   * Recorder tape.
   */
  protected ArrayList<SysexMessage> tape;

  /**
   * The play-back listeners.
   */
  protected HashSet<ISysexMessageListener> listeners;

  /**
   * Verbose level.
   */
  protected int verbose;

  // -- Constructors ----------------------------------------------------------
  
  /**
   * Creates a new SysEx message recorder.
   */
  public SysexRecorder()
  {
    this.listeners = new HashSet<ISysexMessageListener>();
    this.verbose   = 0;
    reset();
  }

  // -- Play-back Listeners Support -------------------------------------------

  /**
   * Adds a SysEx message listener to this recorder.
   * 
   * @param listener The listener.
   */
  public void addSysExListener(ISysexMessageListener listener)
  {
    this.listeners.add(listener);
  }
  
  /**
   * Removes a SysEx message listener from this recorder.
   * 
   * @param listener The listener.
   */
  public void removeSysExListener(ISysexMessageListener listener)
  {
    this.listeners.remove(listener);
  }

  // -- Recorder Operations ---------------------------------------------------

  /**
   * Clears this recorder's tape
   */
  public void reset()
  {
    this.tape = new ArrayList<SysexMessage>();
  }
  
  /**
   * Appends a {@linkplain SysexMsg SysEx message} to this recorder's tape.
   * 
   * @param msg The Message.
   * @throws IllegalArgumentException if <code>msg</code> is <code>null</code>.
   */
  public void record(SysexMessage msg)
  {
    if (msg==null)
      throw SYX.IllArgExc("Argument msg must not be null");
    this.tape.add(msg);
  }

  /**
   * Plays back this recorder's tape.
   */
  public void play()
  {
    log(1,+2,"Playing back SysEx messages to listeners ... ");
    if (this.listeners.size()==0)
    {
      log(1,"no listeners, aborting\n");
      return;
    }
    log(2,"\n");
    for (int i=0; i<this.tape.size(); i++)
    {
      log(2,+2,"Tape pos. #%d ... ",i);
      log(3,"\n");
      int nAccepted = 0;
      ISysexMessageListener lAccepted = null;
      for (ISysexMessageListener listener : this.listeners)
      {
        log(3,"- %s #%d ... ",
            listener.getClass().getSimpleName(),
            listener.getDevNum()
          );
        try
        {
          listener.receiveSysexMsg(this.tape.get(i));
          lAccepted = listener;
          nAccepted ++;
        }
        catch (Exception e)
        {
          log(3,"REJECTED\n");
        }
      }
      switch (nAccepted)
      {
      case 0: 
        log(2,-2,"REJECTED by all listeners!\n");
        break;
      case 1: 
        log(2,-2,"accepted by %s #%d\n",
          lAccepted.getClass().getSimpleName(),lAccepted.getDevNum()
        );
        break;
      default:
        log(2,-2,"accepted by %d listeners\n",nAccepted);
      }
    }
    log(1,-2,"end of tape\n");
  }

  /**
   * Returns the recorder's tape.
   * 
   * @return An array of SysEx messages
   */
  public SysexMessage[] getTape()
  {
    return tape.toArray(new SysexMessage[0]);
  }

  // -- SYX and TXT File Support ----------------------------------------------
  
  /**
   * Reads a MIDI SysEx file into this recorder's tape.
   * 
   * @param fileName The SYX file path.   
   * @throws IOException
   * @see {@link #writeSyxFile(String)}
   * @see {@link #readTxtFile(String)
   */
  public void readSyxFile(String fileName)
  throws IOException
  {
    log(1,+2,"Reading SYX file \"%s\" ... ",fileName);
    log(3,"\n");

    // Read tape from file
    String[] warnings = readData(Files.readAllBytes(Paths.get(fileName))); 

    // Print warnings (if any)
    log(1,-2,"ok%s\n",warnings.length>0?" (with warnings)":"");
    if (warnings.length>0)
    {
      System.err.printf(
        "%d warning%s reading SYX file '%s':\n",
        warnings.length, warnings.length==1 ? "" : "s", fileName 
      );
      for (String warning : warnings)
        System.err.println("- "+warning);
    }
  }

  /**
   * Writes this recorder's tape into a MIDI SysEx file.
   * 
   * @param fileName The SYX file path.   
   * @throws IOException 
   * @see {@link #readSyxFile(String)}
   * @see {@link #writeTxtFile(String)
   */
  public void writeSyxFile(String fileName)
  throws IOException
  {
    log(1,+2,"Writing SYX file \"%s\" ... ",fileName);

    // Preflight
    int flen = 0;
    for (int i=0; i<this.tape.size(); i++)
      flen += this.tape.get(i).getLength();
    
    // Copy tape to a byte buffer
    ByteBuffer buffer = ByteBuffer.allocate(flen);
    for (int i=0; i<this.tape.size(); i++)
    {
      byte[] data = this.tape.get(i).getMessage();
      int    mlen = this.tape.get(i).getLength();
      buffer.put(data,0,mlen);
    }

    // Write binary file
    Files.write(Paths.get(fileName),buffer.array());
    log(1,-2,"ok\n");
  }

  /**
   * Reads a MIDI SysEx text file. The file is expected to contain a
   * white-space separated list of two-digit hexadecimal numbers.
   * 
   * @param fileName The text file path.   
   * @throws IOException
   * @see {@link #writeTxtFile(String)
   * @see {@link #readSyxFile(String)}
   */
  public void readTxtFile(String fileName)
  throws IOException
  {
    log(1,+2,"Reading TXT file \"%s\" ... ",fileName);
    log(3,"\n");

    // Read text file into a byte array
    String s    = new String(Files.readAllBytes(Paths.get(fileName)));
    String[] as = s.trim().split("\\s++");
    byte[] data = new byte[as.length];
    for (int i=0; i<as.length; i++)
      data[i] = (byte)Integer.parseInt(as[i],16);

    // Read tape from byte array
    String[] warnings = readData(data);

    // Print warnings (if any)
    log(1,-2,"ok%s\n",warnings.length>0?" (with warnings)":"");
    if (warnings.length>0)
    {
      System.err.printf(
        "%d warning%s reading TXT file '%s':\n",
        warnings.length, warnings.length==1 ? "" : "s", fileName 
      );
      for (String warning : warnings)
        System.err.println("- "+warning);
    }
  }

  /**
   * Writes this recorder's tape into a MIDI SysEx file.
   * 
   * @param fileName The SYX file path.   
   * @throws IOException
   * @see {@link #readTxtFile(String)}
   * @see {@link #writeSyxFile(String)} 
   */
  public void writeTxtFile(String fileName)
  throws IOException
  {
    log(1,+2,"Writing TXT file \"%s\" ... ",fileName);

    // Write tape into a string
    String buffer = "";
    for (int i=0; i<this.tape.size(); i++)
    {
      byte[] data = this.tape.get(i).getMessage();
      int    mlen = this.tape.get(i).getLength();
      for (int j=0; j<mlen; j++)
      {
        buffer += String.format("%02X ",data[j]);
        if (j<mlen-1 && (j+1)%15==0) buffer += "\n";
      }
      buffer += "\n\n";
    }

    // Write text file
    Files.write(Paths.get(fileName),buffer.getBytes());
    log(1,-2,"ok\n");
  }

  // -- Helpers ---------------------------------------------------------------

  protected String[] readData(byte[] data)
  {
    log(3,"Reading data buffer ...\n");
    ArrayList<String> warnings = new ArrayList<String>();
    
    // Reset tape
    reset();
    
    // Split byte array into data arrays of single SysEx messages
    int lBOX = -1; // Last SysEx status byte (0xF0)
    int lEOX = -1; // Last SysEx terminator (0xF7)
    for (int i=0; i<data.length; i++)
    {
      log(4,"- %5d: %02X",i,data[i]);
      if (data[i]==(byte)0xF0)
      {
        // SysEx status byte
        log(4," - BOX");
        if (lEOX+1 != i)
        {
          String w = String.format("Byes %d...%s ignored after last EOX",lEOX+1,i-1);
          warnings.add(w);
          log(4," - WARNING: %s",w);
        }
        lBOX = i;
      }
      else if (data[i]==(byte)0xF7)
      {
        // SysEx terminator
        log(4," - EOX ");
        if (lBOX>=0)
        {
          log(3,"- SysEx Message in bytes %s...%d",lBOX,i);
          byte[] mdata = new byte[i-lBOX+1];
          for (int j=0; j<mdata.length; j++)
            mdata[j] = data[lBOX+j];
          try
          {
            SysexMessage sxMsg = new SysexMessage(mdata,mdata.length);
            if (getVerbose()>=3)
            {
              log(3,+2,"\n");
              log(3,"%s",SYX.prettyPrintMidiMessage(sxMsg));
              log(3,-2,"");
            }
            this.tape.add(sxMsg);
          } 
          catch (InvalidMidiDataException e)
          {
            e.printStackTrace();
          }
          log(3,"\n");
        }
        else
        {
          String w = String.format("Byes %d...%s ignored (no BOX found)",lEOX+1,i-1);
          warnings.add(w);
          log(4," - WARNING: %s",w);
        }
        lBOX = -1;
        lEOX = i;
      }
      log(4,"\n");
    }
    if (this.tape.size()==0)
      warnings.add("No SysEx data found in file");
    log(3,"%d bytes read, %d warning%s\n",
      data.length, warnings.size(), warnings.size()==1?"":"s"
    );
    
    return warnings.toArray(new String[0]);
  }

  // -- Implementation of ILogger ---------------------------------------------

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
    return "SyxRec";
  }

}

// EOF