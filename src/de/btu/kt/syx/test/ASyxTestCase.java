package de.btu.kt.syx.test;

import java.io.FileInputStream;
import java.util.Properties;

import javax.sound.midi.MidiUnavailableException;

import de.btu.kt.syx.SYX;
import de.btu.kt.syx.util.ILogger;
import de.btu.kt.syx.util.MidiInterface;

public abstract class ASyxTestCase implements ILogger
{

  // -- Attributes ------------------------------------------------------------

  /**
   * Log ID
   */
  private static final String LOG_ID = "TEST";

  /**
   * Static logger
   */
  private static final ILogger logger = new ILogger()
  {

    @Override
    public String getLogID()
    {
      return LOG_ID;
    }

    @Override
    public int getVerbose()
    {
      return 0;
    }
    
  };

  /**
   * MIDI interfaces at photoneGATE2 studio (fallback).
   */
  private static String[] knownMidiInterfaces = new String[] {
    "Komplete Audio 6 MK2 MIDI",
    "KOMPLETE KONTROL EXT - 1"  
  };
  
  /**
   * The test case properties
   */
  private static Properties appProps = null;

  // -- Getters ---------------------------------------------------------------

  /**
   * Returns the test case property with the specified key.
   * 
   * <p>The test case properties are loaded from the file {@code 
   * de/btu/kt/syx/test/test.properties} in the source folder as obtained by
   * {@code SYX.}{@link SYX#getPath(Class, String, int) getPath}{@code
   * (ASyxTestCase.class,"test.properties",SYX.SOURCE)}.</p>
   * 
   * @param key
   *          The key
   * @return The property value or {@code null} if no such property exists
   */
  public static String getProperty(String key)
  {
    if (ASyxTestCase.appProps==null)
      try
      {
        ASyxTestCase.appProps = new Properties();
        logger.log("Loading test configuration ...\n");
        String fn = SYX.getPath(ASyxTestCase.class,"test.properties",SYX.SOURCE);
        logger.log("- File: %s\n",fn);
        ASyxTestCase.appProps.load(new FileInputStream(fn));
        logger.log(0,"ok\n");
      }
      catch (Error|Exception e)
      {
        logger.errlog("  "+e.getMessage()+"\nFAILED\n");
      }

    return ASyxTestCase.appProps.getProperty(key);
  }

  public static String getMidiInterfaceName()
  throws MidiUnavailableException
  {
    // The happy path: Get MIDI interface name from configuration file
    String mi = ASyxTestCase.getProperty("midi.interface");
    if (mi!=null)
      return mi;

    // Fallback: Try interfaces at my site
    logger.errlog("Property midi.interface not found\n");
    logger.log("Trying known MIDI interfaces ...");
    for (String kmi : ASyxTestCase.knownMidiInterfaces)
      try
      {
        MidiInterface.findMidiInPort(kmi);
        MidiInterface.findMidiOutPort(kmi);
        logger.log(0," ok ('%s')\n",kmi);
        return kmi;
      }
      catch (MidiUnavailableException e2)
      { // jo mei...
      }
    logger.errlog(0," FAILED\n");
    throw new MidiUnavailableException("No MIDI interface found");
  }

  // -- Implementation of ILogger ---------------------------------------------

  @Override
  public String getLogID()
  {
    return LOG_ID;
  }

  @Override
  public int getVerbose()
  {
    return 0;
  }

}

// EOF