# Cyber Training Game

## Running the Prototype

**Requirements:** Java, Maven, Python 3 with `scikit-learn` and `joblib`

```bash
# Install Python deps (first time only)
python3 -m pip install scikit-learn joblib

# Start everything
./start.sh
```

This builds the backend, starts the WebSocket server on port 8887, and opens `frontend/index.html` in your browser. Press `Ctrl-C` to stop.

---

## What It Is

A browser-based cybersecurity training game. Players defend against waves of threats (phishing, brute force, malware) by typing the correct response. ML models score threat confidence; XP is awarded based on accuracy.

## Project Structure

```
backend/    Java WebSocket server + game logic
frontend/   Single-page HTML/JS/CSS client
start.sh    One-command launcher
```
