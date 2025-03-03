#!/bin/bash
# Convert the powershell script to groovy style script that can be accepted


# Remove all comments (both full-line and inline comments) and empty lines
sed -e 's/\ *.#.*//g' -e 's/#.*$//g' -e '/^$/d' \
# Escape special characters: backticks, backslashes, and dollar signs
-e 's/"`"/"`"/g' -e 's/\\/\\\\/g' -e 's/\$/\\$/g' \
# Add Windows-style line endings (CRLF) to each line
-e 's/$/\\r\\n/g' \
# Replace variable placeholders with PowerShell property syntax, which fit plugin's requirements
-e 's/\\$Username/${props[[Username]]}/g' \
-e 's/\\$Password/${props[[Password]]}/g' \
-e 's/\\$HostName/${props[[HostName]]}/g' \
-e 's/\\$PortNumber/${props[[PortNumber]]}/g' \
-e 's/\\$LDirectory/${props[[LDirectory]]}/g' \
-e 's/\\$RDirectory/${props[[RDirectory]]}/g' \
-e 's/\\$RemoveFiles/${props[[RemoveFiles]]}/g' \
# Convert array syntax from double brackets to single quotes
-e "s/\[\[/\['/g" \
-e "s/\]\]/'\]/g" \
# Process input file and write to output file
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