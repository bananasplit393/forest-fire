# Starter det hele med bærbarens IP i dashboardets footer: .\demo.ps1
$route = Get-NetRoute -DestinationPrefix 0.0.0.0/0 | Sort-Object RouteMetric | Select-Object -First 1
$env:HOST_IP = (Get-NetIPAddress -InterfaceIndex $route.ifIndex -AddressFamily IPv4).IPAddress
Write-Host "IP: $env:HOST_IP"
docker compose up -d --build