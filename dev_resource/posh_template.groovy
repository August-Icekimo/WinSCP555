/*
 * © Copyright 2012-2020 Micro Focus or one of its affiliates.
 * The only warranties for products and services of Micro Focus and its affiliates and licensors (“Micro Focus”) are set forth in the express warranty statements accompanying such products and services. Nothing herein should be construed as constituting an additional warranty. Micro Focus shall not be liable for technical or editorial errors or omissions contained herein. The information contained herein is subject to change without notice.
 * Contains Confidential Information. Except as specifically indicated otherwise, a valid license is required for possession, use or copying. Consistent with FAR 12.211 and 12.212, Commercial Computer Software, Computer Software Documentation, and Technical Data for Commercial Items are licensed to the U.S. Government under vendor's standard commercial license.
 * DTC coretex consult©  is Micro Focus Partner in Taiwan, and modify this part as custom service for specified customer.
 */

import com.urbancode.air.AirPluginTool
import com.urbancode.air.CommandHelper
import com.urbancode.air.ExitCodeException

final def POWERSHELL_EXE = 'powershell.exe'
final def workDir = new File('.').canonicalFile

def apTool = new AirPluginTool(this.args[0], this.args[1]) //assuming that args[0] is input props file and args[1] is output props file
def props = apTool.getStepProperties()

//Check WinSCP .NET assembly Cmdlet now , Future may come with autoinstall.
File winscpcmdlet = new File('C:\\Windows\\System32\\WinSCPnet.dll');
if(winscpcmdlet.exists() && winscpcmdlet.canRead() ) { 
    /* Found C:\Windows\System32\WinSCPnet.dll */ 
    }
else{
println "Search C:\\Windows\\System32\\WinSCPnet.dll"
println "WinSCP .NET assembly Cmdlet is Missing. "
println "Install WinSCP .NET assembly (With Portable EXE) to C:\\Windows\\System32\\ Please. "
    System.exit(exitCode)
}

def scriptBody = []

// Insert custom script here as scriptBody[]
// Write by HEREDOC, escape happens while redirect
scriptBody << """\
\r\n
"""
scriptBody << """\
\r\n
"""
scriptBody << """\
\r\n
"""

// Makr out empty check, Scriptbody was fill up.
/* if (!scriptBody?.trim()) {
    println 'Script body is empty!'
    System.exit(1)
} */

def commandPath = props['commandPath']

def inputPropsFile = new File(args[0])
def outputPropsFile = new File(args[1])

CommandHelper ch = new CommandHelper(workDir)
ch.ignoreExitValue(true)

//create the script, set it to delete when the step exits, create it and clear it just in case there was somehow some data there.
def curTime = System.currentTimeMillis()
def ScriptDir = new File("../.").getCanonicalPath() //Shift The Script location, I don't want to expose it.
def scriptData = new File(ScriptDir,"DeployScript${curTime}.ps1")
// def scriptData = new File("DeployScript${curTime}.ps1")

scriptData.deleteOnExit()
scriptData.createNewFile()
scriptData.write("")
scriptData.append(scriptBody[0])
scriptData.append(scriptBody[1])
scriptData.append(scriptBody[2])

def cmdArgs = []

def errorHandling = { proc ->
    def out = new PrintStream(System.out, true)
    def err = new StringBuilder()

    proc.out.close() // close stdin

    try {
        proc.waitForProcessOutput(out, err)

        if (err) {
            throw new ExitCodeException("Deploy script failed with error: \n" + err)
        }
    } finally {
        out.flush()
    }
}

if (commandPath) {
    commandPath = commandPath.trim()

    if (commandPath.toLowerCase().endsWith(POWERSHELL_EXE)) {
        cmdArgs << commandPath
    } else {

        while (commandPath.endsWith("\\")) {
            commandPath = commandPath.substring(0, commandPath.length() - 1)
        }

        cmdArgs << commandPath + File.separator + POWERSHELL_EXE
    }
} else {
    cmdArgs << POWERSHELL_EXE
}

cmdArgs << "-executionpolicy" << "unrestricted" << "-command "
cmdArgs << "&('$scriptData.absolutePath') ('$inputPropsFile.absolutePath') ('$outputPropsFile.absolutePath')"

try {
    def exitCode = ch.runCommand("Deploy script Start...", cmdArgs, errorHandling)

    if (exitCode != 0) {
        println "Warning !! Deploy script executed, and Failed."
        System.exit(exitCode)
    } else {
        println "Deploy script executed successfully."
        System.exit(0)
    }
} catch (ExitCodeException e) {
    println "Failed to execute script.\n" + e
    //check if system variable DTC_ANA is set, and println scriptBody.
    if ( env.DTC_ANA == "PLEASE" ) {
        println "Here is debug stuff, enjoy it :"
        println scriptBody[0]
        println scriptBody[1]
        println scriptBody[2]
    }

    System.exit(1)
}