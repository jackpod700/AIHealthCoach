FROM python:3.12-alpine

RUN apk add --no-cache postgresql-client

WORKDIR /app

COPY data/scripts ./scripts
COPY data/foods ./foods
COPY data/exercise ./exercise
COPY data/db ./db
COPY data/meals ./meals

RUN sed -i 's/\r$//' ./scripts/import_data.sh && chmod +x ./scripts/import_data.sh

CMD ["./scripts/import_data.sh"]
