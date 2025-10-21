FROM python:3.11-slim

WORKDIR /app

# Install OS-level dependencies for XGBoost
RUN apt-get update && apt-get install -y libgomp1

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy your service code
COPY . .

EXPOSE 8090

CMD ["uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8090"]
