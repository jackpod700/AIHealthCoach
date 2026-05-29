param(
    [string]$ContainerName = "ai-health-postgres",
    [string]$Database = "ai_health_coach",
    [string]$User = "postgres"
)

$ErrorActionPreference = "Stop"

$DataRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$RepoRoot = Resolve-Path (Join-Path $DataRoot "..\..")
$BackendRoot = Join-Path $RepoRoot "backend"
$ProcessedCsv = Join-Path $DataRoot "build\processed-foods.csv"
$SchemaSql = Join-Path $BackendRoot "src\main\resources\scripts\schema.sql"
$ImportSql = Join-Path $PSScriptRoot "import-foods.sql"

python (Join-Path $PSScriptRoot "prepare_foods.py")

if (-not (Test-Path $ProcessedCsv)) {
    throw "Processed CSV was not created: $ProcessedCsv"
}

docker cp $ProcessedCsv "${ContainerName}:/tmp/processed-foods.csv"
docker cp $SchemaSql "${ContainerName}:/tmp/schema.sql"
docker cp $ImportSql "${ContainerName}:/tmp/import-foods.sql"
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/schema.sql
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/import-foods.sql
docker exec $ContainerName psql -U $User -d $Database -c "SELECT COUNT(*) AS food_count FROM foods;"
