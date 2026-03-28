# Repo Bulletproof Design
**Date:** 2026-03-28
**Project:** 5014_Game_2 (public GitHub portfolio pin)

---

## Goal

Make the public repository safe and professional for employer visibility:
- No academic or sensitive content tracked
- Clean, intentional commit history
- Automatic commit message approval gate that strips any AI attribution

---

## Section 1 — Private Backup

Before any destructive changes, push the current state to a new **private** GitHub repo (`5014_Game_2-archive`).

- Created via `gh repo create --private`
- Push all current branches and history
- Serves as a safe working copy for future report writing

---

## Section 2 — Clean Tracked Files

Two changes applied as part of the rebased history:

**Remove `slides/`:**
- `git rm --cached -r slides/` — untracks all academic content (report .tex, .zip, drawio files, definitions)
- Add `slides/` to `.gitignore` — prevents re-tracking; local files remain untouched for report writing

**Add MIT LICENSE:**
- `LICENSE` file added with MIT license, year 2026, Hunter Antal
- Signals to employers that the code is readable and runnable

---

## Section 3 — History Rewrite

Interactive rebase squashes all current commits into 3 clean milestones:

| # | Commit Message | Absorbs |
|---|---------------|---------|
| 1 | `Initial prototype: JADE multi-agent cybersecurity training game` | d6ed5c2 (Prototype 1.0), c4ee1d3 (initial README), 699281e (diagrams), 3841157 (XP docs) |
| 2 | `Fix DefenderAgent BLOCK move evaluation` | 2e22b24 (kept as-is — meaningful isolated fix) |
| 3 | `Portfolio release: clean structure, README, demo GIF, MIT license` | 5888a2c, cea276d, 4ce94f6, 0764d05, 3074045, cf0c1e9, 774161b — all cleanup, revert, gif, gitignore, and slides removal commits |

After rebase: `git push --force origin main`

---

## Section 4 — Commit Message Hook

A local-only git hook at `.git/hooks/commit-msg`:

**Behaviour:**
1. Strips any `Co-Authored-By:` lines from the message
2. Strips any `🤖 Generated with` lines
3. Displays the cleaned message and prompts:
   ```
   Commit message:
   > <message>

   [A]pprove  [E]dit  [Q]uit :
   ```
   - `A` — proceeds with commit
   - `E` — opens `$EDITOR` (or `nano`) to rewrite the message, then re-prompts
   - `Q` — aborts with exit code 1

**Constraints:**
- Lives in `.git/hooks/` — never tracked in git
- Must be executable (`chmod +x`)
- Works with `git commit -m "..."` and interactive `git commit`
- If no TTY is available (e.g. CI), falls through and approves automatically

---

## Verification

1. `git log --oneline` — confirm exactly 3 commits on main
2. `git ls-files slides/` — returns nothing
3. `cat LICENSE` — MIT license present
4. `git commit -m "test: hook"` then abort — confirm prompt appears and `Q` aborts cleanly
5. `git commit -m "test Co-Authored-By: Claude"` — confirm attribution line is stripped before prompt
6. Check GitHub — public repo shows clean 3-commit history, no slides/, LICENSE visible
