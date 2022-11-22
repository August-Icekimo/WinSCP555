def scriptBody = []
scriptBody << """\
\$PSdebug = [System.Environment]::GetEnvironmentVariable("DTC_ANA")\r\n
if (\$PSdebug -like "PLEASE" ) \r\n
{\r\n
    \$PSdebug = \$True;Write-Host "--DTC Analyst is waiting."\r\n
}\r\n
else \r\n
{\r\n
    \$PSdebug = \$False\r\n
}\r\n
    \$PSdebug = \$True\r\n
\$Global:succeedTransferdFiles = 1\r\n
\$Global:LocalFilesCount = 0\r\n
try\r\n
{\r\n
    Add-Type -Path "C:\\Windows\\System32\\WinSCPnet.dll"\r\n
    if (\$PSdebug) \r\n
    {\r\n
        Write-Host " 00 --WinSCP .NET Imported Successfully."\r\n
    }\r\n
}\r\n
catch\r\n
{\r\n
    if (\$PSdebug)\r\n
    {\r\n
        Write-Host " 00 --WinSCP .NET Imported Faild. Agent JRE Setting Might cause this fail, too."\r\n
    }\r\n
}\r\n
function GetFilesMD5\r\n
{\r\n
 param ([Parameter(Mandatory=\$True,Position=0)][string]\$FilePath)\r\n
    \$jsonf += "{`"File`":`"\$FilePath`",`n`r"\r\n
        \$FilePath = \$FilePath.TrimStart('\\\\?\\')\r\n
        \$MD5s = Get-FileHash "\$FilePath" -Algorithm MD5\r\n
        \$MD5String = \$MD5s.Hash\r\n
    \$jsonf += " `"MD5`":`"\$MD5String`"}"\r\n
    return \$jsonf\r\n
}\r\n
function CountLocalFiles\r\n
{\r\n
    Write-Host -NoNewline " 04 Total: "\r\n
    \$Global:LocalFilesCount = (Get-ChildItem -Path "\${props[LDirectory]}" -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { \$_.PSIsContainer -eq \$False } |  Measure-Object).Count\r\n
    Write-Host -NoNewline "\$Global:LocalFilesCount files count ..."\r\n
    return \$Global:LocalFilesCount\r\n
}\r\n
function FileTransferred\r\n
{\r\n
    Param(\$e, \$succeedTransferdFiles)\r\n
    if (\$Null -eq \$e.Error)\r\n
    {\r\n
        Write-Host -NoNewline "\$Global:succeedTransferdFiles.."\r\n
                \$Global:succeedTransferdFiles++\r\n
    }\r\n
    else\r\n
    {\r\n
        Write-Host " 90 Transfer Failed !!!`t`r`n"\r\n
    }\r\n
    if ((\$Null -ne \$e.Touch ) -and (\$Null -ne \$e.Touch.Error))\r\n
    {\r\n
        Write-Host -NoNewline "<Touch.Error "\r\n
    }\r\n
    if ((\$Null -ne \$e.Chmod ) -and (\$Null -ne \$e.Chmod.Error))\r\n
    {\r\n
        Write-Host  -NoNewline "<Chmod.Error"\r\n
    }\r\n
}\r\n

"""
scriptBody << """\
try\r\n
{\r\n
        \$sessionOptions = New-Object WinSCP.SessionOptions\r\n
    \$sessionOptions.Protocol = [WinSCP.Protocol]::Sftp\r\n
    \$sessionOptions.UserName = "\${props[Username]}"\r\n
    \$sessionOptions.Password = "\${props[Password]}"\r\n
    \$sessionOptions.HostName = "\${props[HostName]}"\r\n
    \$sessionOptions.PortNumber = "\${props[PortNumber]}"\r\n
    \$sessionOptions.GiveUpSecurityAndAcceptAnySshHostKey = "True"\r\n
    \$session = New-Object WinSCP.Session\r\n
    try\r\n
    {\r\n
        \$session.add_FileTransferred( { FileTransferred(\$_) } )\r\n
        Write-Host " 00 Session options loaded ..."\r\n
        if (\$PSdebug)\r\n
        {\r\n
            Write-Host " (\$sessionOptions ) "\r\n
        }\r\n
        try \r\n
        {\r\n
            Write-Host " 01 Now opening Session to \${props[HostName]} ..."\r\n
            \$session.Open(\$sessionOptions)\r\n
        }\r\n
        catch \r\n
        {\r\n
            Write-Host (\$session.Output | Out-String)\r\n
        }\r\n
        if (\$session.Opened)\r\n
        {\r\n
            Write-Host " 02 Session Opened ..."\r\n
            CountLocalFiles\r\n
            Write-Host " 05 Uploading.."\r\n
        }\r\n
        \$synchronizationResult = \$session.PutFiles(  "\\\\?\\\${props[LDirectory]}", "\${props[RDirectory]}", 0 )\r\n
    }\r\n
    finally\r\n
    {\r\n
        \$synchronizationResult.Check()\r\n
        \$Global:succeedTransferdFiles--\r\n
        Write-Host " 06 Total: \$Global:succeedTransferdFiles Files."\r\n
    }\r\n
    exit 0\r\n
}\r\n
catch [Exception]\r\n
{\r\n
    Write-Host " 91 Exception caught. Something went wrong."\r\n
    \$Global:succeedTransferdFiles--;\r\n
    \$successfulUpload = \$synchronizationResult.Transfers.Count - \$synchronizationResult.Failures.Count\r\n
    if (\$successfulUpload -ne \$Global:succeedTransferdFiles)\r\n
    { \r\n
        Write-Host " 92 File transfer stopped, and files count not match."\r\n
    }\r\n
    \$session.Dispose()\r\n
    \$ErrMsg = \$synchronizationResult.Failures\r\n
    Write-Host " 94 Detail Error Message : `r`n \$ErrMsg"\r\n
    if (\$PSdebug)\r\n
    {\r\n
        New-EventLog –LogName Application –Source “Deployment Automation”\r\n
        Write-EventLog –LogName Application –Source “Deployment Automation” –EntryType Error –EventID 2 –Message “\\\$ErrMsg"\r\n
        Remove-EventLog –Source “Deployment Automation"\r\n
    }\r\n
    exit 1\r\n
}\r\n
"""

scriptBody << """\
finally\r\n
{\r\n
    if (\$session.Opened -eq \$True) \r\n
    {\r\n
        Write-Host  " 07 Successful uploaded: \$Global:succeedTransferdFiles ... "\r\n
    }\r\n
    else\r\n
    {\r\n
        if (\$Global:succeedTransferdFiles -eq 0)\r\n
        {\r\n
            \r\n
            Write-Host "Nothing is deployed. Maybe version files imported was empty. "\r\n
            Write-Host "\$Global:succeedTransferdFiles of \$Global:LocalFilesCount Count."\r\n
            if (\$PSdebug)\r\n
            {\r\n
                Write-Host (\$session.Output | Out-String)\r\n
            }\r\n
        }\r\n
    }\r\n
    \$synchronizationResult.Transfers | Where-Object {\$Null -eq \$_.Error }|ForEach-Object {Write-Host \$_.Destination -foregroundcolor Green}\r\n
    \r\n
        if (\$synchronizationResult.Failures.Count -gt 0 )\r\n
    {\r\n
        Write-Host " 07 Warning: Only \$Global:succeedTransferdFiles of \$Global:LocalFilesCount has been uploaded."\r\n
        if (\$PSdebug)\r\n
        {\r\n
            \$Failures = \$synchronizationResult.Transfers | Where-Object {\$Null -ne \$_.Error } | Select-Object -Property FileName\r\n
            if (\$Null  -ne \$Failures.FileName ) \r\n
            {\r\n
                \$RemotePath = Split-Path \$Failures.FileName\r\n
                Write-Host " Local MD5 Checksum in JSON format, may compare to server-side file."\r\n
                \$RS = "\${props[RDirectory]}"\r\n
                \$RemotePath = \$RemotePath.Replace("\\\\?\\\$RS", "")\r\n
                \$RemotePath = "\\" + "\$RemotePath"\r\n
                foreach ( \$Sfilename in \$Failures.FileName)\r\n
                {\r\n
                    GetFilesMD5(\$Sfilename)\r\n
                    if (\$PSdebug)\r\n
                    {\r\n
                        \$Sfilename = \$Sfilename.Replace("\\\\?\\", "")\r\n
                        \$Sfilename = Get-ChildItem \$Sfilename\r\n
                        \$Suspected = \$RemotePath + "/" + \$Sfilename.Name\r\n
                        \$Suspected = \$Suspected.Replace( "\\", "/" )\r\n
                        \$Suspected = \$Suspected.Replace( "//", "/" )\r\n
                        \$x = \$session.FileExists(\$Suspected)\r\n
                        if ( \$x )\r\n
                        {\r\n
                            \$tempfilepath = "\${props[RDirectory]}" + "\\" + "CHECKSUMMD5TEMPFILE"\r\n
                            try\r\n
                            {\r\n
                                \$Global:succeedTransferdFiles++\r\n
                                \$session.GetFiles( \$Suspected, \$tempfilepath).Check()\r\n
                                \$MD5 = Get-FileHash "\$tempfilepath" -Algorithm MD5\r\n
                                \$MD5String = \$MD5.Hash\r\n
                                Write-Host "Remote File Found: \$Suspected `r`n  MD5: \$MD5String ."\r\n
                            }\r\n
                            finally\r\n
                            {\r\n
                                Remove-Item \$tempfilepath\r\n
                            }\r\n
                        }\r\n
                    }\r\n
                }\r\n
            }\r\n
            else \r\n
            {\r\n
                Write-Host "Unauthorized Remote file access may cause this. No checksum or file-compare need." \r\n
            }\r\n
        }\r\n
    }\r\n
    \$session.Dispose()\r\n
    Write-Host " 99 Transfer Session Closed."\r\n
}\r\n
"""
//def ScriptDir = new File("../.").getCanonicalPath() //Shift The Script location, I don't want to expose it.
// def ScriptDir = new File("./.").getCanonicalPath()

// def scriptData = new File(ScriptDir,"Script${curTime}.ps1")
// scriptData.deleteOnExit()
// scriptData.createNewFile()
// scriptData.write("")
// scriptData.append(scriptBody[0])
// scriptData.append(scriptBody[1])
// scriptData.append(scriptBody[2])
//def ScriptPath = scriptData.absolutePath
//ScriptPath = "$ScriptPath"
println scriptBody[0]
println scriptBody[1]
println scriptBody[2]