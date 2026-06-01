package de.btu.kt.syx.test;

/**
 * Yamaha TG55 SyxEx message format specifiers (for test cases and tutorials).
 * 
 * @author Matthias Wolff
 */
public final class TG55FormatSpecifiers
{

  /**
   * This class cannot be instantiated.
   */
  private TG55FormatSpecifiers()
  {
  }

  // Yamaha TG55 SysEx Format Specifiers

  /**
   * Parameter change message w/ parameter 's'.
   */
  public static final String F_TG55_PARCNG1 
    = "43H 1#H 35H 0sH ssH ppH[2] vvH[2]";

  /**
   * Parameter change message w/ parameters 't', 'f', 'e' and 'c'.
   */
  public static final String F_TG55_PARCNG2
    = "43H 1#H 35H 0tH 0feecccc ppH[2] vvH[2]";

  /**
   * Bulk request message.
   */
  public static final String F_TG55_BULK_REQUEST
    = "43H 2#H 7AH 4CH 4DH 20H[2] 38H 31H 30H 33H ssH ttH 00H[14] xxH yyH";

  /**
   *  Bulk header 1 message part.
   */
  public static final String F_TG55_BD_HEADER1
    = "43H 0#H 7AH bbH[2]";

  /**
   *  Bulk header 2 message part.
   */
  public static final String F_TG55_BD_HEADER2
    = "4CH 4DH 20H[2] 38H 31H 30H 33H ssH ttH 00H[14] xxH yyH";

  /**
   * Multi bulk header message part.
   */
  public static final String F_TG55_BDMU_HEADER
    = "aaH bbH ccH ddH eeH ffH ggH hhH iiH jjH 000sssss";

  /**
   * Multi effect message part.
   */
  public static final String F_TG55_BDMU_EFFECT
    = "ttH llH ppH qqH rrH";

  /**
   * Multi voice message part.
   */
  public static final String F_TG55_BDMU_VOICE
    = "0a000ooo 0000000m 00nnnnnn vvH ttH ssH 00pppppp eeH 0rH";

  /**
   * Voice bulk header message part.
   */
  public static final String F_TG55_BDVC_HEADER
    = "**H aaH bbH ccH ddH eeH ffH ggH hhH iiH jjH";

}

// EOF