param(
    [string]$ContainerName = "ai-health-postgres",
    [string]$Database = "ai_health_coach",
    [string]$User = "postgres"
)

$ErrorActionPreference = "Stop"

$DataRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$RepoRoot = Resolve-Path (Join-Path $DataRoot "..")
$DbRoot = Join-Path $DataRoot "db"
$FoodsRoot = Join-Path $DataRoot "foods"
$ProcessedCsv = Join-Path $FoodsRoot "build\processed-foods.csv"
$SchemaSql = Join-Path $DbRoot "schema.sql"
$DataSql = Join-Path $DbRoot "data.sql"
$ImportSql = Join-Path $FoodsRoot "scripts\import-foods.sql"
$MealSeedSql = Join-Path $DataRoot "meals\seed-meals.sql"

python (Join-Path $FoodsRoot "scripts\prepare_foods.py")

if (-not (Test-Path $ProcessedCsv)) {
    throw "Processed CSV was not created: $ProcessedCsv"
}

docker cp $ProcessedCsv "${ContainerName}:/tmp/processed-foods.csv"
docker cp $SchemaSql "${ContainerName}:/tmp/schema.sql"
docker cp $DataSql "${ContainerName}:/tmp/data.sql"
docker cp $ImportSql "${ContainerName}:/tmp/import-foods.sql"
docker cp $MealSeedSql "${ContainerName}:/tmp/seed-meals.sql"
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/schema.sql
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/data.sql
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/import-foods.sql
docker exec $ContainerName psql -v ON_ERROR_STOP=1 -U $User -d $Database -f /tmp/seed-meals.sql
docker exec $ContainerName psql -U $User -d $Database -c "SELECT COUNT(*) AS food_count FROM foods;"
