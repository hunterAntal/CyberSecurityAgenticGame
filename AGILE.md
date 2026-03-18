# Agile Development Documentation
## ESOF 5014 — Cybersecurity Training Game (JADE MAS)

---

## Project Overview

A terminal-style cybersecurity training game built on a JADE multi-agent system. Players respond to live attack agents (phishing, brute-force, malware) using defensive moves. A DefenderAgent provides utility-based recommendations and a MonitoringScoringAgent tracks XP and performance ratings across waves.

**Team Roles**

| Member | Role |
|---|---|
| Teammate | Attack agents (PhishingAttackAgent, BruteForceAgent, MalwarePropagationAgent) + ML models |
| Hunter | DefenderAgent, MonitoringScoringAgent, WebSocket bridge, game flow, frontend |

---

## Product Backlog

| ID | User Story | Priority | Points | Status |
|---|---|---|---|---|
| US-01 | As a player, I want to see live attack alerts so I know what threat I am facing | High | 3 | Done |
| US-02 | As a player, I want to choose a defensive move (PATCH, SCAN, BLOCK, ANALYZE) | High | 3 | Done |
| US-03 | As a player, I want feedback on whether my move was effective | High | 2 | Done |
| US-04 | As a player, I want to type HELP and get a reasoned recommendation | Medium | 3 | Done |
| US-05 | As a player, I want to earn XP and see my score update after each wave | High | 3 | Done |
| US-06 | As a player, I want a session summary with my rating when I finish | Medium | 2 | Done |
| US-07 | As a player, I want to restart a new game without restarting the server | Medium | 2 | Done |
| US-08 | As a developer, I want attack agents registered in the JADE DF | High | 2 | Done |
| US-09 | As a developer, I want a single script to build and launch the full system | Low | 1 | Done |

---

## Sprint Plan

### Sprint 1 — Foundation & Attack Agents
**Duration:** Week 1–2
**Goal:** Project scaffolding, ML model training, and attack agent stubs registered in JADE.

| Task | Owner | Status |
|---|---|---|
| Set up Maven project structure | Hunter | Done |
| Train phishing detection model | Teammate | Done |
| Train brute-force classification model | Teammate | Done |
| Train malware propagation model | Teammate | Done |
| Implement PhishingAttackAgent (JADE + DF) | Teammate | Done |
| Implement BruteForceAgent (JADE + DF) | Teammate | Done |
| Implement MalwarePropagationAgent (JADE + DF) | Teammate | Done |

**Sprint 1 Review:** All three attack agents compile and register with the JADE Directory Facilitator. ML models are validated and persisted.

---

### Sprint 2 — Core Agent Logic
**Duration:** Week 3–4
**Goal:** Implement DefenderAgent and MonitoringScoringAgent with their mathematical models.

| Task | Owner | Status |
|---|---|---|
| Define utility function U(action) for DefenderAgent | Hunter | Done |
| Implement `recommendMove()` — highest U(action) | Hunter | Done |
| Implement `evaluateMove()` — SUPER_EFFECTIVE / NORMAL / WEAK | Hunter | Done |
| Define XP scoring model B(e, h, r) | Hunter | Done |
| Implement `processOutcome()` with XP computation | Hunter | Done |
| Implement `endSession()` returning GameSummary + rating | Hunter | Done |
| Register both agents with JADE DF | Hunter | Done |

**Sprint 2 Review:** DefenderAgent correctly recommends BLOCK vs PHISHING, PATCH vs BRUTEFORCE, SCAN vs MALWARE based on utility scores. XP math verified for all help/retry combinations.

---

### Sprint 3 — WebSocket Bridge & Game Flow
**Duration:** Week 5–6
**Goal:** Wire all agents together through a shared state bridge and implement the full game loop.

| Task | Owner | Status |
|---|---|---|
| Implement GameStateBridge (WebSocket server, port 8887) | Hunter | Done |
| Implement singleton pattern for bridge agent access | Hunter | Done |
| Implement GameFlowController with blocking input queue | Hunter | Done |
| Wire HELP flow → DefenderAgent → bridge broadcast | Hunter | Done |
| Wire move outcome → MonitoringScoringAgent → SCORE_UPDATE | Hunter | Done |
| Implement FINISH → GAME_OVER message with GameSummary | Hunter | Done |
| Implement READY restart (new session without server restart) | Hunter | Done |
| All dialog templates wired (announcements, outcomes, weak feedback) | Hunter | Done |

**Sprint 3 Review:** Full game loop functional end-to-end. Both agents write to bridge independently. Verified no write conflicts using synchronized methods and AtomicReference for agent registration.

---

### Sprint 4 — Frontend & Integration
**Duration:** Week 7
**Goal:** Terminal UI, startup script, and full system integration test.

| Task | Owner | Status |
|---|---|---|
| Build terminal frontend (HTML/CSS/JS, no frameworks) | Hunter | Done |
| Implement typewriter effect (30ms/char) with message queue | Hunter | Done |
| Implement SCORE_UPDATE and GAME_OVER display | Hunter | Done |
| Input validation and unknown command handling | Hunter | Done |
| Write `start.sh` — build, launch backend, open browser | Hunter | Done |
| End-to-end integration test across all wave/XP scenarios | Hunter | Done |

**Sprint 4 Review:** `./start.sh` builds, starts JADE, waits for port 8887, and opens the browser in one command. All 9 user stories verified complete.

---

## Definition of Done

A user story is considered **Done** when:
- Code compiles with `mvn compile` (zero errors)
- The feature works end-to-end through the WebSocket
- Edge cases (HELP used, retry, ANALYZE on any threat) produce correct output
- No regressions in previously completed stories

---

## Sprint Velocity

| Sprint | Planned Points | Completed Points |
|---|---|---|
| Sprint 1 | 8 | 8 |
| Sprint 2 | 10 | 10 |
| Sprint 3 | 12 | 12 |
| Sprint 4 | 6 | 6 |
| **Total** | **36** | **36** |

---

## Key Iterative Improvements

| Iteration | Change | Reason |
|---|---|---|
| Sprint 2 → 3 | Moved scoring state fully into MonitoringScoringAgent | Separated concerns — bridge should not own business logic |
| Sprint 3 → 4 | Added message queue on frontend | Prevented dialog lines from overlapping during rapid broadcasts |
| Sprint 4 | Capped HELP waves at 25 XP flat (removed tiered help scoring) | Simplified model, clearer penalty for using hints |
| Sprint 4 | Wrapped game loop to allow READY restart | Usability — testers should not need to refresh between sessions |
