# SYX - Handles MIDI System Exclusive Data

[JavaDoc](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/index.html) - 
[Getting Started](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/tutorials/GettingStarted.html) - 
[The Tutorials](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/tutorials/package-summary.html)

SYX is a Java package which you can use to develop your own MIDI system exclusive communications application. SYX is  _not_  a SysEx librarian.

## The Idea

MIDI system exclusive messages (see \[1\] for a comprehensive introduction) contain a device-specific byte array with a skeleton of fix bits and variable bits (parameters). Consider, for example, this message:

`0xF0 0x43 0x10 0x35 0x7F 0x00 0x00 0x00 0x00 0x03 0xF7`

It is an error messages from a Yamaha SY55 or TG55 synthesizer and means "MIDI Data Error" on SY55/TG55 device #1 (see \[2\], pp. Add-16,17).

With SYX, we can describe the message data content (excluding the leading status byte `0xF0` and the tailing EOX byte `0xF7`) by a [format specifier](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html), in our example

`"43H 1#H 35H 7FH 00H 00H 00H 00H nnH",`

which defines the "skeleton" and the two variables of the error message: `'#'` for the 4 bit device number, and `'n'` for the 7 bit error code.

Then we can easily access the message parameters as follows:

*   [`getMidiValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#getMidiValue(char))`('#')` or [`setMidiValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setMidiValue(char,int))`('#',0x00)`, and
*   [`getMidiValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#getMidiValue(char))`('n')` or [`setMidiValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setMidiValue(char,int))`('n',0x03)`.

The setters automatically prevent from setting parameter values which are negative or too large to fit into the parameter bits.

## Basic Features

*   **MIDI to Model Value Maps** – Sometimes the "MIDI value" in the system exclusive message does not equal the "model value" as displayed on the synthesizer. SYX supports mapping between MIDI and model values. For instance, the device number parameter `'#'` of the SY55/TG55 error message above can take MIDI values ∈ \[ 0x00, 0x0F \]. However, the displayed model value takes values ∈ \[ 1, 16 \]. Hence, we need a [MIDI-to-model value map](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxParamInfo.html#setValueMap(int%5B%5D,int%5B%5D)) { 0x00 ↦ 1, 0x01 ↦ 2, ..., 0x0F ↦ 16 }. We can access the model value of the parameter as follows:
    *   [`getModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#getModelValue(char))`('#')` or [`setModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setModelValue(char,int))`('#',1)`
*   **MIDI Value to Value Name Maps** – Model values can be strings. For instance, the error number parameter `'n'` of the SY55/TG55 error message above is displayed on the synthesizer as an error description. Hence, we need a [MIDI value to value name map](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxParamInfo.html#setValueMap(int%5B%5D,java.lang.String%5B%5D)) { 0x01 ↦ "MIDI Buffer Full", 0x02 ↦ "SEQ Buffer Full", ... }. We can access the value name of the parameter as follows:
    *   [`getModelValueAsString`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#getModelValueAsString(char))`('n')` or [`setModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setModelValue(char,java.lang.String))`('n',"MIDI Buffer Full")`
*   **Restricted MIDI Value Sets** – In some cases not all _n_\-bit MIDI values are permissible for a parameter. For instance, the error number parameter `'n'` of the SY55/TG55 error message above has 7 bits, i.e., a potential value range of \[ 0x00, 0x7F \], but its permissible values are only ∈ \[ 0x01, 0x20 \]. A value map as described above must only contain the permissible MIDI values. If we try to set an _invalid_ MIDI or model value, e.g,
    
    *   [`setMidiValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setMidiValue(char,int))`('n',0x40)` or
    *   [`setModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxDataStruct.html#setModelValue(char,java.lang.String))`('n',"nonsense")`,
    
    the setters will throw an [`InvalidMidiDataException`](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/InvalidMidiDataException.html) which constitutes parameter value validation.
*   **Message Property Sets** – In large system exclusive messages (such as bulk dumps), accessing the parameters as shown above may become awkward. Therefore, SYX supports unqiue property names for the parameters of a message. Then parameter access looks as follows:
    *   [`getModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxMessage.html#getModelValue(java.lang.String))`("DEVICE_NO")` or [`setModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxMessage.html#setModelValue(java.lang.String,int))`("DEVICE_NO",1)`, or
    *   [`getModelValueAsString`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxMessage.html#getModelValueAsString(java.lang.String))`("ERROR_MSG")` or [`setModelValue`](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxMessage.html#setModelValue(java.lang.String,java.lang.String))`("ERROR_MSG","MIDI Buffer Full")`.
*   **Checksums** – SysEx bulk data usually contain checksums. SYX can [compute](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxChecksum.html#computeCheckSum(de.btu.kt.syx.midi.SyxMessage)) and [validate](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/SyxChecksum.html#validateCheckSum(de.btu.kt.syx.midi.SyxMessage)) checksums.
*   **Parameter Change Listeners** – Changes of parameter values can be monitored by [MIDI data change listeners](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/ISyxDataChangeListener.html).
*   **SysEx Message Listeners** – SYX features a [MIDI interface wrapper](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/MidiInterface.html) with which [listeners to incoming system exclusive messages](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/midi/ISysexMessageListener.html) can be registered.

## Advanced Features

*   **Device Models** – SYX features [device models](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/devices/package-summary.html) which represent the state of a hardware synthesizer (as far as can be obtained through SysEx communication). The state may include [patch data](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/devices/IPatch.html), [multi data](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/devices/IMulti.html), system settings, [patch and multi banks](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/devices/BankTree.html), etc. Device models can also encapsulate (device-specific) SysEx parameter change messages.
*   **SysEx Data Tapes and SYX Files** – SYX provides a "[SysEx recorder](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/SysexRecorder.html)" which can [record](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/SysexRecorder.html#record(javax.sound.midi.SysexMessage)) and [play back](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/SysexRecorder.html#play()) "tapes" containing sequences of system exclusive messages. The tapes can be [written to](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/SysexRecorder.html#writeSyxFile(java.lang.String)) and [read from](https://www-docs.b-tu.de/fg-kommunikationstechnik/public/matthias.wolff/JavaDoc/SYX/de/btu/kt/syx/util/SysexRecorder.html#readSyxFile(java.lang.String)) SYX or TXT files.

## References

* \[1\] Martin Russ: Everything You Ever Wanted To Know About System Exclusive. Sound On Sound, April 1989. [Online](https://www.muzines.co.uk/articles/everything-you-ever-wanted-to-know-about-system-exclusive/4539), retrieved May 12, 2026

* \[2\] Yamaha Corp.: TG55 Operating Manual. [Online](https://data.yamaha.com/files/download/other_assets/9/316979/TG55G.pdf), retrieved May 12, 2026

## See Also

* [SYX.Devices](https://github.com/matthias-wolff/SYX.Devices): Device-Specific MIDI SyxEx Data Models for SYX
