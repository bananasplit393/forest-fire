# Bygges fra api/:  docker build -f deploy/api/Dockerfile src/ForestFire.Api
# Build context er projektmappen. Dockerfile.dockerignore ved siden af bruges automatisk.
# SDK kører på byggemaskinen og krydskompilerer til målet (amd64 eller arm64)
FROM --platform=$BUILDPLATFORM mcr.microsoft.com/dotnet/sdk:9.0 AS build
ARG TARGETARCH
WORKDIR /src

COPY ForestFire.Api.csproj ./
RUN dotnet restore -a $TARGETARCH

COPY . .
RUN dotnet publish -c Release -a $TARGETARCH -o /app/publish --no-restore

FROM mcr.microsoft.com/dotnet/aspnet:9.0
WORKDIR /app

ENV ASPNETCORE_HTTP_PORTS=8080
EXPOSE 8080

COPY --from=build /app/publish .

USER $APP_UID
ENTRYPOINT ["dotnet", "ForestFire.Api.dll"]
