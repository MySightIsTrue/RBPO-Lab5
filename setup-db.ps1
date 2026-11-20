param(
    [ValidatePattern('^translations_lab5(?:_test)?$')]
    [string]$Database = 'translations_lab5',
    [string]$PgHost = 'localhost',
    [int]$Port = 5432,
    [string]$AdminUser = 'postgres',
    [string]$Psql
)
$ErrorActionPreference = 'Stop'
function Find-Psql {
    $command = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $directories = @()
    if ($env:PGBIN) { $directories += $env:PGBIN }
    if ($env:PGHOME) { $directories += Join-Path $env:PGHOME 'bin' }
    # The installer records its actual directory, including installations on D:.
    foreach ($key in @('HKLM:\SOFTWARE\PostgreSQL\Installations\*', 'HKLM:\SOFTWARE\WOW6432Node\PostgreSQL\Installations\*')) {
        foreach ($installation in @(Get-ItemProperty -Path $key -ErrorAction SilentlyContinue)) {
            if ($installation.'Base Directory') {
                $directories += Join-Path $installation.'Base Directory' 'bin'
            }
        }
    }
    foreach ($root in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if ($root) {
            $directories += Get-ChildItem -Path (Join-Path $root 'PostgreSQL\*') -Directory -ErrorAction SilentlyContinue |
                Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin' }
        }
    }
    foreach ($directory in $directories) {
        $candidate = Join-Path $directory 'psql.exe'
        if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
    }
}
if (-not $Psql) { $Psql = Find-Psql }
if (-not $Psql) {
    Write-Host 'PostgreSQL client psql.exe was not found in PATH or installed PostgreSQL directories.'
    Write-Host 'If PostgreSQL is installed elsewhere, enter its full path, for example D:\PostgreSQL\18\bin\psql.exe.'
    Write-Host 'If it is not installed, install PostgreSQL with Command Line Tools, then run this script again.'
    $Psql = Read-Host 'Full path to psql.exe (Enter to exit)'
}
if ($Psql) { $Psql = $Psql.Trim().Trim('"') }
if (-not $Psql -or -not (Test-Path -LiteralPath $Psql -PathType Leaf)) {
    throw 'psql.exe not found. Install PostgreSQL Command Line Tools or run .\setup-db.ps1 -Psql "D:\PostgreSQL\18\bin\psql.exe" with your actual path.'
}
$previousPassword = $env:PGPASSWORD
$previousEncoding = $env:PGCLIENTENCODING
try {
    if ($env:DB_ADMIN_PASSWORD) { $env:PGPASSWORD = $env:DB_ADMIN_PASSWORD }
    elseif (-not $env:PGPASSWORD) {
        $password = Read-Host 'PostgreSQL administrator password' -AsSecureString
        $env:PGPASSWORD = [System.Net.NetworkCredential]::new('', $password).Password
    }
    $env:PGCLIENTENCODING = 'UTF8'
    $envFile = Join-Path $PSScriptRoot '.env'
    $appPassword = $env:DB_PASSWORD
    if (-not $appPassword -and (Test-Path -LiteralPath $envFile)) {
        $line = Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^DB_PASSWORD=' } | Select-Object -First 1
        if ($line) { $appPassword = $line.Substring('DB_PASSWORD='.Length) }
    }
    if (-not $appPassword) { $appPassword = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N') }
    $connection = @('-X', '-w', '-h', $PgHost, '-p', "$Port", '-U', $AdminUser, '-v', 'ON_ERROR_STOP=1')
    & $Psql @connection -d postgres -v "database_name=$Database" -v 'app_user=translation_lab5_app' -v "app_password=$appPassword" -f (Join-Path $PSScriptRoot 'database/setup.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Database / role preparation failed.' }
    $initialized = & $Psql @connection -d $Database -Atc "SELECT to_regclass('public.app_users') IS NOT NULL"
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect the database.' }
    if ($initialized -eq 'f') {
        & $Psql @connection -d $Database --single-transaction -f (Join-Path $PSScriptRoot 'db/01-schema.sql') -f (Join-Path $PSScriptRoot 'db/02-seed.sql') -f (Join-Path $PSScriptRoot 'db/04-approved-index.sql')
        if ($LASTEXITCODE -ne 0) { throw 'Schema / seed preparation failed.' }
    }
    & $Psql @connection -d $Database --single-transaction -f (Join-Path $PSScriptRoot 'db/05-security.sql') -f (Join-Path $PSScriptRoot 'db/06-sessions.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Security schema preparation failed.' }
    & $Psql @connection -d $Database -v "database_name=$Database" -v 'app_user=translation_lab5_app' -f (Join-Path $PSScriptRoot 'database/grants.sql')
    if ($LASTEXITCODE -ne 0) { throw 'Granting application permissions failed.' }
    # A test database must not replace the application's saved connection.
    if ($Database -eq 'translations_lab5') {
        $jwtSecret = $env:JWT_SECRET
        if (-not $jwtSecret -and (Test-Path -LiteralPath $envFile)) {
            $line = Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^JWT_SECRET=' } | Select-Object -First 1
            if ($line) { $jwtSecret = $line.Substring('JWT_SECRET='.Length) }
        }
        if (-not $jwtSecret) {
            $bytes = New-Object byte[] 32
            $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
            try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
            $jwtSecret = [Convert]::ToBase64String($bytes)
        }
        $editorSetting = ''
        if (Test-Path -LiteralPath $envFile) {
            $editorSetting = Get-Content -LiteralPath $envFile | Where-Object { $_ -match '^EDITOR_PASSWORD=' } | Select-Object -First 1
        }
        $settings = "DB_URL=jdbc:postgresql://${PgHost}:${Port}/$Database`nDB_USER=translation_lab5_app`nDB_PASSWORD=$appPassword`n"
        $settings += "JWT_SECRET=$jwtSecret`n$editorSetting`n"
        [System.IO.File]::WriteAllText($envFile, $settings, [System.Text.UTF8Encoding]::new($false))
    }
    Write-Host "Ready: $Database (translation_lab5_app)."
} finally {
    $env:PGPASSWORD = $previousPassword
    $env:PGCLIENTENCODING = $previousEncoding
}



