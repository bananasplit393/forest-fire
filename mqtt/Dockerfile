# Bygges fra mqtt/:  docker build -t forest-fire-mqtt .
# Dockerfile.dockerignore ved siden af bruges automatisk.
# SDK kører på byggemaskinen og krydskompilerer til målet (amd64 eller arm64)
FROM --platform=$BUILDPLATFORM mcr.microsoft.com/dotnet/sdk:9.0 AS build
ARG TARGETARCH
WORKDIR /src

COPY ForestFire.Mqtt.csproj ./
RUN dotnet restore -a $TARGETARCH

COPY . .
RUN dotnet publish -c Release -a $TARGETARCH -o /app/publish --no-restore

# servicen er ikke en webapp, så runtime-imaget er nok (som Edger)
FROM mcr.microsoft.com/dotnet/runtime:9.0
WORKDIR /app

COPY --from=build /app/publish .

USER $APP_UID
ENTRYPOINT ["dotnet", "ForestFire.Mqtt.dll"]
