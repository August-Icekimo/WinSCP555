sed -e 's/\ *.#.*//g' -e 's/#.*$//' -e '/^$/d' \
-e 's/"`"/"``"/g' -e 's/\\/\\\\/g' -e 's/\$/\\$/g' \
-e 's/$/\\r\\n/g' \
-e 's/\\$Username/\${props\[\[Username\]\]}/g' \
-e 's/\\$Password/\${props\[\[Password\]\]}/g' \
-e 's/\\$HostName/\${props\[\[HostName\]\]}/g' \
-e 's/\\$PortNumber/\${props\[\[PortNumber\]\]}/g' \
-e 's/\\$LDirectory/\${props\[\[LDirectory\]\]}/g' \
-e 's/\\$RDirectory/\${props\[\[RDirectory\]\]}/g' \
-e 's/\\$RemoveFiles/\${props\[\[RemoveFiles\]\]}/g' \
-e "s/\[\[/\['/g" \
-e "s/\]\]/'\]/g" \
./Script_SFTP_TEST.ps1 > Script_SFTP.body

sed -e 's/\ *.#.*//g' -e 's/#.*$//' -e '/^$/d' \
-e 's/"`"/"``"/g' -e 's/\\/\\\\/g' -e 's/\$/\\$/g' \
-e 's/$/\\r\\n/g' \
-e 's/\\$Username/\${props\[\[Username\]\]}/g' \
-e 's/\\$Password/\${props\[\[Password\]\]}/g' \
-e 's/\\$HostName/\${props\[\[HostName\]\]}/g' \
-e 's/\\$PortNumber/\${props\[\[PortNumber\]\]}/g' \
-e 's/\\$LDirectory/\${props\[\[LDirectory\]\]}/g' \
-e 's/\\$RDirectory/\${props\[\[RDirectory\]\]}/g' \
-e 's/\\$RemoveFiles/\${props\[\[RemoveFiles\]\]}/g' \
-e "s/\[\[/\['/g" \
-e "s/\]\]/'\]/g" \
./Script_SFTP2_TEST.ps1 > Script_SFTP2.body

sed -e 's/\ *.#.*//g' -e 's/#.*$//' -e '/^$/d' \
-e 's/"`"/"``"/g' -e 's/\\/\\\\/g' -e 's/\$/\\$/g' \
-e 's/$/\\r\\n/g' \
-e 's/\\$FtpSecure/\${props\[\[FtpSecure\]\]}/g' \
-e 's/\\$Username/\${props\[\[Username\]\]}/g' \
-e 's/\\$Password/\${props\[\[Password\]\]}/g' \
-e 's/\\$HostName/\${props\[\[HostName\]\]}/g' \
-e 's/\\$PortNumber/\${props\[\[PortNumber\]\]}/g' \
-e 's/\\$LDirectory/\${props\[\[LDirectory\]\]}/g' \
-e 's/\\$RDirectory/\${props\[\[RDirectory\]\]}/g' \
-e 's/\\$RemoveFiles/\${props\[\[RemoveFiles\]\]}/g' \
-e "s/\[\[/\['/g" \
-e "s/\]\]/'\]/g" \
./Script_FTPS_TEST.ps1 > Script_FTPS.body

sed -e 's/\ *.#.*//g' -e 's/#.*$//' -e '/^$/d' \
-e 's/"`"/"``"/g' -e 's/\\/\\\\/g' -e 's/\$/\\$/g' \
-e 's/$/\\r\\n/g' \
-e 's/\\$Username/\${props\[\[Username\]\]}/g' \
-e 's/\\$FtpSecure/\${props\[\[FtpSecure\]\]}/g' \
-e 's/\\$Password/\${props\[\[Password\]\]}/g' \
-e 's/\\$HostName/\${props\[\[HostName\]\]}/g' \
-e 's/\\$PortNumber/\${props\[\[PortNumber\]\]}/g' \
-e 's/\\$LDirectory/\${props\[\[LDirectory\]\]}/g' \
-e 's/\\$RDirectory/\${props\[\[RDirectory\]\]}/g' \
-e 's/\\$RemoveFiles/\${props\[\[RemoveFiles\]\]}/g' \
-e "s/\[\[/\['/g" \
-e "s/\]\]/'\]/g" \
./Script_FTPS2_TEST.ps1 > Script_FTPS2.body