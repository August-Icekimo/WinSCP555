import com.urbancode.air.CommandHelper;
import com.urbancode.air.AirPluginTool;

final def workDir = new File('.').canonicalFile
def showScript = false // Open the Debug Mode
def apTool = new AirPluginTool(this.args[0], this.args[1]) //assuming that args[0] is input props file and args[1] is output props file
def props = apTool.getStepProperties();

//Check WinSCP .NET assembly Cmdlet now , Future may come with autoinstall.
File winscpcmdlet = new File('C:\\Windows\\System32\\WinSCPnet.dll');
if(winscpcmdlet.exists() && winscpcmdlet.canRead() ) { /* Found C:\Windows\System32\WinSCPnet.dll */ }
else{
println "Search C:\\Windows\\System32\\WinSCPnet.dll"
println "WinSCP .NET assembly Cmdlet is Missing. "
println "Install WinSCP .NET assembly (With Portable EXE) to C:\\Windows\\System32\\ Please. "
    System.exit(exitCode)
}

def scriptBody = []
scriptBody << """\
    # \$PSdebug = \$True ;\r\n
    \$global:succeedTransferdFiles = 1;\r\n
if (\$PSdebug) {Write-Host "--DeBug Mode is On"};\r\n
Add-Type -Path "C:\\Windows\\System32\\WinSCPnet.dll";\r\n
if (\$PSdebug) {Write-Host "--WinSCP .NET Imported Successfully"}else{Write-Host "Uploading..."};\r\n
function FileTransferred\r\n
{\r\n
    Param(\$e, \$succeedTransferdFiles)\r\n
    # Show synchronizationResult.transfer;\r\n
    if (\$e.Error -eq \$Null)\r\n
    {\r\n
        Write-Host ("\$global:succeedTransferdFiles`t{0} Upload Succeeded.`r`n" -f \$e.Destination);\r\n
		\$global:succeedTransferdFiles++ ;\r\n
    }\r\n
    else\r\n
    {\r\n
        Write-Host ("Upload Failed:`t`r`n {0} `r`n {1}" -f \$e.Filename, \$e.Error);\r\n
    }\r\n
    if (\$e.Chmod -ne \$Null)\r\n
    {\r\n
        if (\$e.Chmod.Error -eq \$Null)\r\n
        {\r\n
             if (\$PSdebug) {Write-Host ("-->Permisions of {0} set to {1}" -f \$e.Chmod.FileName, \$e.Chmod.FilePermissions)};\r\n
        }\r\n
        else\r\n
        {\r\n
            Write-Host ("-->Setting permissions of {0} failed:`r`n {1}" -f \$e.Chmod.FileName, \$e.Chmod.Error);\r\n
        }\r\n
    }\r\n
    else\r\n
    {\r\n
         if (\$PSdebug) {Write-Host ("-->Permissions of {0} kept with their defaults" -f \$e.Destination)};\r\n
    }\r\n
    if (\$e.Touch -ne \$Null)\r\n
    {\r\n
        if (\$e.Touch.Error -eq \$Null)\r\n
        {\r\n
             if (\$PSdebug) {Write-Host ("-->Timestamp of {0} set to {1}" -f \$e.Touch.FileName, \$e.Touch.LastWriteTime)};\r\n
        }\r\n
        else\r\n
        {\r\n
            Write-Host ("-->Setting timestamp of {0} failed:`r`n {1}" -f \$e.Touch.FileName, \$e.Touch.Error);\r\n
        }\r\n
    }\r\n
    else\r\n
    {\r\n
        # This should never happen with Session.SynchronizeDirectories\r\n
         if (\$PSdebug) {Write-Host ("-->Timestamp of {0} kept with its default (current time)" -f \$e.Destination)};\r\n
    }\r\n
}\r\n


"""
scriptBody << """\
# Main script\r\n
try\r\n
{\r\n
    \$sessionOptions = New-Object WinSCP.SessionOptions;\r\n
    \$sessionOptions.Protocol = [WinSCP.Protocol]::Ftp;\r\n
    \$sessionOptions.FtpSecure = "${props['FtpSecure']}";\r\n
    \$sessionOptions.UserName = "${props['Username']}";\r\n
    \$sessionOptions.Password = "${props['Password']}";\r\n
    \$sessionOptions.HostName = "${props['HostName']}";\r\n
    \$sessionOptions.PortNumber = "${props['PortNumber']}";\r\n
    \$sessionOptions.GiveUpSecurityAndAcceptAnyTlsHostCertificate = "\$True";\r\n
"""
scriptBody << """\
	\$sessionOptions.AddRawSettings("SslSessionReuse", "1")	;\r\n\
	${props['SessionOptionsAddRawSettings']};\r\n\
"""
scriptBody << """\
    \$session = New-Object WinSCP.Session;\r\n\
    try\r\n
    {\r\n
        # Will continuously report progress of synchronization;\r\n
        \$session.add_FileTransferred( { FileTransferred(\$_) } );\r\n
        \$session.Open(\$sessionOptions);\r\n
        \$synchronizationResult = \$session.PutFiles(\r\n
            "\\\\?\\${props['LDirectory']}", "${props['RDirectory']}", ${props['RemoveFiles']});\r\n
        \$synchronizationResult.Check();\r\n
        \$global:succeedTransferdFiles--;\r\n
		Write-Host "Total: \$global:succeedTransferdFiles Files has been Uploaded.";\r\n
    }\r\n
"""
scriptBody << """\
    finally\r\n
    {\r\n
        \$session.Dispose();\r\n
    }\r\n
     exit 0;\r\n
}\r\n
catch [Exception]\r\n
{\r\n
    Write-Host \$_.Exception.Message;\r\n
    \$global:succeedTransferdFiles--;\r\n
    Write-Host "Total: \$global:succeedTransferdFiles Files has been Uploaded, there May got Some Files lost on the way.";\r\n
    exit 1;\r\n
}
"""
	if (showScript) { 
	println scriptBody 
	}
def inputPropsFile = new File(args[0])
def outputPropsFile = new File(args[1])

CommandHelper ch = new CommandHelper(workDir)
ch.ignoreExitValue(true)

//create the script, set it to delete when the step exits, create it and clear it just in case there was somehow some data there.
def curTime = System.currentTimeMillis()
def ScriptDir = new File("../.").getCanonicalPath() //Shift The Script location, I don't want to expose it.
def scriptData = new File(ScriptDir,"Script${curTime}.ps1")
scriptData.deleteOnExit()
scriptData.createNewFile()
scriptData.write("")
scriptData.append(scriptBody[0])
scriptData.append(scriptBody[1])
scriptData.append(scriptBody[2])
scriptData.append(scriptBody[3])
scriptData.append(scriptBody[4])
def ScriptPath = scriptData.absolutePath
ScriptPath = "$ScriptPath"

if (showScript) {println "Synchronization Script is Created in " + ScriptPath}

def cmdArgs = []

def commandPath = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0" // PowerShell Path

if (!commandPath.equals("")) {
    commandPath.trim()
    while (commandPath.endsWith("\\")) {
        commandPath = commandPath.substring(0, commandPath.length-1)
    }
    cmdArgs << commandPath + File.separator + "powershell.exe"
}

else {
    cmdArgs << "powershell.exe"
}

cmdArgs << "-ExecutionPolicy" << "Bypass" << "-File" 
//cmdArgs << \"scriptData.absolutePath\" << inputPropsFile.absolutePath << outputPropsFile.absolutePath

cmdArgs << ScriptPath 
	if (showScript) {
		println "ScriptPath is : " + ScriptPath
		println "Final PwerShell Command is "  + cmdArgs
	}
def exitCode = ch.runCommand("Deploy Start... Reuse SSL Session", cmdArgs)


if (exitCode != 0) {
    println "Deployment Failed"
    System.exit(exitCode)
}
else {
    println "Deployment successfully."
    System.exit(0) 
}