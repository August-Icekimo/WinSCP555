# function Get-Example 
# {
#     <#
#     .SYNOPSIS
#         透過Powershell使用WinSCP.dll元件進行FTPS/SFTP傳輸的通用腳本
#     .DESCRIPTION

# # 可以直接修改下列參數進行傳輸測試
#     .PARAMETER ${props['FtpSecure']}
#         $FtpSecure = "ExplicitTls"
#     .PARAMETER ${props['Username']}
#         $Username = ""
#     .PARAMETER ${props['Password']}
#         $Password = ""
#     .PARAMETER ${props['HostName']}
#         $HostName = "10.10.133.1"
#     .PARAMETER ${props['PortNumber']}
#         $PortNumber = "991"
#     .PARAMETER ${props['LDirectory']}
#         $LDirectory = "D:\var\wrk\"
#     .PARAMETER ${props['RDirectory']}
#         $RDirectory = "\"
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
$PSdebug = [System.Environment]::GetEnvironmentVariable("DTC_ANA")
if ($PSdebug -like "PLEASE" ) 
{
    $PSdebug = $True;Write-Host "--DTC Analyst is waiting."
}
else 
{
    $PSdebug = $False
}
#    $PSdebug = $True # Force to debug mode.

$Global:succeedTransferdFiles = 1
$Global:LocalFilesCount = 0

# Test if WinSCP.dll is placed "C:\Windows\System32\WinSCPnet.dll"
try
{
    Add-Type -Path "C:\Windows\System32\WinSCPnet.dll"
    if ($PSdebug) 
    {
        Write-Host " 00 --WinSCP .NET Imported Successfully."
    }
}
catch
{
    if ($PSdebug)
    {
        Write-Host " 00 --WinSCP .NET Imported Faild. Agent JRE Setting Might cause this fail, too."
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
    $sessionOptions.Protocol = [WinSCP.Protocol]::Ftp
    $sessionOptions.FtpSecure = "$FtpSecure"
    $sessionOptions.UserName = "$Username"
    $sessionOptions.Password = "$Password"
    $sessionOptions.HostName = "$HostName"
    # 新增加可以自由調整port number
    $sessionOptions.PortNumber = "$PortNumber"
    $sessionOptions.GiveUpSecurityAndAcceptAnyTlsHostCertificate = "$True"
    # 在此直接插入Raw Setting, 輸入方式：
    # https://winscp.net/eng/docs/rawsettings
    # 
    # $sessionOptions.AddRawSettings("ProxyMethod", "3")
    # $sessionOptions.AddRawSettings("ProxyHost", "proxy")
    ${props['SessionOptionsAddRawSettings']}
    $session = New-Object WinSCP.Session

    # Open transfer session    
    try
    {
        $session.add_FileTransferred( { FileTransferred($_) } )
        Write-Host " 00 Session options loaded ..."
        try 
        {
            Write-Host " 01 Now opening Session to $HostName ..."
            $session.Open($sessionOptions)
        }
        catch 
        {
            # Pront out the Error message.
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
        Write-Host " 03 Total: $Global:succeedTransferdFiles Files."
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

    if ($session.Opened -eq $true) 
    # 如果有成功連線，就計算檔案傳輸完成數
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
            $Failures = $synchronizationResult.Transfers | Where-Object {$Null -ne $_.Error } | Select-Object -Property FileName
            if ($Null  -ne $Failures.FileName ) 
            {
                $RemotePath = Split-Path $Failures.FileName
                Write-Host " Local MD5 Checksum in JSON format, may compare to server-side file."
                $RS = "$RDirectory"
                $RemotePath = $RemotePath.Replace("\\?\$RS", "")
                $RemotePath = "\" + "$RemotePath"
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
    Write-Host " 99 Transfer Session CLosed."
}
