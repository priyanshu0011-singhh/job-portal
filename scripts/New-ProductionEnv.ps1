$ErrorActionPreference = "Stop"

$secretDirectory = Join-Path $env:LOCALAPPDATA "JobPortal"
New-Item -ItemType Directory -Path $secretDirectory -Force | Out-Null
$envPath = Join-Path $secretDirectory ".env.production"
if (Test-Path $envPath) {
    throw ".env.production already exists; refusing to overwrite existing secrets."
}

$adminEmail = (Read-Host "Initial admin email").Trim()
$adminName = (Read-Host "Initial admin display name").Trim()
$adminContact = (Read-Host "Initial admin contact number").Trim()
if ($adminEmail -notmatch '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$') {
    throw "Enter a valid email address."
}
if ($adminName -notmatch "^[A-Za-z0-9 .'-]{1,100}$") {
    throw "Use a 1-100 character ASCII display name."
}
if ($adminContact -notmatch '^[0-9+() -]{5,32}$') {
    throw "Enter a valid contact number."
}

$securePassword = Read-Host "Initial admin password (16+ letters, numbers, ! @ # % _ -)" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $adminPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
if ($adminPassword -notmatch '^[A-Za-z0-9!@#%_-]{16,}$') {
    throw "Password must be at least 16 characters and use only the displayed character set."
}

$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $jwtBytes = New-Object byte[] 64
    $databaseBytes = New-Object byte[] 48
    $rng.GetBytes($jwtBytes)
    $rng.GetBytes($databaseBytes)
} finally {
    $rng.Dispose()
}

$jwtSecret = [Convert]::ToBase64String($jwtBytes)
$databasePassword = [Convert]::ToBase64String($databaseBytes)
$lines = @(
    "SPRING_PROFILES_ACTIVE=prod"
    "POSTGRES_DB=jobportal"
    "POSTGRES_USER=jobportal"
    "DB_URL=jdbc:postgresql://db:5432/jobportal"
    "DB_USERNAME=jobportal"
    "DB_PASSWORD=`"$databasePassword`""
    "JWT_SECRET=`"$jwtSecret`""
    "JWT_EXPIRATION_MS=3600000"
    "APP_UPLOAD_DIR=/app/uploads/resumes"
    "APP_UPLOAD_MAX_SIZE=10MB"
    "APP_PORT=8099"
    "ADMIN_BOOTSTRAP_ENABLED=true"
    "ADMIN_EMAIL=$adminEmail"
    "ADMIN_PASSWORD=`"$adminPassword`""
    "ADMIN_NAME=`"$adminName`""
    "ADMIN_CONTACT_NUMBER=`"$adminContact`""
    "DOMAIN=localhost"
)
Set-Content -Path $envPath -Value $lines -Encoding ascii

$currentUser = [Security.Principal.WindowsIdentity]::GetCurrent().Name
$icacls = Join-Path $env:SystemRoot "System32\icacls.exe"
& $icacls $envPath /inheritance:r /grant:r "${currentUser}:(F)" "SYSTEM:(F)" | Out-Null
if ($LASTEXITCODE -ne 0) {
    Remove-Item -Force $envPath
    throw "Could not restrict .env.production permissions."
}

Write-Host "Created protected secrets file at $envPath. It is outside the synced project folder."
Write-Host "After the first successful admin login, set ADMIN_BOOTSTRAP_ENABLED=false and clear ADMIN_PASSWORD in that file."