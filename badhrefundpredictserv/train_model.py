import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split, GridSearchCV, KFold
from datetime import datetime
import joblib
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import numpy as np

# --- LOAD DATA ---
df = pd.read_csv('dummy_tax_refund_data.csv')
df['created_at'] = pd.to_datetime(df['created_at'])
df['created_at_ordinal'] = df['created_at'].map(datetime.toordinal)

# --- FEATURE ENGINEERING ---
df['created_month'] = df['created_at'].dt.month
df['created_weekday'] = df['created_at'].dt.weekday
df['is_weekend'] = (df['created_weekday'] >= 5).astype(int)

# --- SELECT FEATURES ---
X = df[['tax_year', 'income', 'expense', 'tax_rate_percent', 'deducted', 'refund_amount',
        'created_at_ordinal', 'created_month', 'created_weekday', 'is_weekend']]
y = df['days_to_refund']

# --- CLEANING: ENSURE NUMERIC TYPES ---
X = X.copy()
for col in X.columns:
    X[col] = pd.to_numeric(X[col], errors='coerce')
X = X.fillna(0)

# --- OUTLIER HANDLING ---
# (Optional) Remove y values > 70 days as outlier
mask = y < 70
X = X[mask]
y = y[mask]

# --- TRAIN/TEST SPLIT ---
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)

# --- HYPERPARAMETER TUNING ---
params = {
    'learning_rate': [0.01, 0.05, 0.1],
    'max_depth': [3, 5, 7],
    'n_estimators': [50, 100, 200]
}
model = xgb.XGBRegressor(random_state=42)
grid = GridSearchCV(model, params, cv=3, scoring='neg_mean_absolute_error', verbose=1)
grid.fit(X_train, y_train)
print(f"Best hyperparameters: {grid.best_params_}")

# Select best model
model = grid.best_estimator_

# --- CROSS-VALIDATION PERFORMANCE ---
cv = KFold(n_splits=5, shuffle=True, random_state=42)
cv_maes = []
cv_rmses = []
cv_r2s = []

for train_idx, val_idx in cv.split(X_train):
    X_tr, X_val = X_train.iloc[train_idx], X_train.iloc[val_idx]
    y_tr, y_val = y_train.iloc[train_idx], y_train.iloc[val_idx]
    model.fit(X_tr, y_tr)
    y_val_pred = model.predict(X_val)
    cv_maes.append(mean_absolute_error(y_val, y_val_pred))
    cv_rmses.append(np.sqrt(mean_squared_error(y_val, y_val_pred)))
    cv_r2s.append(r2_score(y_val, y_val_pred))

print(f"K-fold CV MAE: {np.mean(cv_maes):.2f}")
print(f"K-fold CV RMSE: {np.mean(cv_rmses):.2f}")
print(f"K-fold CV R²: {np.mean(cv_r2s):.2f}")

# --- FINAL TEST PERFORMANCE ---
y_pred = model.predict(X_test)
mae = mean_absolute_error(y_test, y_pred)
mse = mean_squared_error(y_test, y_pred)
rmse = np.sqrt(mse)
r2 = r2_score(y_test, y_pred)

print(f"Test Mean Absolute Error (MAE): {mae:.2f}")
print(f"Test Mean Squared Error (MSE): {mse:.2f}")
print(f"Test Root Mean Squared Error (RMSE): {rmse:.2f}")
print(f"Test R² Score: {r2:.2f}")

# --- SAVE TRAINED MODEL ---
joblib.dump(model, 'xgb_refund_model.pkl')
