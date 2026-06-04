#!/bin/sh
set -eu

python ./foods/scripts/prepare_foods.py
python ./exercise/scripts/prepare_exercise.py

cp ./foods/build/processed-foods.csv /tmp/processed-foods.csv
cp ./exercise/build/processed-exercise.csv /tmp/processed-exercise.csv

psql -v ON_ERROR_STOP=1 -f ./db/schema.sql
psql -v ON_ERROR_STOP=1 -f ./db/data.sql
psql -v ON_ERROR_STOP=1 -f ./foods/scripts/import-foods.sql
psql -v ON_ERROR_STOP=1 -f ./exercise/scripts/import-exercise.sql
psql -v ON_ERROR_STOP=1 -f ./meals/seed-meals.sql
psql -c "SELECT COUNT(*) AS food_count FROM foods;"
