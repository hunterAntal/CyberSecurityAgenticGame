---
marp: true
theme: default
paginate: true
style: |
  section {
    font-family: 'Segoe UI', sans-serif;
  }
  table {
    width: 100%;
  }
  th {
    background-color: #2d6cdf;
    color: white;
  }
  code {
    background: #f0f0f0;
    padding: 2px 6px;
    border-radius: 4px;
  }
---

# XP System Overview

**XP represents a defender's skill rating** — it rises with good decisions and falls with bad ones, starting at **500 XP**.

### How XP Moves

| Situation | XP Change |
|---|---|
| SUPER_EFFECTIVE move (no help, first try) | **+150 XP** |
| NORMAL move (no help, first try) | **+100 XP** |
| Correct answer using retry OR help | **+25 XP** |
| Wrong answer | **−(proportional deduction)** |
| XP reaches 0 | **Game Over** |

### Performance Tiers (final score)

| XP Range | Rank |
|---|---|
| 0 – 300 | RECRUIT |
| 301 – 600 | ANALYST |
| 601 – 900 | SPECIALIST |
| 901 + | DEFENDER |

---

# The Math — XP Deduction Formula

When the player answers **incorrectly**, the `MonitoringScoringAgent` calls `deductXP(confidence)`:

```
deduction = MAX(1, FLOOR(totalXP × confidence))
totalXP   = MAX(0, totalXP − deduction)
```

> `confidence` is the ML model's probability score (0.0 – 1.0) for the current threat wave.

### Why this matters

- **High confidence threat misidentified → large penalty.**
  The ML model was sure what the threat was; missing it is costly.
- **Low confidence threat misidentified → small penalty.**
  Ambiguous threats are penalised lightly.
- The `MAX(1, ...)` guarantees at least 1 XP is always lost — no free passes.

### Example

| State | Calculation | Result |
|---|---|---|
| 400 XP, confidence = 0.75 | `MAX(1, FLOOR(400 × 0.75))` = 300 | **100 XP remaining** |
| 50 XP, confidence = 0.10 | `MAX(1, FLOOR(50 × 0.10))` = 5 | **45 XP remaining** |
