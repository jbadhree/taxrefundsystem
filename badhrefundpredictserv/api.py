from fastapi import FastAPI
from pydantic import BaseModel
import joblib
import pandas as pd
from datetime import datetime

app = FastAPI()
model = joblib.load("xgb_refund_model.pkl")

class RefundRequest(BaseModel):
    tax_year: int
    income: float
    expense: float
    tax_rate_percent: float
    deducted: float
    refund_amount: float
    created_at: str  # ISO format, e.g. "2023-01-10"

@app.post("/predict")
def predict_refund_days(req: RefundRequest):
    # Feature engineering (mirror your training pipeline)
    created_at_datetime = datetime.fromisoformat(req.created_at)
    row = {
        "tax_year": req.tax_year,
        "income": req.income,
        "expense": req.expense,
        "tax_rate_percent": req.tax_rate_percent,
        "deducted": req.deducted,
        "refund_amount": req.refund_amount,
        "created_at_ordinal": created_at_datetime.toordinal(),
        "created_month": created_at_datetime.month,
        "created_weekday": created_at_datetime.weekday(),
        "is_weekend": int(created_at_datetime.weekday() >= 5)
    }

    # Prepare DataFrame for model
    df = pd.DataFrame([row])
    for col in df.columns:
        df[col] = pd.to_numeric(df[col], errors='coerce')
    df = df.fillna(0)

    # Predict days to refund
    y_pred = model.predict(df)[0]
    return {"predicted_days_to_refund": float(y_pred)}

# To run locally: uvicorn api:app --reload
