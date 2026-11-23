FROM python:3.12-slim

# git : requis par dbt (`dbt debug`, et `dbt deps` pour installer des paquets dbt)
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt   # versions figées : voir requirements.txt
COPY . .
