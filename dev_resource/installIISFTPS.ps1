# 提供一個IIS制式安裝FTPS Server的SOP，最好是順便開好服務
# REF : https://4sysops.com/archives/install-and-configure-an-ftp-server-with-powershell/
# REF : https://github.com/ztrhgf/useful_powershell_functions/blob/master/FTP/install_FTP_server.ps1
# Install the Windows feature for FTP

if (! ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    throw "+管理者權限 Run with administrator rights!"
}


Set-Variable -Name "FTPSStatus" -Scope global -Description "Web-FTPS-Server install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'Web-FTP-Server*').State

Install-WindowsFeature Web-FTP-Server -IncludeAllSubFeature
Install-WindowsFeature Web-Server -IncludeAllSubFeature  IncludeManagementTools

# Import the module, this will map an Internet Information Services (IIS) drive (IIS:\)
Import-Module WebAdministration -ea Stop
Write-Host "change range of ports for passive FTP to 60000-60100"
Set-WebConfiguration "/system.ftpServer/firewallSupport" -PSPath "IIS:\" -Value @{lowDataChannelPort="60000";highDataChannelPort="60100";}
# cmd /c "$env:windir\System32\inetsrv\appcmd set config /section:system.ftpServer/firewallSupport /lowDataChannelPort:60000 /highDataChannelPort:65535"
Write-Host"請檢查防火牆規則與限縮動態埠範圍 Please check firewall setting."
Get-IISConfigSection -SectionPath "system.ftpServer/firewallSupport" 
Write-Host "先重啟服務 Restart Ftp Service" 
Restart-Service ftpsvc 


Write-Host "確認防火牆規則 FTPS-Server-In-TCP 已經建立，如果沒有，就新增一個"

if (!(Get-NetFirewallRule -Name "FTPS-Server-In-TCP" -ErrorAction SilentlyContinue | Select-Object Name, Enabled)) 
{
    Write-Output "'FTPS-Server-In-TCP'防火牆規則建立中..."
    New-NetFirewallRule -Name 'FTPS-Server-In-TCP' -DisplayName 'FTPS Server (IIS)' -Enabled True -Profile Any -Direction Inbound -Protocol TCP -Program Any -LocalAddress Any -Action Allow -LocalPort 21,60000-60100
} 
else 
{
    Write-Output "'FTPS-Server-In-TCP'防火牆規則已經存在(已建立)。"
}

# Config the FTP site
try {
    $FTPSiteName = 'My FTPS Site 001'
    $prompt = Read-Host "Accept Site name: $FTPSiteName, or keyin a new one."
    $prompt = ($FTPSiteName,$prompt)[[bool]$prompt]
    $prompt = $null

    $FTPRootDir = 'D:\DAVAR\FTPsRoot'
    $prompt = Read-Host "Accept Site name: $FTPRootDir, or keyin a new one."
    $prompt = ($FTPRootDir,$prompt)[[bool]$prompt]
    $prompt = $null

    $FTPPort = 21
}
catch {
    Write-Error "Someone stop configing the FTP site."
}
finally {
    # Create the FTP site
    New-WebFtpSite -Name $FTPSiteName -Port $FTPPort -PhysicalPath $FTPRootDir
}

# Create the ftps user/group
try {
    Write-Host "Create the local Windows group: FTPS Users"
    $FTPUserGroupName = "FTPS_Users"
    $prompt = Read-Host "Accept FTPUserGroupName: $FTPUserGroupName, or keyin a new one."
    $prompt = ($FTPUserGroupName,$prompt)[[bool]$prompt]
    $prompt = $null

    New-LocalGroup -Name $FTPUserGroupName -Description “Members of this group can connect throgh FTPS”
}
catch {
    Write-Error "初四了阿北 Something wrong while Create group: $FTPUserGroupName"
}

try {
    Write-Host "Create an FTP user"
    $FTPUserName = "libftps001"
    $prompt = "Accept FTP User Name: $FTPUserName, or keyin a new one."
    $prompt = ($FTPUserName,$prompt)[[bool]$prompt]
    $prompt = $null

    $FTPPassword = Read-Host -Prompt "輸入FTPS使用者密碼 一次機會 One chance password keyin" -AsSecureString 

    New-LocalUser -Name $FTPUserName -Password $FTPPassword -Description “User account to FTPS access” -PasswordNeverExpires -UserMayNotChangePassword
}
catch {
    Write-Error "初四了阿北 Something wrong while Create User: $FTPUserName"
}

try {
    Write-Host "Add FTP user $FTPUserName to group $FTPUserGroupName"
    Add-LocalGroupMember -Name $FTPUserGroupName -Member $FTPUserNam
}
catch {
    Write-Error "Something wrong while adding FTP user $FTPUserName to group $FTPUserGroupName"
}

# Enable basic authentication on the FTP site
try {
    $FTPSitePath = "IIS:\Sites\$FTPSiteName"
    $BasicAuth = 'ftpServer.security.authentication.basicAuthentication.enabled'
    Set-ItemProperty -Path $FTPSitePath -Name $BasicAuth -Value $True
    Write-Host "Add an authorization write rule for Group $FTPUserGroupName."
    $Param = @{
        Filter   = "/system.ftpServer/security/authorization"
        Value    = @{
            accessType  = "Allow"
            roles       = "$FTPUserGroupName"
            permissions = 3
        }
        PSPath   = 'IIS:\'
        Location = $FTPSiteName
    }
}
catch {
    Write-Host "初四了阿北 Fail to authorization group $FTPUserGroupName read right on $FTPSitePath."
}
finally {
    Add-WebConfiguration @param

    # check these settings under IIS Manager > FTP Site > FTP Authorization Rules.
    # Change the SSL policy from Require SSL to Allow SSL connections.
    $UserAccount = New-Object System.Security.Principal.NTAccount("$FTPUserGroupName")
    $AccessRule = [System.Security.AccessControl.FileSystemAccessRule]::new($UserAccount,
        'ReadAndExecute',
        'ContainerInherit,ObjectInherit',
        'None',
        'Allow'
    )
    $ACL = Get-Acl -Path $FTPRootDir
    $ACL.SetAccessRule($AccessRule)
    $ACL | Set-Acl -Path $FTPRootDir
}

Write-Host "verify this from the FTP root folder properties under the Security tab."

# force FTPS

$dnsName = "poc.icekimo.idv.tw"
$prompt = "Accept DNS name of this FTP server $dnsName. It will be used for certificate creation."
$prompt = ($dnsName,$prompt)[[bool]$prompt]
$prompt = $null

Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.controlChannelPolicy -Value 1
Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.dataChannelPolicy -Value 1
$newCert = New-SelfSignedCertificate -FriendlyName "FTPS Server" -CertStoreLocation "Cert:\LocalMachine\$FTPSiteName" -DnsName $dnsName -NotAfter (Get-Date).AddMonths(60)
# bind certificate to FTP site
Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.serverCertHash -Value $newCert.GetCertHashString()

# Restart the FTP site for all changes to take effect
Restart-WebItem "IIS:\Sites\$FTPSiteName" -Verbose