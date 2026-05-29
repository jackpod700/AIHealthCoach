FROM python:3.12-alpine

RUN apk add --no-cache postgresql-client

WORKDIR /app

COPY data/foods/raw ./raw
COPY data/foods/scripts ./scripts
COPY backend/src/main/resources/scripts/schema.sql ./schema.sql

RUN chmod +x ./scripts/import_foods.sh

CMD ["./scripts/import_foods.sh"]
