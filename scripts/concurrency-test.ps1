# =====================================================================
# Escenario 13 - Dos solicitudes SIMULTANEAS para la misma sala y horario.
# Resultado esperado: exactamente una 201 y una 409 (RN-01).
# Uso (con la API corriendo):  powershell -ExecutionPolicy Bypass -File scripts\concurrency-test.ps1
# =====================================================================
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Requests = 2
)

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/login" -ContentType "application/json" `
    -Body '{"usuario":"admin","password":"Admin123*"}'
$token = $login.token

$day = (Get-Date).AddDays((Get-Random -Minimum 400 -Maximum 700)).ToString("yyyy-MM-dd")
$body = "{`"salaId`":2,`"miembroId`":3,`"inicio`":`"${day}T15:00:00`",`"fin`":`"${day}T16:00:00`",`"asistentes`":2}"
Write-Host "Enviando $Requests solicitudes simultaneas para la sala 2 el $day 15:00-16:00..."

$jobs = 1..$Requests | ForEach-Object {
    Start-Job -ScriptBlock {
        param($url, $token, $body)
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$url/api/reservas" `
                -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body $body
            [int]$r.StatusCode
        } catch {
            [int]$_.Exception.Response.StatusCode
        }
    } -ArgumentList $BaseUrl, $token, $body
}

$codes = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
Write-Host "Codigos HTTP recibidos: $($codes -join ', ')"
$created = ($codes | Where-Object { $_ -eq 201 }).Count
$conflict = ($codes | Where-Object { $_ -eq 409 }).Count
if ($created -eq 1 -and $conflict -eq ($Requests - 1)) {
    Write-Host "OK: una reserva creada (201) y $conflict rechazada(s) con 409 RN-01" -ForegroundColor Green
} else {
    Write-Host "FALLO: se esperaba exactamente una 201" -ForegroundColor Red
    exit 1
}
