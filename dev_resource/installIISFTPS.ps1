# 提供一個IIS制式安裝FTPS Server的SOP，最好是順便開好服務，最好還會重開機喔
# REF : https://4sysops.com/archives/install-and-configure-an-ftp-server-with-powershell/
# REF : https://github.com/ztrhgf/useful_powershell_functions/blob/master/FTP/install_FTP_server.ps1
#
# Explicit 顯式模式（也稱為FTPES），FTPS客戶端先與伺服器建立明文連接，然後從控制通道明確請求伺服器端升級為加密連接（Cmd: AUTH TLS）。 
# 控制通道與資料通道預設埠與原始FTP一樣。控制通道始終加密，而資料通道是否加密則為可選項。 
# 同時若伺服器未限制明文連接，也可以使用未加密的原始FTP進行連接。
# 如果您啟用 FTPS 並將 FTP site指派給埠 990 以外的任何埠，就會使用顯式模式。
#
# 隱式模式 Implicit FTPS下不支援協商是否使用加密，所有的連接資料均為加密。客戶端必須先使用TLS Client Hello訊息向FTPS伺服器進行握手來建立加密連接。
# 如果FTPS伺服器未收到此類訊息，則伺服器應斷開連接。 
# 為了保持與現有的非FTPS感知客戶端的相容性，隱式FTPS預設在IANA規定的埠990/TCP上監聽FTPS控制通道，並在埠989/TCP上監聽FTPS資料通道。
# 使用 FTP 7 時，如果您啟用 FTPS 並將 FTP site指派給埠 990，則會使用隱式模式。
#
# 為了資安考量，故需強制使用990，使用Implicit FTPS強迫加密，但預設值WinSCP連接時不會連往CA驗證憑證，可簡單使用自簽憑證。


######################################
# Install the Windows feature for FTP

if (! ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    throw "+管理者權限 Run with administrator rights!"
}

# 先檢查目前安狀狀態，首次執行會要求重新開機
# 重新開機以後，直接再執行一次，就繼續設定

Set-Variable -Name "FTPSStatus" -Scope global -Description "Web-FTPS-Server install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'Web-FTP-Server').InstallState
if ( $FTPSStatus -ne "Installed" )
{
    $FTPSStatus = Install-WindowsFeature Web-FTP-Server -IncludeAllSubFeature
    # ExitCode      : SuccessRestartRequired / NoChangeNeeded
}

Set-Variable -Name "WebStatus" -Scope global -Description "Web-Server install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'Web-Server').InstallState
if ( $WebStatus -ne "Installed" )
{
    $WebStatus = Install-WindowsFeature Web-Server -IncludeAllSubFeature -IncludeManagementTools
    # ExitCode      : SuccessRestartRequired / NoChangeNeeded
}

if ( $FTPSStatus.ExitCode -eq "SuccessRestartRequired" -or $WebStatus.ExitCode -eq "SuccessRestartRequired" )
{
    Write-Host "準備重新開機"
    Restart-Computer -Confirm
}
else
{
    Write-Host "請手動檢查安裝結果"
}

# Import the module, this will map an Internet Information Services (IIS) drive (IIS:\)
Import-Module WebAdministration -ea Stop
Write-Host "change range of ports for passive FTP to 60000-60100"
Set-WebConfiguration "/system.ftpServer/firewallSupport" -PSPath "IIS:\" -Value @{lowDataChannelPort="60000";highDataChannelPort="60100";}
# cmd /c "$env:windir\System32\inetsrv\appcmd set config /section:system.ftpServer/firewallSupport /lowDataChannelPort:60000 /highDataChannelPort:65535"
Write-Host "請檢查防火牆規則與限縮動態埠範圍 Please check firewall setting."
Get-IISConfigSection -SectionPath "system.ftpServer/firewallSupport" 
Write-Host "先重啟服務 Restart Ftp Service" 
Restart-Service ftpsvc 

# 設定防火牆規則，關閉 20,21 ，修改為Implicit FTPS: 990,989,60K+100
# https://learn.microsoft.com/en-us/powershell/module/netsecurity/remove-netfirewallrule?view=windowsserver2022-ps
if ( ( Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-Passive-In-TCP" -ErrorAction SilentlyContinue | Select-Object Enabled ) -eq $True )
{
    Disable-NetFirewallRule -Name "IIS-WebServerRole-FTP-Passive-In-TCP"
    Write-Host "關閉預設防火牆規則: IIS-WebServerRole-FTP-Passive-In-TCP [TCP >1023], $? ."
}

if ( ( Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-In-TCP-21" -ErrorAction SilentlyContinue | Select-Object Enabled ) -eq $True )
{
    Disable-NetFirewallRule -Name "IIS-WebServerRole-FTP-In-TCP-21"
    Write-Host "關閉預設防火牆規則: IIS-WebServerRole-FTP-In-TCP-21, $? ."
}

if ( (Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-Out-TCP-20" -ErrorAction SilentlyContinue | Select-Object Enabled ) -eq $True )
{
    Disable-NetFirewallRule -Name "IIS-WebServerRole-FTP-Out-TCP-20"
    Write-Host "關閉預設防火牆規則: IIS-WebServerRole-FTP-Out-TCP-20, $? ."
}

if ( ( (Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-In-TCP-990" -ErrorAction SilentlyContinue | Select-Object Enabled)) -eq $False )
{
    Enable-NetFirewallRule -Name "IIS-WebServerRole-FTP-In-TCP-990"
    Write-Host "確保開啟預設防火牆規則: IIS-WebServerRole-FTP-In-TCP-990, $? ."
}

if ( ( (Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-Out-TCP-989" -ErrorAction SilentlyContinue | Select-Object Enabled)) -eq $False )
{
    Enable-NetFirewallRule -Name "IIS-WebServerRole-FTP-Out-TCP-989"
    Write-Host "確保開啟預設防火牆規則: IIS-WebServerRole-FTP-Out-TCP-989, $? ."
}

Write-Host "確認防火牆規則 FTPS Server Passive In被動輸入埠60K+100 已經建立，如果沒有，就新增一個"

if ((Get-NetFirewallRule -Name "IIS-WebServerRole-FTP-Passive-In-TCP6W" -ErrorAction SilentlyContinue | Select-Object Enabled) -eq $False ) 
{
    Write-Output "'FTPS-Server-In-TCP 60K+100'防火牆規則建立中..."
    New-NetFirewallRule -Name 'IIS-WebServerRole-FTP-Passive-In-TCP6W' -DisplayName 'FTP 伺服器被動 (FTP 被動輸入流量60K+100)' -Description "允許 Internet Information Services (IIS) 之被動 FTP 流量的輸入規則 [60K+100]" -Enabled True -Profile Any -Direction Inbound -Protocol TCP -Program Any -LocalAddress Any -Action Allow -LocalPort 60000-60100
    Write-Host "新建立防火牆規則 FTPS Server Passive In被動輸入埠60K+100"
} 
else 
{
    Write-Output "'FTPS-Server-In-TCP 60K+100'防火牆規則已經存在(已建立)。"
}
######################################
# 結束程式安裝


###################################### 
# 設定站台組態 Config the FTP site
try {
    $FTPSiteName = 'My FTPS Site 001'
    $prompt = Read-Host "Accept Site name: $FTPSiteName, or keyin a new one."
    $prompt = ($FTPSiteName,$prompt)[[bool]$prompt]
    $prompt = $null

    $FTPRootDir = 'D:\DAVAR\FTPsRoot'
    $prompt = Read-Host "Accept Site name: $FTPRootDir, or keyin a new one."
    $prompt = ($FTPRootDir,$prompt)[[bool]$prompt]
    $prompt = $null

    $FTPPort = 990
}
catch 
{
    Write-Error "Someone stop configing the FTP site."
}
finally 
{
    if (Test-Path -Path $FTPRootDir) 
    {
        mkdir -p $FTPRootDir
    }
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
    Add-LocalGroupMember -Name $FTPUserGroupName -Member $FTPUserName
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

######################################
# 強制使用force FTPS

$dnsName = "poc.icekimo.idv.tw"
$prompt = "Accept DNS name of this FTP server $dnsName. It will be used for certificate creation."
$prompt = ($dnsName,$prompt)[[bool]$prompt]
$prompt = $null

# REF: https://learn.microsoft.com/zh-tw/iis/configuration/system.applicationhost/sites/sitedefaults/ftpserver/security/ssl
Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.controlChannelPolicy -Value 1 
# SslRequire = 1, SslRequireCredentialsOnly = 2
Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.dataChannelPolicy -Value 1 
# SslRequire = 1, SslRequireCredentialsOnly = 2

$newCert = New-SelfSignedCertificate -FriendlyName "FTPS Server" -CertStoreLocation "Cert:\LocalMachine\MY" -DnsName $dnsName -NotAfter (Get-Date).AddMonths(60)
# serverCertStoreName 指定伺服器 SSL 憑證的憑證存放區。 預設值是 MY

# bind certificate to FTP site
# 指定要用於 SSL 連線之伺服器端憑證的指紋雜湊。
Set-ItemProperty -Path $FTPSitePath -Name ftpServer.security.ssl.serverCertHash -Value $newCert.GetCertHashString()

# Restart the FTP site for all changes to take effect
Restart-WebItem "IIS:\Sites\$FTPSiteName" -Verbose

######################################
# 經測試此時已經可以用WinSCP /FTP /implicit成功登入