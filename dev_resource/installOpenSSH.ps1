
Set-Variable -Name "clientStatus" -Scope global -Description "OpenSSH Client install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Client*').State
Set-Variable -Name "serverStatus" -Scope global -Description "OpenSSH Server install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Server*').State

function checkOpenSSHStatus {
$clientStatus = (Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Client*').State
$serverStatus = (Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Server*').State
Write-Host "OpenSSH安裝狀態 Client: $clientStatus , Server: $serverStatus"
}
Write-Host "確認目前OpenSSH安裝狀態"
checkOpenSSHStatus

try 
{
    if ( $clientStatus -eq "NotPresent" )
    {
        # Install the OpenSSH Client
        try {
            Add-WindowsCapability -Online -Name "OpenSSH.Client~~~~0.0.1.0"
        }
        catch {
            Write-Host "$Error[0]"
        }
        finally {
            Write-Host "OpenSSH Client installed."
        }
    }
    if ( $serverStatus -eq "NotPresent" )
    {
        # Install the OpenSSH Client
        try {
            Add-WindowsCapability -Online -Name "OpenSSH.Server~~~~0.0.1.0"
        }
        catch {
            Write-Host "$Error[0]"
        }
        finally {
            Write-Host "OpenSSH Server installed."
        }
    }
}
catch 
{
    Write-Host "Ooops, something go wrong."
    Write-Host "$Error[0]"
    checkOpenSSHStatus
}
finally 
{
    checkOpenSSHStatus
    Write-Host "Start the sshd service"
    Start-Service sshd

    # OPTIONAL but recommended:
    Set-Service -Name sshd -StartupType 'Automatic'
}

Write-Host "確認防火牆規則已經建立，如果沒有，就新增一個"

if (!(Get-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -ErrorAction SilentlyContinue | Select-Object Name, Enabled)) 
{
    Write-Output "Firewall Rule 'OpenSSH-Server-In-TCP' does not exist, creating it..."
    New-NetFirewallRule -Name 'OpenSSH-Server-In-TCP' -DisplayName 'OpenSSH Server (sshd)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22
} 
else 
{
    Write-Output "Firewall rule 'OpenSSH-Server-In-TCP' has been created and exists."
}

