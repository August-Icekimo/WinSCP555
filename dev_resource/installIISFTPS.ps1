# 提供一個IIS制式安裝FTPS Server的SOP，最好是順便開好服務
# REF : https://4sysops.com/archives/install-and-configure-an-ftp-server-with-powershell/
# REF : https://github.com/ztrhgf/useful_powershell_functions/blob/master/FTP/install_FTP_server.ps1
# Install the Windows feature for FTP

if (! ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    throw "Run with administrator rights"
}


Set-Variable -Name "FTPSStatus" -Scope global -Description "Web-FTPS-Server install status" -PassThru -Value (Get-WindowsCapability -Online | Where-Object Name -like 'Web-FTP-Server*').State

Install-WindowsFeature Web-FTP-Server -IncludeAllSubFeature
Install-WindowsFeature Web-Server -IncludeAllSubFeature  IncludeManagementTools

# Import the module, this will map an Internet Information Services (IIS) drive (IIS:\)
Import-Module WebAdministration -ea Stop

$dnsName = Read-Host "Enter DNS name of this FTP server (e.g. ftp.contoso.com). It will be used for certificate creation."


Write-Host "確認防火牆規則 FTPS-Server-In-TCP 已經建立，如果沒有，就新增一個"

if (!(Get-NetFirewallRule -Name "FTPS-Server-In-TCP" -ErrorAction SilentlyContinue | Select-Object Name, Enabled)) 
{
    Write-Output "'FTPS-Server-In-TCP'防火牆規則建立中..."
    New-NetFirewallRule -Name 'FTPS-Server-In-TCP' -DisplayName 'FTPS Server (IIS)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 21
} 
else 
{
    Write-Output "'FTPS-Server-In-TCP'防火牆規則已經存在(已建立)。"
}

# Config the FTP site
try {
    $FTPSiteName = 'My FTPS Site 001'
    $FTPRootDir = 'D:\DAVAR\FTPsRoot'
    $FTPPort = 21
}
catch {
    <#Do this if a terminating exception happens#>
}
finally {
    # Create the FTP site
    New-WebFtpSite -Name $FTPSiteName -Port $FTPPort -PhysicalPath $FTPRootDir
}

# Create the ftps user/group
try {
    # Create the local Windows group
    $FTPUserGroupName = "FTPS Users"
    # $ADSI = [ADSI]"WinNT://$env:ComputerName"
    # $FTPUserGroup = $ADSI.Create("Group", "$FTPUserGroupName")
    # $FTPUserGroup.SetInfo()
    # $FTPUserGroup.Description = "Members of this group can connect through FTPS"
    # $FTPUserGroup.SetInfo()
    New-LocalGroup -Name $FTPUserGroupName -Description “Members of this group can connect throgh FTPS”
}
catch {
    <#Do this if a terminating exception happens#>
}

try {
    # Create an FTP user
    $FTPUserName = Read-Host -Prompt "輸入FTPS使用者名稱"
    $FTPPassword = Read-Host -Prompt "輸入FTPS使用者密碼" -AsSecureString 
    # $CreateUserFTPUser = $ADSI.Create("User", "$FTPUserName")
    # $CreateUserFTPUser.SetInfo()
    # $CreateUserFTPUser.SetPassword("$FTPPassword")
    # $CreateUserFTPUser.SetInfo()
    New-LocalUser -Name $FTPUserName -Password $FTPPassword -Description “User account to FTPS access” -PasswordNeverExpires -UserMayNotChangePassword
}
catch {
    <#Do this if a terminating exception happens#>
}

try {
    # Add an FTP user to the group FTP Users
    # $UserAccount = New-Object System.Security.Principal.NTAccount("$FTPUserName")
    # $SID = $UserAccount.Translate([System.Security.Principal.SecurityIdentifier])
    # $Group = [ADSI]"WinNT://$env:ComputerName/$FTPUserGroupName,Group"
    # $User = [ADSI]"WinNT://$SID"
    # $Group.Add($User.Path)
    Add-LocalGroupMember -Name $FTPUserGroupName -Member $FTPUserNam
}
catch {
    <#Do this if a terminating exception happens#>
}

# Enable basic authentication on the FTP site
try {
    $FTPSitePath = "IIS:\Sites\$FTPSiteName"
    $BasicAuth = 'ftpServer.security.authentication.basicAuthentication.enabled'
    Set-ItemProperty -Path $FTPSitePath -Name $BasicAuth -Value $True
    # Add an authorization read rule for FTP Users.
    $Param = @{
        Filter   = "/system.ftpServer/security/authorization"
        Value    = @{
            accessType  = "Allow"
            roles       = "$FTPUserGroupName"
            permissions = 1
        }
        PSPath   = 'IIS:\'
        Location = $FTPSiteName
    }
}
catch {
    <#Do this if a terminating exception happens#>
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

# verify this from the FTP root folder properties under the Security tab.

# Restart the FTP site for all changes to take effect
Restart-WebItem "IIS:\Sites\$FTPSiteName" -Verbose