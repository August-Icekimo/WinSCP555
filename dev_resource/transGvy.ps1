#Requires -Version 5.1
<#
.SYNOPSIS
    Converts specific PowerShell script files (.ps1) into a format suitable
    for embedding within another system (like Groovy), saving the output
    with a .body extension. Mimics the functionality of the original transGvy.sh.

.DESCRIPTION
    This script processes four predefined PowerShell script files located in the
    same directory as this script:
    - Script_SFTP_TEST.ps1
    - Script_SFTP2_TEST.ps1
    - Script_FTPS_TEST.ps1
    - Script_FTPS2_TEST.ps1

    For each file, it performs the following transformations:
    1. Removes full-line and inline PowerShell comments (#).
    2. Removes empty lines.
    3. Escapes backslashes (\ -> \\).
    4. Escapes dollar signs ($ -> \$).
    5. Escapes backticks within double quotes ("`" -> "``").
    6. Replaces specific PowerShell variable placeholders (e.g., $Username)
       with a Groovy-like property syntax (e.g., ${props\[\[Username\]\]}).
    7. Converts double-bracket array syntax ([[...]]) to single-quoted syntax (['...']).
    8. Appends a literal '\r\n' (CRLF) sequence to the end of each processed line.
    9. Saves the resulting content to a corresponding .body file (e.g., Script_SFTP.body)
       in UTF8 encoding without a trailing newline.

.NOTES
    Author: Blackjack (AI Persona)
    Date:   2023-10-27
    Ensure the source .ps1 files exist in the same directory as this script.
    PowerShell 5.1 compatible.
#>

# --- Configuration ---
# Set to $true to enable verbose output during processing
$isDebug = $false

# Define the input files relative to the script's location
$ScriptBase = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
# Define the base names of the files to process
$FileBaseNames = @(
    "Script_SFTP_TEST.ps1",
    "Script_SFTP2_TEST.ps1",
    "Script_FTPS_TEST.ps1",
    "Script_FTPS2_TEST.ps1"
)
# Construct the full input file paths
$InputFiles = $FileBaseNames | ForEach-Object { Join-Path -Path $ScriptBase -ChildPath $_ }

# Define the variable replacements (PowerShell var name -> Target Groovy-like prop syntax)
# Using the literal target string format observed in the bash script
$VariableMap = @{
    'Username'    = '${props\[\[Username\]\]}'
    'Password'    = '${props\[\[Password\]\]}'
    'HostName'    = '${props\[\[HostName\]\]}'
    'PortNumber'  = '${props\[\[PortNumber\]\]}'
    'LDirectory'  = '${props\[\[LDirectory\]\]}'
    'RDirectory'  = '${props\[\[RDirectory\]\]}'
    'RemoveFiles' = '${props\[\[RemoveFiles\]\]}'
    'FtpSecure'   = '${props\[\[FtpSecure\]\]}' # Present in FTPS scripts
}

# Literal string to append representing CRLF
$crlfLiteral = '\r\n'

# --- Processing Logic ---

Write-Host "Starting PowerShell script conversion..."

foreach ($inputFile in $InputFiles) {
    $outputFile = $inputFile -replace '\.ps1$', '.body'
    Write-Host "Processing '$($inputFile)' -> '$($outputFile)'"

    # Check if input file exists
    if (-not (Test-Path -LiteralPath $inputFile)) {
        Write-Warning "Input file not found: '$inputFile'. Skipping."
        continue # Move to the next file
    }

    # Array to hold processed lines for the current file
    $processedLines = @()

    # Read input file line by line (UTF8 is a safe default for PS scripts)
    $lines = Get-Content -LiteralPath $inputFile -Encoding UTF8

    foreach ($line in $lines) {
        $currentLine = $line

        # 1. Remove comments
        # Remove full-line comments (potentially with leading whitespace)
        $currentLine = $currentLine -replace '^\s*#.*$'
        # Remove inline comments (making sure not to remove escaped # like `#')
        # This regex looks for a # not preceded by a backtick `
        $currentLine = $currentLine -replace '(?<!`)#.*$'

        # 2. Skip empty or whitespace-only lines resulting from comment removal
        if ($currentLine -notmatch '\S') {
            if ($isDebug) { Write-Host "  Skipping empty line: '$line'" }
            continue
        }

        # --- Apply transformations in the same order as sed ---
        # Note: PowerShell's -replace uses regex. Backslashes and dollar signs
        # have special meaning in BOTH the pattern and the replacement string
        # and often need escaping (e.g., using '\' or backticks depending on context).

        # 3. Escape backticks within double quotes: "`" -> "``"
        # Match literal " then ` then " -> Replace with literal " then `` then "
        $currentLine = $currentLine -replace '"`"', '"``"' # Simple literal replacement should work here

        # 4. Escape backslashes: \ -> \\
        # Match a single backslash -> Replace with two backslashes
        $currentLine = $currentLine -replace '\\', '\\'

        # 5. Escape dollar signs: $ -> \$
        # Match a literal dollar sign -> Replace with backslash and dollar sign
        $currentLine = $currentLine -replace '\$', '\$'

        # 6. Replace variable placeholders (e.g., \$Username -> ${props\[\[Username\]\]})
        foreach ($varName in $VariableMap.Keys) {
            # Pattern: Match literal \$ followed by the variable name. Escape $ in regex.
            # Use [regex]::Escape for robustness in case var names have special chars, though unlikely here.
            $regexPattern = '\$' + [regex]::Escape($varName)
            # Replacement: Use the literal string from the map. $ in replacement needs care,
            # but the target format already seems escaped for the final destination.
            $replacementValue = $VariableMap[$varName]
            $currentLine = $currentLine -replace $regexPattern, $replacementValue
        }

        # 7. Convert array syntax: [[ -> [' and ]] -> ']
        # Match literal [[ -> Replace with literal ['
        $currentLine = $currentLine -replace '\[\[', "\['"
        # Match literal ]] -> Replace with literal ']
        $currentLine = $currentLine -replace '\]\]', "'\]"

        # 8. Append literal '\r\n'
        $processedLines += $currentLine + $crlfLiteral

        if ($isDebug) { Write-Host "  Processed: $($currentLine + $crlfLiteral)" }
    }

    # Join all processed lines for this file into a single string
    # The literal '\r\n' is already appended to each line, so just join them.
    $finalContent = $processedLines -join ''

    # Write the final content to the output file
    # Use -Encoding UTF8 for consistency.
    # Use -NoNewline to prevent PowerShell adding an extra final newline.
    try {
        Set-Content -LiteralPath $outputFile -Value $finalContent -Encoding UTF8 -NoNewline -ErrorAction Stop
        Write-Host "  Successfully created '$outputFile'" -ForegroundColor Green
    }
    catch {
        Write-Error "Failed to write to '$outputFile'. Error: $($_.Exception.Message)"
    }
}

Write-Host "Conversion process finished."
