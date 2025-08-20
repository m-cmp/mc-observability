FROM python:3.11-slim

# Install system dependencies  
RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*    

# Install uv
RUN pip install --no-cache-dir uv

# Set working directory
WORKDIR /app

# Copy project files
COPY . /app

# Install project dependencies
RUN uv sync

EXPOSE 9001

CMD ["uv", "run", "src/server.py", "--host", "0.0.0.0", "--transport", "sse"]