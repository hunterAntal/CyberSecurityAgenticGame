package game;

import agents.BruteForceAgent;
import agents.DefenderAgent;
import agents.MalwarePropagationAgent;
import agents.MonitoringScoringAgent;
import agents.PhishingAttackAgent;
import bridge.GameStateBridge;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * GameFlowController — orchestrates the full game session.
 * Runs on its own thread, blocks on player input via a BlockingQueue.
 */
public class GameFlowController implements Runnable {

    private static final String[] THREATS = {"PHISHING", "BRUTEFORCE", "MALWARE"};
    private static final Random RNG = new Random();

    private final GameStateBridge bridge;
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    // ── Per-wave state ─────────────────────────────────────────────────────
    private String currentThreat;
    private double currentConfidence;
    private boolean helpUsedThisWave;
    private boolean retryThisWave;

    public GameFlowController(GameStateBridge bridge) {
        this.bridge = bridge;
    }

    // ── Thread entry ───────────────────────────────────────────────────────
    @Override
    public void run() {
        try {
            playIntro();

            // Outer loop: restart a full session each time player types READY
            while (running) {
                // Wait for READY
                while (running) {
                    String input = waitForInput();
                    if ("READY".equals(input)) break;
                    bridge.sendDialog("SYSTEM", "Type READY to begin.");
                }

                MonitoringScoringAgent scoring = bridge.getScoringAgent();
                if (scoring != null) {
                    scoring.startSession();
                }

                // Play waves until FINISH
                playSession();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Runs waves until the player types FINISH. Returns when session ends. */
    private void playSession() throws InterruptedException {
        while (running) {
            startWave();
            boolean waveComplete = false;

            while (!waveComplete && running) {
                String input = waitForInput();

                switch (input) {
                    case "HELP" -> handleHelp();
                    case "PATCH", "SCAN", "BLOCK", "ANALYZE" -> {
                        boolean complete = handleMove(input);
                        if (complete) {
                            waveComplete = true;
                            String next = waitForNextOrFinish();
                            if ("FINISH".equals(next)) {
                                handleFinish();
                                return; // back to outer loop → waits for READY
                            }
                            // NEXT → loop back to startWave
                        }
                    }
                    case "FINISH" -> {
                        handleFinish();
                        return; // back to outer loop → waits for READY
                    }
                    default -> bridge.sendDialog("SYSTEM",
                            "Unknown command. Valid commands: PATCH | SCAN | BLOCK | ANALYZE | HELP | NEXT | FINISH");
                }
            }
        }
    }

    // ── Input routing ──────────────────────────────────────────────────────
    public void receiveInput(String input) {
        inputQueue.add(input);
    }

    public void shutdown() {
        running = false;
        inputQueue.add("__SHUTDOWN__");
    }

    // ── Game phases ────────────────────────────────────────────────────────

    private void playIntro() throws InterruptedException {
        bridge.sendDialog("SYSTEM", "Initializing secure connection...");
        Thread.sleep(200);
        bridge.sendDialog("DEFENDER", "Welcome, recruit. I am DEFENDER.");
        bridge.sendDialog("DEFENDER", "Our network is under attack. Hostile agents are probing our systems.");
        bridge.sendDialog("DEFENDER", "Your mission is to detect these threats and neutralize them before they cause irreversible damage.");
        bridge.sendDialog("DEFENDER", "You will learn to PATCH vulnerabilities, SCAN for intrusions, BLOCK compromised nodes, and ANALYZE enemy tactics.");
        bridge.sendDialog("DEFENDER", "Type READY to begin.");
    }

    private void startWave() throws InterruptedException {
        currentThreat = selectThreat();
        helpUsedThisWave = false;
        retryThisWave = false;

        MonitoringScoringAgent scoring = bridge.getScoringAgent();
        if (scoring != null) {
            scoring.startWave(currentThreat);
        }

        switch (currentThreat) {
            case "PHISHING" -> {
                bridge.sendDialog("DEFENDER", "ALERT. PHISHING AGENT detected on NODE-04.");
                bridge.sendDialog("DEFENDER", "It is sending spoofed emails to your users, attempting to steal credentials.");
            }
            case "BRUTEFORCE" -> {
                bridge.sendDialog("DEFENDER", "ALERT. BRUTE FORCE AGENT detected.");
                bridge.sendDialog("DEFENDER", "It is hammering our login systems, attempting to crack user passwords.");
            }
            case "MALWARE" -> {
                bridge.sendDialog("DEFENDER", "ALERT. MALWARE AGENT detected on the network.");
                bridge.sendDialog("DEFENDER", "It is spreading between nodes, compromising systems as it propagates.");
            }
        }

        bridge.sendDialog("DEFENDER",
                String.format("Threat confidence: %.1f%%", currentConfidence * 100));
        bridge.sendDialog("DEFENDER", "Available moves: PATCH | SCAN | BLOCK | ANALYZE");
        bridge.sendDialog("DEFENDER", "What is your move, recruit? (Type HELP if you need guidance)");
    }

    /**
     * Selects the next threat using ML model confidences as weights.
     * A small random jitter ensures variety across waves while keeping
     * selection data-driven (higher confidence → more likely to be chosen).
     * Falls back to pure random if no agent confidences are available.
     */
    private String selectThreat() {
        PhishingAttackAgent phishing = bridge.getPhishingAgent();
        BruteForceAgent bruteForce = bridge.getBruteForceAgent();
        MalwarePropagationAgent malware = bridge.getMalwareAgent();

        double pConf = phishing != null ? phishing.getConfidence() : 0.0;
        double bConf = bruteForce != null ? bruteForce.getConfidence() : 0.0;
        double mConf = malware != null ? malware.getConfidence() : 0.0;

        // Fall back to random if all confidences are zero (ML unavailable)
        if (pConf == 0.0 && bConf == 0.0 && mConf == 0.0) {
            currentConfidence = 0.0;
            return THREATS[RNG.nextInt(THREATS.length)];
        }

        // Add jitter so all three threats appear across waves;
        // higher-confidence threats remain more likely to be selected
        double pScore = pConf + RNG.nextDouble() * 0.15;
        double bScore = bConf + RNG.nextDouble() * 0.15;
        double mScore = mConf + RNG.nextDouble() * 0.15;

        if (pScore >= bScore && pScore >= mScore) {
            currentConfidence = pConf;
            return "PHISHING";
        } else if (bScore >= pScore && bScore >= mScore) {
            currentConfidence = bConf;
            return "BRUTEFORCE";
        } else {
            currentConfidence = mConf;
            return "MALWARE";
        }
    }

    private void handleHelp() throws InterruptedException {
        if (!helpUsedThisWave) {
            helpUsedThisWave = true;
            MonitoringScoringAgent scoring = bridge.getScoringAgent();
            if (scoring != null) {
                scoring.recordHelpUsed();
            }
        }

        DefenderAgent defender = bridge.getDefenderAgent();
        String rec = (defender != null) ? defender.recommendMove(currentThreat) : "BLOCK";

        bridge.sendDialog("DEFENDER", "Analyzing threat...");

        switch (currentThreat) {
            case "PHISHING" -> {
                bridge.sendDialog("DEFENDER", "PHISHING AGENT is exploiting user trust through deceptive emails.");
                bridge.sendDialog("DEFENDER", "Recommended move: BLOCK");
                bridge.sendDialog("DEFENDER", "Isolating the compromised node cuts off the attacker's foothold in the network.");
            }
            case "BRUTEFORCE" -> {
                bridge.sendDialog("DEFENDER", "BRUTE FORCE AGENT is exploiting unpatched vulnerabilities to crack credentials.");
                bridge.sendDialog("DEFENDER", "Recommended move: PATCH");
                bridge.sendDialog("DEFENDER", "Patching closes the entry point the agent is actively exploiting. No vulnerability, no attack.");
            }
            case "MALWARE" -> {
                bridge.sendDialog("DEFENDER", "MALWARE AGENT is spreading through active network traffic patterns.");
                bridge.sendDialog("DEFENDER", "Recommended move: SCAN");
                bridge.sendDialog("DEFENDER", "Anomaly detection catches the malware spreading between nodes before it propagates further.");
            }
        }

        bridge.sendDialog("DEFENDER", "What is your move, recruit?");
    }

    /**
     * @return true if the wave is now complete (SUPER_EFFECTIVE or NORMAL)
     */
    private boolean handleMove(String move) throws InterruptedException {
        DefenderAgent defender = bridge.getDefenderAgent();
        if (defender == null) {
            bridge.sendDialog("SYSTEM", "Defender system offline. Please restart.");
            return false;
        }

        String effectiveness = defender.evaluateMove(move, currentThreat, helpUsedThisWave, retryThisWave);

        if ("WEAK".equals(effectiveness)) {
            sendWeakDialog(move, currentThreat);
            bridge.sendDialog("DEFENDER", "Try again, recruit.");
            retryThisWave = true;
            return false;
        }

        // SUPER_EFFECTIVE or NORMAL — wave complete
        MonitoringScoringAgent scoring = bridge.getScoringAgent();
        int xpAwarded = 0;
        if (scoring != null) {
            xpAwarded = scoring.processOutcome(effectiveness, helpUsedThisWave, retryThisWave);
        }

        sendOutcomeDialog(effectiveness, move, currentThreat);
        bridge.sendDialog("DEFENDER", "+" + xpAwarded + " XP");

        // totalXP is updated in bridge.writeScoreState — read it back from scoring
        // We can re-read via scoring agent or track locally; scoring agent has it
        // Just re-query it from bridge's last known score (it was set in processOutcome)
        bridge.sendDialog("DEFENDER", "Type NEXT for the next threat or FINISH to end your session.");
        return true;
    }

    private void sendWeakDialog(String move, String threat) {
        switch (threat) {
            case "PHISHING" -> {
                if (move.equals("BLOCK")) {
                    // BLOCK is super-effective vs PHISHING — won't reach here
                } else if (move.equals("PATCH")) {
                    bridge.sendDialog("DEFENDER", "PATCH had little effect...");
                    bridge.sendDialog("DEFENDER", "Patching vulnerabilities won't stop a social engineering attack. Target the node directly.");
                } else if (move.equals("SCAN")) {
                    bridge.sendDialog("DEFENDER", "SCAN had little effect...");
                    bridge.sendDialog("DEFENDER", "PHISHING AGENT is targeting users directly through email. You need to isolate the compromised node.");
                } else if (move.equals("ANALYZE")) {
                    bridge.sendDialog("DEFENDER", "ANALYZE revealed useful intelligence but did not neutralize the threat.");
                    bridge.sendDialog("DEFENDER", "Save ANALYZE for coordinated insider attacks.");
                }
            }
            case "BRUTEFORCE" -> {
                if (move.equals("PATCH")) {
                    // PATCH is super-effective vs BRUTEFORCE — won't reach here
                } else if (move.equals("BLOCK")) {
                    bridge.sendDialog("DEFENDER", "BLOCK had little effect...");
                    bridge.sendDialog("DEFENDER", "BRUTE FORCE AGENT is targeting credentials, not network access. Blocking a node won't stop it.");
                } else if (move.equals("SCAN")) {
                    bridge.sendDialog("DEFENDER", "SCAN had little effect...");
                    bridge.sendDialog("DEFENDER", "BRUTE FORCE AGENT is using credential attacks, not network intrusion. Patch the vulnerability.");
                } else if (move.equals("ANALYZE")) {
                    bridge.sendDialog("DEFENDER", "ANALYZE revealed useful intelligence but did not neutralize the threat.");
                    bridge.sendDialog("DEFENDER", "Save ANALYZE for coordinated insider attacks.");
                }
            }
            case "MALWARE" -> {
                if (move.equals("SCAN")) {
                    // SCAN is super-effective vs MALWARE — won't reach here
                } else if (move.equals("PATCH")) {
                    bridge.sendDialog("DEFENDER", "PATCH had little effect...");
                    bridge.sendDialog("DEFENDER", "MALWARE AGENT is already spreading through active connections. Scan for it first.");
                } else if (move.equals("BLOCK")) {
                    bridge.sendDialog("DEFENDER", "BLOCK had some effect but was not optimal...");
                    bridge.sendDialog("DEFENDER", "Isolating helps but SCAN would catch the spread pattern earlier and more effectively.");
                } else if (move.equals("ANALYZE")) {
                    bridge.sendDialog("DEFENDER", "ANALYZE revealed useful intelligence but did not neutralize the threat.");
                    bridge.sendDialog("DEFENDER", "Save ANALYZE for coordinated insider attacks.");
                }
            }
        }
    }

    private void sendOutcomeDialog(String effectiveness, String move, String threat) {
        if ("SUPER_EFFECTIVE".equals(effectiveness)) {
            switch (threat) {
                case "PHISHING" -> {
                    bridge.sendDialog("DEFENDER", "BLOCK was super effective!");
                    bridge.sendDialog("DEFENDER", "Isolating the compromised node cut off PHISHING AGENT's foothold. Attack neutralized.");
                }
                case "BRUTEFORCE" -> {
                    bridge.sendDialog("DEFENDER", "PATCH was super effective!");
                    bridge.sendDialog("DEFENDER", "Closing the vulnerability removed BRUTE FORCE AGENT's entry point. No vulnerability, no attack.");
                }
                case "MALWARE" -> {
                    bridge.sendDialog("DEFENDER", "SCAN was super effective!");
                    bridge.sendDialog("DEFENDER", "Anomaly detection caught MALWARE AGENT spreading between nodes. Infection contained.");
                }
            }
        } else {
            bridge.sendDialog("DEFENDER", move + " was effective. Threat level reduced.");
        }
    }

    private String waitForNextOrFinish() throws InterruptedException {
        while (running) {
            String input = waitForInput();
            if ("NEXT".equals(input) || "FINISH".equals(input)) return input;
            bridge.sendDialog("SYSTEM", "Type NEXT for the next threat or FINISH to end your session.");
        }
        return "FINISH";
    }

    private void handleFinish() {
        bridge.sendDialog("DEFENDER", "Session complete. Compiling your results...");

        MonitoringScoringAgent scoring = bridge.getScoringAgent();
        if (scoring != null) {
            GameSummary summary = scoring.endSession();
            bridge.sendGameOver(summary);
        }
    }

    private String waitForInput() throws InterruptedException {
        while (running) {
            String input = inputQueue.take();
            if ("__SHUTDOWN__".equals(input)) {
                running = false;
                throw new InterruptedException("Shutdown requested");
            }
            return input;
        }
        throw new InterruptedException("Controller stopped");
    }
}
