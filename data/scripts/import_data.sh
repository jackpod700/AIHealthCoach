#!/bin/sh
set -eu

python ./exercise/scripts/prepare_exercise.py

cp ./exercise/build/processed-exercise.csv /tmp/processed-exercise.csv
cp ./exercise/build/exercise-activity-options.csv /tmp/exercise-activity-options.csv

psql -v ON_ERROR_STOP=1 -f ./db/schema.sql
psql -v ON_ERROR_STOP=1 -f ./db/data.sql
psql -v ON_ERROR_STOP=1 -f ./exercise/scripts/import-exercise.sql
if [ "${FOOD_IMPORT_ENABLED:-false}" = "true" ]; then
    if [ ! -f ./foods/foods.csv ]; then
        echo "Missing ./foods/foods.csv. Generate foods.csv before importing foods."
        exit 1
    fi

    cp ./foods/foods.csv /tmp/foods.csv
    psql -v ON_ERROR_STOP=1 -f ./foods/import-foods.sql
    psql -c "SELECT COUNT(*) AS food_count FROM foods;"
else
    echo "Skipping foods import. Set FOOD_IMPORT_ENABLED=true to import data/foods/foods.csv."
fi
psql -v ON_ERROR_STOP=1 -f ./meals/seed-meals.sql
psql -c "SELECT COUNT(*) AS food_count FROM foods;"
psql -c "SELECT COUNT(*) AS physical_activity_count FROM physical_activities;"
psql -c "SELECT COUNT(*) AS exercise_activity_option_count FROM exercise_activity_options;"
