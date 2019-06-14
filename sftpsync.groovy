import com.urbancode.air.CommandHelper;
import com.urbancode.air.AirPluginTool;

final def workDir = new File('.').canonicalFile
def apTool = new AirPluginTool(this.args[0], this.args[1]) //assuming that args[0] is input props file and args[1] is output props file
def props = apTool.getStepProperties();

def scriptBody = []
scriptBody << """\
    \$PSdebug = [System.Environment]::GetEnvironmentVariable("DTC_ANA");\r\n
    if (\$PSdebug -like "PLEASE" ) {\$PSdebug = \$True;Write-Host "--DTC Analyst is waiting.";}else {\$PSdebug = \$False};\r\n
    \$Global:succeedTransferdFiles = 1;\r\n
    \$Global:LocalFilesCount = 0;\r\n
try\r\n
{\r\n
    Add-Type -Path "C:\\Windows\\System32\\WinSCPnet.dll";\r\n
    if (\$PSdebug) {Write-Host "--WinSCP .NET Imported Successfully."};\r\n
}\r\n
catch\r\n
{\r\n
    if (\$PSdebug) {Write-Host "--WinSCP .NET Imported Faild. Agent JRE Setting Might cause this fail, too."};\r\n
}\r\n

function GetFilesMD5\r\n
{\r\n
 param ([Parameter(Mandatory=\$True,Position=0)][string]\$FilePath)\r\n
    \$jsonf += "{`"File`":`"\$FilePath`",`n`r";\r\n
	\$FilePath = \$FilePath.TrimStart('\\\\?\\');\r\n
	\$MD5s = Get-FileHash \"\$FilePath\" -Algorithm MD5;\r\n
	\$MD5String = \$MD5s.Hash;\r\n
    \$jsonf += " `"MD5`":`"\$MD5String`"}"\r\n
    return \$jsonf;\r\n
}\r\n

function CountLocalFiles\r\n
{\r\n
Write-Host -NoNewline "Total: ";\r\n
    \$Global:LocalFilesCount = (Get-ChildItem -Path "${props['LDirectory']}" -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { \$_.PSIsContainer -eq \$false } |  Measure-Object).Count;\r\n
    return \$Global:LocalFilesCount;\r\n
}\r\n
function FileTransferred\r\n
{\r\n
    Param(\$e, \$succeedTransferdFiles)\r\n
    if (\$e.Error -eq \$Null)\r\n
    {\r\n
        Write-Host -NoNewline \$Global:succeedTransferdFiles".. ";\r\n
		\$Global:succeedTransferdFiles++;\r\n
    }\r\n
    else\r\n
    {\r\n
        Write-Host "Transfer Failed!!!`t`r`n";\r\n
    }\r\n
    if (( \$e.Touch -ne \$Null ) -and (\$e.Touch.Error -ne \$Null))\r\n
    {\r\n
            Write-Host -NoNewline "<Touch.Error ";\r\n
    }\r\n
    if ((\$e.Chmod -ne \$Null ) -and (\$e.Chmod.Error -ne \$Null))\r\n
    {\r\n
            Write-Host  -NoNewline "<Chmod.Error";\r\n
    }\r\n
}\r\n
"""
scriptBody << """\
try\r\n
{\r\n
	\$sessionOptions = New-Object WinSCP.SessionOptions;\r\n
    \$sessionOptions.Protocol = [WinSCP.Protocol]::Sftp;\r\n
    \$sessionOptions.UserName = "${props['Username']}";\r\n
    \$sessionOptions.Password = "${props['Password']}";\r\n
    \$sessionOptions.HostName = "${props['HostName']}";\r\n
    \$sessionOptions.PortNumber = 22;\r\n
    \$sessionOptions.GiveUpSecurityAndAcceptAnySshHostKey = "\$True";\r\n
    \$session = New-Object WinSCP.Session;\r\n
    try\r\n
    {\r\n
        \$session.add_FileTransferred( { FileTransferred(\$_) } );\r\n
		try { \$session.Open(\$sessionOptions);}\r\n
        catch { \$session.Output; }\r\n
        if (\$session.Opened) {Write-Host -NoNewLine "Session Opened." ; CountLocalFiles;};\r\n
        \$synchronizationResult = \$session.PutFiles(  "\\\\?\\${props['LDirectory']}", "${props['RDirectory']}", ${props['RemoveFiles']} );\r\n
     }\r\n
     finally\r\n
     {\r\n
        \$synchronizationResult.Check();\r\n
        \$Global:succeedTransferdFiles--;\r\n
		Write-Host -NoNewLine "Total: \$Global:succeedTransferdFiles Files ";\r\n
     }\r\n
     exit 0;\r\n
}\r\n
catch [Exception]\r\n
{\r\n
    \$Global:succeedTransferdFiles--;\r\n
    \$successfulUpload = \$synchronizationResult.Transfers.Count - \$synchronizationResult.Failures.Count;\r\n
    if (\$successfulUpload -ne \$Global:succeedTransferdFiles)\r\n
    { Write-Host "File Transfer is stopped."; };\r\n
    exit 1;\r\n
}\r\n
"""
scriptBody << """\
finally\r\n
{\r\n
    if (\$Global:succeedTransferdFiles -eq 0)\r\n 
	{\r\n
		Write-Host "Version Files which imported was empty. Nothing is deployed."\r\n
	exit 1;\r\n
	}\r\n
	if (\$synchronizationResult.Failures.Count -gt 0 ){Write-Host "Warning: Only \$Global:succeedTransferdFiles of \$Global:LocalFilesCount has been Uploaded."};\r\n
    if (\$session.Opened -eq \$true) {Write-Host  "Successful uploaded:"}\r\n
    \$synchronizationResult.Transfers | Where-Object {\$_.Error -eq \$null }|ForEach-Object {Write-Host \$_.Destination -foregroundcolor Green}\r\n
    if (\$synchronizationResult.Failures.Count -gt 0 )\r\n
    {\r\n
        \$Failures = \$synchronizationResult.Transfers | Where-Object {\$_.Error -ne \$null } | Select-Object -Property FileName;\r\n
        if (\$Failures.FileName -ne \$null ) \r\n
		{\r\n
			\$RemotePath = Split-Path \$Failures.FileName ;\r\n
        Write-Host " Local MD5 Checksum in JSON format, may compare to server-side file.";\r\n
		\$RS = "${props['LDirectory']}";\r\n
		\$RemotePath = \$RemotePath.Replace("\\\\?\\\$RS", "");\r\n
        \$RemotePath = "${props['RDirectory']}" + "\$RemotePath";\r\n
    foreach ( \$Sfilename in \$Failures.FileName)\r\n
    {\r\n
		GetFilesMD5(\$Sfilename);\r\n
        if (\$PSdebug)\r\n
        {\r\n
            \$Sfilename = \$Sfilename.Replace("\\\\?\\", "");\r\n
			\$Sfilename = Get-ChildItem \$Sfilename;\r\n
            \$Suspected = \$RemotePath + "/" + \$Sfilename.Name;\r\n
            \$Suspected = \$Suspected.Replace( "\\", "/" );\r\n
			\$Suspected = \$Suspected.Replace( "//", "/" );\r\n
                \$x = \$session.FileExists(\$Suspected);\r\n
                if ( \$x )\r\n
                {\r\n
                    \$tempfilepath = "${props['LDirectory']}" + "\\" + "CHECKSUMMD5TEMPFILE";\r\n
                    try\r\n
                    {\r\n
						\$Global:succeedTransferdFiles++;\r\n
						\$session.GetFiles( \$Suspected, \$tempfilepath).Check();\r\n
						\$MD5 = Get-FileHash "\$tempfilepath" -Algorithm MD5;\r\n
                        \$MD5String = \$MD5.Hash;\r\n
                        Write-Host "Remote File Found: \$Suspected `r`n  MD5: "\$MD5String .\r\n
                    }\r\n
                    finally\r\n
                    {\r\n
                        Remove-Item \$tempfilepath;\r\n
						\$session.Dispose();\r\n
                    }\r\n
                }\r\n
        }\r\n
    }\r\n
		}\r\n
		else \r\n
		{\r\n
			Write-Host "Unauthorized Remote files may cause this. No checksum or file-compare need." 
		}\r\n
    \$session.Dispose();\r\n
    \$ErrMsg = \$synchronizationResult.Failures;\r\n
    Write-Host "Detail Error Message : `r`n \$ErrMsg";\r\n
    }\r\n
}\r\n
"""
def inputPropsFile = new File(args[0])
def outputPropsFile = new File(args[1])
inputPropsString = '\'' + inputPropsFile.absolutePath + '\''
outputPropsString = '\'' + outputPropsFile.absolutePath + '\''

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
def ScriptPath = scriptData.absolutePath
ScriptPath = "$ScriptPath"

def cmdArgs = []

//def commandPath = props['commandPath'] != ("") ? props['commandPath'] : ""
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

cmdArgs << ";" << "set-executionpolicy unrestricted" << ";" << "[Threading.Thread]::CurrentThread.CurrentUICulture = 'en-US'" << ";"
cmdArgs << scriptData.absolutePath  << inputPropsString << outputPropsString

def exitCode = ch.runCommand("Deploy script Start... SFTP", cmdArgs)


if (exitCode != 0) {
    println "Warning !! Deployment Failed."
    System.exit(exitCode)
}
else {
    println "Deployment successfully."
    System.exit(0)
}