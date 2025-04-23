# function Get-Example 
# {
#     <#
#     .SYNOPSIS
#         透過Powershell使用WinSCP.dll元件進行SFTP傳輸的通用腳本
#     .DESCRIPTION

# # 可以直接修改下列參數進行傳輸測試
#     .PARAMETER ${props['Username']}
#         $Username = "libsftphnms"
#     .PARAMETER ${props['Password']}
#         $Password = "1qaz@WSX"
#     .PARAMETER ${props['HostName']}
#         $HostName = "192.168.1.198"
#     .PARAMETER ${props['PortNumber']}
#         $PortNumber = "22"
#     .PARAMETER ${props['LDirectory']}
#         $LDirectory = "C:\varwrk\redpill-load\master\*"
#     .PARAMETER ${props['RDirectory']}
#         $RDirectory = "/"
#     .PARAMETER ${props['RemoveFiles']}
#         $RemoveFiles = "0"
#     .INPUTS
#         最終接入DA plugin時從$props[]接入各項參數，並透過環境變數DTC_ANA改變訊息
#     .OUTPUTS
#         透過環境變數DTC_ANA改變訊息
#     .EXAMPLE

#     .LINK

#     .NOTES


#     #>

# }

# Get a EnvironmentVariable DTC_ANA=PLEASE to enable debug mode.
if ($PSdebug -eq "PLEASE" ) 
{
    $PSdebug = $True
    Write-Host "--DTC Analyst is waiting."
}
else 
{
    $PSdebug = $False
}
#  自我測試時可開  $PSdebug = $True # Force to debug mode.
# 預設 WinSCP .NET 組件路徑
$DefaultWinSCPPath = "C:\Windows\System32\WinSCPnet.dll"
$WinSCPNetDllPath = $DefaultWinSCPPath

# 檢查環境變數 WINSCPDLLPATH 是否存在且有值
if ($env:WINSCPDLLPATH) {
    $WinSCPNetDllPath = $env:WINSCPDLLPATH
    if ($PSdebug) { Write-Host "DEBUG: Using WinSCP path from environment variable WINSCPDLLPATH: '$WinSCPNetDllPath'" }
} elseif ($PSdebug) { Write-Host "DEBUG: Environment variable WINSCPDLLPATH not set or empty. Using default WinSCP path: '$WinSCPNetDllPath'" }

$Global:succeedTransferdFiles = 1
$Global:LocalFilesCount = 0

# Test if WinSCP.dll is placed "C:\Windows\System32\WinSCPnet.dll"
try
{
    Add-Type -Path $WinSCPNetDllPath
    if ($PSdebug) 
    {
        Write-Host " 00 --WinSCP .NET Imported Successfully."
    }
}
catch
{
    if ($PSdebug)
    {
        Write-Host " 00 --WinSCP .NET Imported Faild from path: '$WinSCPNetDllPath'. Agent JRE Setting Might cause this fail, too."
    }
}

function GetFilesMD5
# Function to get sigle file MD5
{
 param ([Parameter(Mandatory=$True,Position=0)][string]$FilePath)

    $jsonf += "{`"File`":`"$FilePath`",`n`r"
	$FilePath = $FilePath.TrimStart('\\?\')
	$MD5s = Get-FileHash "$FilePath" -Algorithm MD5
	$MD5String = $MD5s.Hash
    $jsonf += " `"MD5`":`"$MD5String`"}"
    return $jsonf
}


function CountLocalFiles
# Fuction to count local files.
{
    Write-Host -NoNewline " 04 Total: "
    $Global:LocalFilesCount = (Get-ChildItem -Path "$LDirectory" -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer -eq $False } |  Measure-Object).Count
    Write-Host -NoNewline "$Global:LocalFilesCount files count ..."
    return $Global:LocalFilesCount
}

function FileTransferred
# Function to transfer and count how many files transferd.
{

    Param($e, $succeedTransferdFiles)

    if ($Null -eq $e.Error)
    # 正確傳輸就增加計數
    {
        # Write continious .. and numbers like couters do.
        Write-Host -NoNewline "$Global:succeedTransferdFiles.."
		$Global:succeedTransferdFiles++
    }
    else
    {
        Write-Host " 90 Transfer Failed !!!`t`r`n"
    }

    # Print out what may cause the transfer error .
    if (($Null -ne $e.Touch ) -and ($Null -ne $e.Touch.Error))
    # 無法變更傳輸後檔案時間
    {
        Write-Host -NoNewline "<Touch.Error "
    }

    if (($Null -ne $e.Chmod ) -and ($Null -ne $e.Chmod.Error))
    # 檔案權限不足
    {
        Write-Host  -NoNewline "<Chmod.Error"
    }
}


try
{
    # Initial transfer session parameters with session object
	$sessionOptions = New-Object WinSCP.SessionOptions
    $sessionOptions.Protocol = [WinSCP.Protocol]::Sftp
    $sessionOptions.UserName = "$Username"
    $sessionOptions.Password = "$Password"
    $sessionOptions.HostName = "$HostName"
    $sessionOptions.PortNumber = 22
# Reserve Future Version   $sessionOptions.SshHostKeyPolicy = "AcceptNew"
    $sessionOptions.GiveUpSecurityAndAcceptAnySshHostKey = "True"
    $session = New-Object WinSCP.Session

    # Open transfer session    
    try
    {
        $session.add_FileTransferred( { FileTransferred($_) } )
        Write-Host " 00 Session options loaded ..."
        if ($PSdebug)
        {
            Write-Host " ($sessionOptions ) "
        }
        try 
        {
            Write-Host " 01 Now opening Session to $HostName ..."
            $session.Open($sessionOptions)
        }
        catch 
        {
            # Print out the Error message.
            Write-Host ($session.Output | Out-String)
        }

        if ($session.Opened)
        {
            Write-Host " 02 Session Opened ..."
            CountLocalFiles
            Write-Host " 05 Uploading.."
        }
        $synchronizationResult = $session.PutFiles(  "\\?\$LDirectory", "$RDirectory", $RemoveFiles )
    }
    finally
    {
        $synchronizationResult.Check()
        $Global:succeedTransferdFiles--
        Write-Host " 06 Total: $Global:succeedTransferdFiles Files."
    }
    exit 0

}

catch [Exception]
# 抓到執行錯誤，exit 1
{

    Write-Host " 91 Exception caught. Something went wrong."
    $Global:succeedTransferdFiles--;
    $successfulUpload = $synchronizationResult.Transfers.Count - $synchronizationResult.Failures.Count

    if ($successfulUpload -ne $Global:succeedTransferdFiles)
    { 
        Write-Host " 92 File transfer stopped, and files count not match."
    }
    $session.Dispose() # Maybe session was started.
    $ErrMsg = $synchronizationResult.Failures
    Write-Host " 94 Detail Error Message : `r`n $ErrMsg"
    if ($PSdebug)
    {
        New-EventLog –LogName Application –Source “Deployment Automation”
        Write-EventLog –LogName Application –Source “Deployment Automation” –EntryType Error –EventID 2 –Message “\$ErrMsg"
        Remove-EventLog –Source “Deployment Automation"
    }
    exit 1
}

finally
# 傳輸後核對檢查
{

    if ($session.Opened -eq $True) 
    # 如果有成功打開連線，就計算檔案傳輸完成數
    {
        Write-Host  " 07 Successful uploaded: $Global:succeedTransferdFiles ... "
    }
    else
    # 如果Session連線失敗，計算檔案數也是0，就印出訊息
    {
        if ($Global:succeedTransferdFiles -eq 0)
        {
            
            Write-Host "Nothing is deployed. Maybe version files imported was empty. "
            Write-Host "$Global:succeedTransferdFiles of $Global:LocalFilesCount Count."
            if ($PSdebug)
            {
                Write-Host ($session.Output | Out-String)
            }
            # exit 1
        }
    }

    # Count how many files failures.
    $synchronizationResult.Transfers | Where-Object {$Null -eq $_.Error }|ForEach-Object {Write-Host $_.Destination -foregroundcolor Green}
    
	if ($synchronizationResult.Failures.Count -gt 0 )
    # 如果計算出傳輸失敗數
    {
        Write-Host " 07 Warning: Only $Global:succeedTransferdFiles of $Global:LocalFilesCount has been uploaded."
        if ($PSdebug)
        {
            #試圖找出斷頭的檔案，應該MD5會不一樣
            $Failures = $synchronizationResult.Transfers | Where-Object {$Null -ne $_.Error } | Select-Object -Property FileName
            if ($Null  -ne $Failures.FileName ) 
            {
                #先處理檔案路徑轉換
                $RemotePath = Split-Path $Failures.FileName
                Write-Host " Local MD5 Checksum in JSON format, may compare to server-side file."
                $RS = "$RDirectory"
                $RemotePath = $RemotePath.Replace("\\?\$RS", "")
                $RemotePath = "\" + "$RemotePath"
                #逐一比對檔案MD5 Checksum
                foreach ( $Sfilename in $Failures.FileName)
                {
                    GetFilesMD5($Sfilename)
                    if ($PSdebug)
                    {
                        $Sfilename = $Sfilename.Replace("\\?\", "")
                        $Sfilename = Get-ChildItem $Sfilename
                        $Suspected = $RemotePath + "/" + $Sfilename.Name
                        $Suspected = $Suspected.Replace( "\", "/" )
                        $Suspected = $Suspected.Replace( "//", "/" )
                        $x = $session.FileExists($Suspected)
                        #如果有嫌犯，試圖抓回來查看

                        if ( $x )
                        {
                            # Try to find out which file may broken.
                            $tempfilepath = "$RDirectory" + "\" + "CHECKSUMMD5TEMPFILE"
                            try
                            {
                                $Global:succeedTransferdFiles++
                                $session.GetFiles( $Suspected, $tempfilepath).Check()
                                $MD5 = Get-FileHash "$tempfilepath" -Algorithm MD5
                                $MD5String = $MD5.Hash
                                Write-Host "Remote File Found: $Suspected `r`n  MD5: $MD5String ."
                            }
                            finally
                            {
                                # clean up 
                                Remove-Item $tempfilepath
                            }
                        }
                    }
                }
            }
            else 
            {
                Write-Host "Unauthorized Remote file access may cause this. No checksum or file-compare need." 
            }
        }
    }
    $session.Dispose()
    Write-Host " 99 Transfer Session Closed."
}
