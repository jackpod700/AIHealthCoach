FROM python:3.12-alpine

RUN apk add --no-cache postgresql-client

WORKDIR /app

COPY data/scripts ./scripts
COPY data/foods ./foods
COPY data/db ./db
COPY data/meals ./meals

RUN chmod +x ./scripts/import_data.sh

CMD ["./scripts/import_data.sh"]
