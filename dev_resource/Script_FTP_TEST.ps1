    $PSdebug = [System.Environment]::GetEnvironmentVariable("DTC_ANA");

    if ($PSdebug -like "PLEASE" ) {$PSdebug = $True;Write-Host "--DTC Analyst is waiting.";}else {$PSdebug = $False};

    $PSdebug = $True

    $Global:succeedTransferdFiles = 1;

    $Global:LocalFilesCount = 0;

try

{

    Add-Type -Path "C:\Windows\System32\WinSCPnet.dll";

    if ($PSdebug) {Write-Host "--WinSCP .NET Imported Successfully."};

}


catch

{

    if ($PSdebug) {Write-Host "--WinSCP .NET Imported Faild. Agent JRE Setting Might cause this fail, too."};

}


function GetFilesMD5

{

 param ([Parameter(Mandatory=$True,Position=0)][string]$FilePath)

    $jsonf += "{`"File`":`"$FilePath`",`n`r";

	$FilePath = $FilePath.TrimStart('\\?\');

	$MD5s = Get-FileHash "$FilePath" -Algorithm MD5;

	$MD5String = $MD5s.Hash;

    $jsonf += " `"MD5`":`"$MD5String`"}"

    return $jsonf;

}


function CountLocalFiles

{

Write-Host -NoNewline "Total: ";

    $Global:LocalFilesCount = (Get-ChildItem -Path "D:\var\wrk\BHCF-10.10.133.1~2" -Force -Recurse -ErrorAction SilentlyContinue | Where-Object { $_.PSIsContainer -eq $false } |  Measure-Object).Count;

    return $Global:LocalFilesCount;

}

function FileTransferred

{

    Param($e, $succeedTransferdFiles)

    if ($Null -eq $e.Error)

    {

        Write-Host -NoNewline $Global:succeedTransferdFiles".. ";

		$Global:succeedTransferdFiles++;

    }

    else

    {

        Write-Host "Transfer Failed!!!`t`r`n";

    }

    if (( $Null -ne $e.Touch ) -and ($Null -ne $e.Touch.Error))

    {

            Write-Host -NoNewline "<Touch.Error ";

    }

    if (($Null -ne $e.Chmod ) -and ($Null -ne $e.Chmod.Error))

    {

            Write-Host  -NoNewline "<Chmod.Error";

    }

}


try

{

	$sessionOptions = New-Object WinSCP.SessionOptions;

    $sessionOptions.Protocol = [WinSCP.Protocol]::Ftp;

    $sessionOptions.FtpSecure = "ExplicitTls";

    $sessionOptions.UserName = "";

    $sessionOptions.Password = "+";

    $sessionOptions.HostName = "10.10.1.1";

    $sessionOptions.PortNumber = "991";

    $sessionOptions.GiveUpSecurityAndAcceptAnyTlsHostCertificate = "$True";

    $session = New-Object WinSCP.Session;

    try

    {

        $session.add_FileTransferred( { FileTransferred($_) } );

        Write-Host  -NoNewline "Try to open Seesion";

		try { $session.Open($sessionOptions);}

        catch { $session.Output | Select-object -skip 8; }

        if ($session.Opened) {Write-Host -NoNewLine "Session Opened." ; CountLocalFiles;};

        $synchronizationResult = $session.PutFiles(  "\\?\D:\var\wrk\BHCF-10.10.133.1~2", "\", 0 );

     }

     finally

     {

        $synchronizationResult.Check();

        $Global:succeedTransferdFiles--;

		Write-Host -NoNewLine "Total: $Global:succeedTransferdFiles Files ";

     }

     exit 0;

}

catch [Exception]

{
    { Write-Host"Exception caught. Something went wrong."; };

    $Global:succeedTransferdFiles--;

    $successfulUpload = $synchronizationResult.Transfers.Count - $synchronizationResult.Failures.Count;

    if ($successfulUpload -ne $Global:succeedTransferdFiles)

    { Write-Host"File Transfer is stopped."; };

    exit 1;

}

finally

{

    if ($Global:succeedTransferdFiles -eq 0)
 
	{

		Write-Host "Version Files which imported was empty. Nothing is deployed."

		exit 1;

	}

	if ($synchronizationResult.Failures.Count -gt 0 ){Write-Host "Warning: Only $Global:succeedTransferdFiles of $Global:LocalFilesCount has been Uploaded."};

    if ($session.Opened -eq $true) {Write-Host  "Successful uploaded:"}

    $synchronizationResult.Transfers | Where-Object {$Null -eq $_.Error }|ForEach-Object {Write-Host $_.Destination -foregroundcolor Green}

    if ($synchronizationResult.Failures.Count -gt 0 )

    {

        $Failures = $synchronizationResult.Transfers | Where-Object {$Null -ne $_.Error } | Select-Object -Property FileName;

        if ($Null  -ne $Failures.FileName ) 

		{

			$RemotePath = Split-Path $Failures.FileName ;

        Write-Host " Local MD5 Checksum in JSON format, may compare to server-side file.";

		$RS = "D:\var\wrk\BHCF-10.10.133.1~2";

		$RemotePath = $RemotePath.Replace("\\?\$RS", "");

        $RemotePath = "\" + "$RemotePath";

    foreach ( $Sfilename in $Failures.FileName)

    {

		GetFilesMD5($Sfilename);

        if ($PSdebug)

        {

            $Sfilename = $Sfilename.Replace("\\?\", "");

			$Sfilename = Get-ChildItem $Sfilename;

            $Suspected = $RemotePath + "/" + $Sfilename.Name;

            $Suspected = $Suspected.Replace( "\", "/" );

			$Suspected = $Suspected.Replace( "//", "/" );

                $x = $session.FileExists($Suspected);

                if ( $x )

                {

                    $tempfilepath = "D:\var\wrk\BHCF-10.10.133.1~2" + "\" + "CHECKSUMMD5TEMPFILE";

                    try

                    {

						$Global:succeedTransferdFiles++;

						$session.GetFiles( $Suspected, $tempfilepath).Check();

						$MD5 = Get-FileHash "$tempfilepath" -Algorithm MD5;

                        $MD5String = $MD5.Hash;

                        Write-Host "Remote File Found: $Suspected `r`n  MD5: "$MD5String .

                    }

                    finally

                    {

                        Remove-Item $tempfilepath;

						$session.Dispose();

                    }

                }

        }

    }

		}

		else 

		{

			Write-Host "Unauthorized Remote files may cause this. No checksum or file-compare need." 
		}

    $session.Dispose();

    $ErrMsg = $synchronizationResult.Failures;

    Write-Host "Detail Error Message : `r`n $ErrMsg";

    }

}

