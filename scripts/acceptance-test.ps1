# =====================================================================
# CoWorkHub - 14 escenarios de aceptacion contra la API en ejecucion.
# Equivalente a postman/CoWorkHub.postman_collection.json, sin Postman.
# Requisito: reiniciar la API antes (import.sql recarga las reservas 1, 2 y 3).
# Uso:  powershell -ExecutionPolicy Bypass -File scripts\acceptance-test.ps1
# =====================================================================
param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"
$script:passed = 0
$script:failed = 0

function Call($method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $params = @{ UseBasicParsing = $true; Method = $method; Uri = "$BaseUrl$path"; Headers = $headers }
    if ($body) { $params["ContentType"] = "application/json; charset=utf-8"; $params["Body"] = [Text.Encoding]::UTF8.GetBytes($body) }
    try {
        $r = Invoke-WebRequest @params
        $status = [int]$r.StatusCode; $content = $r.Content
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        $status = [int]$resp.StatusCode
        # PowerShell 5.1 a veces ya consumio el cuerpo; ErrorDetails lo conserva
        $content = $_.ErrorDetails.Message
        if (-not $content) {
            $reader = New-Object IO.StreamReader($resp.GetResponseStream())
            $content = $reader.ReadToEnd()
        }
    }
    $json = $null
    if ($content) { try { $json = $content | ConvertFrom-Json } catch {} }
    return [pscustomobject]@{ Status = $status; Json = $json; Raw = $content }
}

function Check($name, $condition, $detail) {
    if ($condition) { $script:passed++; Write-Host "  [OK]    $name" -ForegroundColor Green }
    else { $script:failed++; Write-Host "  [FALLO] $name -> $detail" -ForegroundColor Red }
}

function Login($user, $pass) {
    (Call POST "/auth/login" $null "{`"usuario`":`"$user`",`"password`":`"$pass`"}").Json.token
}

$admin = Login "admin" "Admin123*"
$recep = Login "recepcion" "Recep123*"
$ana = Login "ana" "Ana123*"
Check "Login de admin, recepcion y ana" ($admin -and $recep -and $ana) "no se obtuvo token"

$fecha = (Get-Date).AddDays((Get-Random -Minimum 30 -Maximum 330)).ToString("yyyy-MM-dd")
function Booking($room, $member, $from, $to, $extra) {
    "{`"salaId`":$room,`"miembroId`":$member,`"inicio`":`"${fecha}T${from}:00`",`"fin`":`"${fecha}T${to}:00`",`"asistentes`":2$extra}"
}
Write-Host "Fecha de prueba: $fecha"

Write-Host "`nEscenario 1 - Reservar sala libre con 2 servicios"
$r = Call POST "/api/reservas" $admin (Booking 1 3 "09:00" "10:00" ',"servicios":[{"servicioId":1,"cantidad":2},{"servicioId":3,"cantidad":1}]')
Check "201 con costo calculado" ($r.Status -eq 201 -and $r.Json.servicios.Count -eq 2 -and $r.Json.costoServicios -eq 28000 -and $r.Json.costoTotal -eq ($r.Json.costoSala + $r.Json.costoServicios)) "$($r.Status) $($r.Raw)"
$s1 = $r.Json.id

Write-Host "`nEscenario 2 - Reserva que se cruza"
$r = Call POST "/api/reservas" $admin (Booking 1 3 "09:30" "10:30" "")
Check "409 RN-01" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-01") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 3 - Reserva consecutiva 10:00-11:00"
$r = Call POST "/api/reservas" $admin (Booking 1 3 "10:00" "11:00" "")
Check "201" ($r.Status -eq 201) "$($r.Status) $($r.Raw)"
$s3 = $r.Json.id

Write-Host "`nEscenario 4 - 45 minutos / fuera del horario"
$r = Call POST "/api/reservas" $admin (Booking 1 3 "12:00" "12:45" "")
Check "45 min -> 409 RN-02" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-02") "$($r.Status) $($r.Raw)"
$r = Call POST "/api/reservas" $admin (Booking 1 3 "05:00" "06:00" "")
Check "05:00 -> 409 RN-03" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-03") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 5 - Cancelar con 30 h de anticipacion"
$r = Call POST "/api/reservas/1/cancelar" $ana
Check "200, cargo 0 y horas devueltas" ($r.Status -eq 200 -and $r.Json.cargoCancelacion -eq 0 -and $r.Json.horasIncluidasAplicadas -eq 0) "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 6 - Cancelar con 5 h de anticipacion"
$r = Call POST "/api/reservas/2/cancelar" $ana
Check "200, cargo 50 % (34000)" ($r.Status -eq 200 -and $r.Json.cargoCancelacion -eq 34000) "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 7 - Cancelar con 1 h de anticipacion"
$r = Call POST "/api/reservas/3/cancelar" $ana
Check "409 RN-08" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-08") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 8 - Confirmar una reserva cancelada"
$r = Call POST "/api/reservas/4/confirmar" $recep
Check "409 RN-09" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-09") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 9 - Miembro consulta la reserva de otro"
$r = Call GET "/api/reservas/5" $ana
Check "403" ($r.Status -eq 403) "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 10 - Sin token / token alterado"
$r = Call GET "/api/reservas" $null
Check "Sin token -> 401 con formato uniforme" ($r.Status -eq 401 -and $r.Json.codigo -and $r.Json.error -and $r.Json.timestamp) "$($r.Status) $($r.Raw)"
$r = Call GET "/api/reservas" ($admin + "x")
Check "Token alterado -> 401" ($r.Status -eq 401) "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 11 - Eliminar sala con reservas futuras"
$r = Call DELETE "/api/salas/1" $admin
Check "409 RN-12" ($r.Status -eq 409 -and $r.Json.codigo -eq "RN-12") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 12 - Crear sede sin nombre"
$r = Call POST "/api/sedes" $admin '{"ciudad":"Bucaramanga","direccion":"Calle 1","horaApertura":"08:00","horaCierre":"18:00"}'
Check "400" ($r.Status -eq 400 -and $r.Json.codigo -eq "VALIDACION") "$($r.Status) $($r.Raw)"

Write-Host "`nEscenario 13 - Dos solicitudes simultaneas (misma sala y horario)"
$body = Booking 2 3 "15:00" "16:00" ""
$jobs = 1..2 | ForEach-Object {
    Start-Job -ScriptBlock {
        param($url, $token, $body)
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$url/api/reservas" -Headers @{ Authorization = "Bearer $token" } -ContentType "application/json" -Body $body
            "$([int]$r.StatusCode)|$(($r.Content | ConvertFrom-Json).id)"
        } catch { "$([int]$_.Exception.Response.StatusCode)|" }
    } -ArgumentList $BaseUrl, $admin, $body
}
$results = $jobs | Wait-Job | Receive-Job; $jobs | Remove-Job
$codes = $results | ForEach-Object { [int]($_.Split('|')[0]) }
$s13 = ($results | Where-Object { $_.StartsWith("201|") } | ForEach-Object { $_.Split('|')[1] })
Check "Una 201 y una 409 (recibidos: $($codes -join ', '))" ((($codes | Where-Object { $_ -eq 201 }).Count -eq 1) -and (($codes | Where-Object { $_ -eq 409 }).Count -eq 1)) "codigos $($codes -join ', ')"

Write-Host "`nEscenario 14 - Listar miembros ?estado=SUSPENDED"
$r = Call GET "/api/miembros?estado=SUSPENDED" $recep
$list = @($r.Json)
Check "200 solo suspendidos" ($r.Status -eq 200 -and $list.Count -gt 0 -and -not ($list | Where-Object { $_.estado -ne "SUSPENDED" })) "$($r.Status) $($r.Raw)"

Write-Host "`nLimpieza: cancelando reservas creadas"
foreach ($id in @($s1, $s3, $s13)) { if ($id) { $null = Call POST "/api/reservas/$id/cancelar" $admin } }

Write-Host "`nResultado: $script:passed OK, $script:failed fallos" -ForegroundColor ($(if ($script:failed -eq 0) { "Green" } else { "Red" }))
if ($script:failed -gt 0) { exit 1 }
