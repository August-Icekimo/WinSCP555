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
if (\$PSdebug) {Write-Host "--WinSCP .NET Imported Successfully"}\r\n
else{\r\n
    \$Total = Get-ChildItem -Path "${props['LDirectory']}" -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { \$_.PSIsContainer -eq \$false } |  Measure-Object | Select-Object -ExpandProperty Count;\r\n
Write-Host "\$Total Files Counted, Now Upload Files(SFTP Advance)....";\r\n
}\r\n
function FileTransferred \r\n
{\r\n
    Param(\$e, \$succeedTransferdFiles)\r\n
    # Show synchronizationResult.transfer\r\n
    if (\$e.Error -eq \$Null)\r\n
    {\r\n
        Write-Host ("`r`n\$global:succeedTransferdFiles`t{0} Upload Succeeded." -f \$e.Destination);\r\n
    }\r\n
    else\r\n
    {\r\n
        Write-Host ("`r`nUpload Failed:`t`r`n {0} `r`n {1}" -f \$e.Filename, \$e.Error);\r\n
        \$global:succeedTransferdFiles--;\r\n
    }\r\n
    if (\$e.Chmod -ne \$Null)\r\n
    {\r\n
        if (\$e.Chmod.Error -eq \$Null)\r\n
        {\r\n
            Write-Host ("`t-->Permisions set to {0}" -f \$e.Chmod.FilePermissions)};\r\n
        else\r\n
        {\r\n
            Write-Host ("`t-->Setting permissions failed:`r`n {0}" -f  \$e.Chmod.Error);\r\n
        }\r\n
    }\r\n
    else\r\n
    {\r\n
         Write-Host ("`t-->Permissions kept with site defaults");\r\n
    }\r\n
    if (\$e.Touch -ne \$Null)\r\n
    {\r\n
        if (\$e.Touch.Error -eq \$Null)\r\n
        {\r\n
            Write-Host ("`t-->Timestamp set to {0}" -f \$e.Touch.LastWriteTime);\r\n
        }\r\n
        else\r\n
        {\r\n
            Write-Host ("`t-->Setting timestamp failed:`r`n {0}" -f \$e.Touch.Error);\r\n
        }\r\n
    }\r\n
    else\r\n
    {\r\n
        # This should never happen with Session.SynchronizeDirectories\r\n
        Write-Host ("`t-->Timestamp kept with its default (current time)");\r\n
    }\r\n
            \$global:succeedTransferdFiles++ ;\r\n
}\r\n
"""
scriptBody << """\
# Main script\r\n\
 \r\n\
try\r\n\
{\r\n\
    \$sessionOptions = New-Object WinSCP.SessionOptions;\r\n\
    \$sessionOptions.Protocol = [WinSCP.Protocol]::Sftp;\r\n\
    \$sessionOptions.UserName = "${props['Username']}";\r\n\
    \$sessionOptions.Password = "${props['Password']}";\r\n\
    \$sessionOptions.HostName = "${props['HostName']}";\r\n\
    \$sessionOptions.PortNumber = 22;\r\n\
	\$sessionOptions.GiveUpSecurityAndAcceptAnySshHostKey = "\$True" ;\r\n\
"""
scriptBody << """\
	${props['SessionOptionsAddRawSettings']};\r\n\
"""
scriptBody << """\
    \$session = New-Object WinSCP.Session;\r\n\
    try\r\n\
    {\r\n\
        # Will continuously report progress of synchronization;\r\n\
        \$session.add_FileTransferred( { FileTransferred(\$_) } );\r\n\
        \$session.Open(\$sessionOptions);\r\n\
		\$transferOptions = New-Object WinSCP.TransferOptions;\r\n\
		\$transferOptions.FilePermissions = \$Null;\r\n\
		\$transferOptions.PreserveTimestamp = \$False;\r\n\
        \$synchronizationResult = \$session.PutFiles(\r\n\
            "\\\\?\\${props['LDirectory']}", "${props['RDirectory']}", ${props['RemoveFiles']},\$transferOptions);\r\n\

        \$synchronizationResult.Check();\r\n\
		\$global:succeedTransferdFiles--;\r\n
	if (\$global:succeedTransferdFiles -eq 0)\r\n 
	{\r\n
	Write-Host "Version Files which imported was empty. Nothing is deployed."\r\n
	exit 1;\r\n
	}\r\n
		Write-Host "\$global:succeedTransferdFiles Files has been Uploaded.";\r\n\
    }\r\n\
"""
scriptBody << """\
    finally { \$session.Dispose(); }\r\n
     exit 0;\r\n
}\r\n
catch [Exception]\r\n
{\r\n
    Write-Host \$_.Exception.Message;\r\n
    \$global:succeedTransferdFiles--;\r\n
    Write-Host "Total: \$global:succeedTransferdFiles of \$Total Files has been Uploaded, there May got Some Files lost on the way.";\r\n
    exit 1;\r\n
}\r\n
"""
	if (showScript) { 	println scriptBody 	}

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

	if (showScript) {println " Synchronization Script is Created in " + ScriptPath}

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
		println "Final PowerShell Command is "  + cmdArgs
	}
def exitCode = ch.runCommand("WinSCP .NET assembly Synchronization Start...", cmdArgs)


if (exitCode != 0) {
    println "Synchronization Failed"
    System.exit(exitCode)
}
else {
    println "Synchronization successfully."
    System.exit(0) 
}