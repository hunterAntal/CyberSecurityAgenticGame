# Repo Bulletproof Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the public GitHub repo safe and professional for employer visibility — private backup, clean history, no academic content, MIT license, and a commit-message approval hook.

**Architecture:** Four sequential phases: (1) private backup before any destructive changes, (2) file cleanup committed on top of existing history, (3) full history rewrite into 3 clean milestones via automated rebase, (4) local-only git hook for commit message approval.

**Tech Stack:** git, GitHub CLI (`gh`), Python 3 (rebase automation script), bash (commit-msg hook)

---

## Task 1: Create Private Backup Repo

**Files:** none modified

- [ ] **Step 1: Create private GitHub repo**

```bash
gh repo create 5014_Game_2-archive --private --source=. --push
```

Expected output: `✓ Created repository <username>/5014_Game_2-archive on GitHub` followed by push progress.

- [ ] **Step 2: Verify backup exists**

```bash
gh repo view 5014_Game_2-archive --json name,visibility -q '"\(.name) is \(.visibility)"'
```

Expected: `5014_Game_2-archive is PRIVATE`

- [ ] **Step 3: Remove the archive remote (keep origin clean)**

```bash
git remote remove 5014_Game_2-archive 2>/dev/null || true
```

---

## Task 2: Remove slides/ and Add MIT LICENSE

**Files:**
- Modify: `.gitignore`
- Create: `LICENSE`
- Remove from tracking: `slides/` (entire directory)

- [ ] **Step 1: Untrack slides/**

```bash
git rm --cached -r slides/
```

Expected: lines like `rm 'slides/5014_Report.tex'`, `rm 'slides/xp-system.md'`, etc.

- [ ] **Step 2: Add slides/ to .gitignore**

Open `.gitignore` and add at the bottom:

```
# Academic report content
slides/
```

- [ ] **Step 3: Create LICENSE**

Create file `LICENSE` with this exact content (replace `Hunter Antal` if needed):

```
MIT License

Copyright (c) 2026 Hunter Antal

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

- [ ] **Step 4: Stage and commit**

```bash
git add .gitignore LICENSE
git commit -m "Remove academic content, add MIT license"
```

---

## Task 3: Rewrite History Into 3 Clean Commits

This task uses a Python script as a non-interactive `GIT_SEQUENCE_EDITOR` to squash all commits automatically — no manual editor interaction required.

**Files:**
- Create (temp, deleted after): `/tmp/rebase_transform.py`

**Target history:**
| # | Message |
|---|---------|
| 1 | `Initial prototype: JADE multi-agent cybersecurity training game` |
| 2 | `Fix DefenderAgent BLOCK move evaluation` |
| 3 | `Portfolio release: clean structure, README, demo GIF, MIT license` |

**Grouping logic (25 commits + Task 2 = 26 total, oldest-first):**
- Indices 0–16 → Group 1 (from "First" through "Add XP system overview")
- Index 17 → Group 2 (DefenderAgent fix — kept standalone)
- Indices 18–25 → Group 3 (all portfolio cleanup, gif, gitignore, slides, spec, license)

- [ ] **Step 1: Write the rebase transform script**

Create `/tmp/rebase_transform.py`:

```python
import sys

with open(sys.argv[1]) as f:
    lines = [l for l in f if l.strip() and not l.startswith('#')]

hashes = [l.split()[1] for l in lines]

msgs = [
    "Initial prototype: JADE multi-agent cybersecurity training game",
    "Fix DefenderAgent BLOCK move evaluation",
    "Portfolio release: clean structure, README, demo GIF, MIT license",
]

result = []

# Group 1: first 17 commits squashed together
result.append(f"pick {hashes[0]}\n")
result.append(f'exec git commit --amend -m "{msgs[0]}"\n')
for h in hashes[1:17]:
    result.append(f"fixup {h}\n")

# Group 2: single commit (index 17)
result.append(f"pick {hashes[17]}\n")
result.append(f'exec git commit --amend -m "{msgs[1]}"\n')

# Group 3: remaining commits (index 18 onward)
result.append(f"pick {hashes[18]}\n")
result.append(f'exec git commit --amend -m "{msgs[2]}"\n')
for h in hashes[19:]:
    result.append(f"fixup {h}\n")

with open(sys.argv[1], 'w') as f:
    f.writelines(result)
```

- [ ] **Step 2: Run the automated rebase**

```bash
GIT_SEQUENCE_EDITOR='python3 /tmp/rebase_transform.py' git rebase -i --root
```

Expected: rebase runs silently through all 26 commits, prints `Successfully rebased and updated refs/heads/main.`

If you see conflicts, run `git rebase --abort` and report back before continuing.

- [ ] **Step 3: Verify history**

```bash
git log --oneline
```

Expected output (exactly 3 lines):
```
xxxxxxx Portfolio release: clean structure, README, demo GIF, MIT license
xxxxxxx Fix DefenderAgent BLOCK move evaluation
xxxxxxx Initial prototype: JADE multi-agent cybersecurity training game
```

- [ ] **Step 4: Verify slides/ is gone**

```bash
git ls-files slides/
```

Expected: no output (empty).

- [ ] **Step 5: Force push to GitHub**

```bash
git push --force origin main
```

Expected: `+ xxxxxxx...xxxxxxx main -> main (forced update)`

- [ ] **Step 6: Clean up temp script**

```bash
rm /tmp/rebase_transform.py
```

---

## Task 4: Install Commit Message Approval Hook

The hook lives in `.git/hooks/commit-msg` — local only, never tracked in git.

**Files:**
- Create: `.git/hooks/commit-msg`

- [ ] **Step 1: Write the hook**

Create `.git/hooks/commit-msg` with this exact content:

```bash
#!/usr/bin/env bash
set -e

MSG_FILE="$1"

# ── Skip during rebase (exec steps would prompt unnecessarily) ────────────
GIT_DIR=$(git rev-parse --git-dir 2>/dev/null || echo ".git")
if [ -d "$GIT_DIR/rebase-merge" ] || [ -d "$GIT_DIR/rebase-apply" ]; then
    exit 0
fi

# ── Strip attribution lines ───────────────────────────────────────────────
python3 - "$MSG_FILE" << 'PYEOF'
import sys

path = sys.argv[1]
with open(path) as f:
    lines = f.readlines()

cleaned = [
    l for l in lines
    if not l.startswith("Co-Authored-By:")
    and "Generated with" not in l
    and "\U0001f916" not in l  # 🤖 emoji
]

# Remove trailing blank lines
while cleaned and not cleaned[-1].strip():
    cleaned.pop()
if cleaned:
    cleaned.append("\n")

with open(path, "w") as f:
    f.writelines(cleaned)
PYEOF

MSG=$(cat "$MSG_FILE")

# ── Skip prompt if message is empty (let git handle it) ───────────────────
if [ -z "$(echo "$MSG" | tr -d '[:space:]')" ]; then
    exit 0
fi

# ── Interactive approval loop ─────────────────────────────────────────────
while true; do
    printf "\n"
    printf "Commit message:\n"
    printf "────────────────────────────────────────\n"
    printf "%s\n" "$MSG"
    printf "────────────────────────────────────────\n\n"
    printf "[A]pprove  [E]dit  [Q]uit : "

    read -r CHOICE < /dev/tty

    case "${CHOICE,,}" in
        e)
            ${EDITOR:-nano} "$MSG_FILE" < /dev/tty > /dev/tty
            # Re-strip after edit
            python3 - "$MSG_FILE" << 'PYEOF'
import sys
path = sys.argv[1]
with open(path) as f:
    lines = f.readlines()
cleaned = [l for l in lines
    if not l.startswith("Co-Authored-By:")
    and "Generated with" not in l
    and "\U0001f916" not in l]
while cleaned and not cleaned[-1].strip():
    cleaned.pop()
if cleaned:
    cleaned.append("\n")
with open(path, "w") as f:
    f.writelines(cleaned)
PYEOF
            MSG=$(cat "$MSG_FILE")
            ;;
        q)
            printf "Commit aborted.\n"
            exit 1
            ;;
        a|"")
            break
            ;;
        *)
            printf "Please enter A, E, or Q.\n"
            ;;
    esac
done

exit 0
```

- [ ] **Step 2: Make hook executable**

```bash
chmod +x .git/hooks/commit-msg
```

- [ ] **Step 3: Test — approval path**

```bash
echo "test file" > /tmp/hook-test.txt
git add /tmp/hook-test.txt 2>/dev/null || true
git commit -m "test: hook approval" --allow-empty
```

Expected: prompt appears showing the message. Type `A` — commit succeeds.

Then clean up:
```bash
git reset HEAD~1
```

- [ ] **Step 4: Test — quit path**

```bash
git commit -m "test: hook quit" --allow-empty
```

Expected: prompt appears. Type `Q` — output shows `Commit aborted.`, commit does NOT happen.

- [ ] **Step 5: Test — attribution stripping**

```bash
git commit --allow-empty -m "$(printf 'test: strip check\n\nCo-Authored-By: Claude Sonnet <noreply@anthropic.com>')"
```

Expected: prompt shows ONLY `test: strip check` — the `Co-Authored-By` line is gone. Type `Q` to abort.

---

## Verification Checklist

```bash
# 1. Exactly 3 commits
git log --oneline | wc -l   # → 3

# 2. No slides/ tracked
git ls-files slides/         # → (empty)

# 3. LICENSE present
head -1 LICENSE              # → MIT License

# 4. Hook is executable
ls -la .git/hooks/commit-msg # → -rwxr-xr-x ...

# 5. Check GitHub remotely
gh repo view --web           # visually confirm: 3 commits, LICENSE badge, no slides/
```
