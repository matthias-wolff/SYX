/**
 * <h1 style="margin-top:0">Tutorials</h1>
 * <p>I assume that you are familiar with MIDI system exclusive messages. If
 * not, you find a comprehensive introduction <a
 * href="https://www.muzines.co.uk/articles/everything-you-ever-wanted-to-know-about-system-exclusive/4539"
 * >here</a>.</p>
 * 
 * <h2 style="margin-top:1em">Getting Started</h2>
 * <ol>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.GettingStarted#createMI() Creating a MIDI Interface}</li>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.GettingStarted#sendSysEx() Sending a System Exclusive Message}</li>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.GettingStarted#receiveSysEx() Receiving a System Exclusive Message}</li>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.GettingStarted#addParamInfo() Adding Parameter Information}</li>
 * </ol>
 * 
 * <h2 style="margin-top:1em">Handling Bulk Data</h2>
 * <ol>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.HandlingBulkData#fetchPatchName() Fetching a Patch Name}</li>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.HandlingBulkData#createSimplePatchBank() Creating a Simple Patch Bank}</li>
 *   <li>{@linkplain de.btu.kt.syx.tutorials.HandlingBulkData#syxFiles() Reading and Writing SysEx Files}</li>
 * </ol>
 * 
 * <h2 style="margin-top:1em">Advanced Coding Examples</h2>
 * <ol>
 *   <li>{@linkplain de.btu.kt.syx.devices.vz1.VZ1Patch A Patch Model for Casio VZ-1/10M}</li>
 *   <li>{@linkplain de.btu.kt.syx.devices.tg55.TG55 A Multi Model for Yamaha SY55/TG55}</li>
 *   <li>{@linkplain de.btu.kt.syx.apps.vz1.VZ1PatchAppc A Test App for VZ-1/10M Patches}</li>
 *   <li>{@linkplain de.btu.kt.syx.apps.vz1.VZ1PatchBankAppc A Test App for VZ-1/10M Patch Banks}</li>
 * </ol>
 * 
 * @author Matthias Wolff
 */
package de.btu.kt.syx.tutorials;

// EOF