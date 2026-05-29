#!/bin/sh
set -eu

python ./scripts/prepare_foods.py

cp ./build/processed-foods.csv /tmp/processed-foods.csv

psql -v ON_ERROR_STOP=1 -f ./schema.sql
psql -v ON_ERROR_STOP=1 -f ./scripts/import-foods.sql
psql -c "SELECT COUNT(*) AS food_count FROM foods;"
