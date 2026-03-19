package agents;

import bridge.GameStateBridge;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 * DefenderAgent — JADE agent that evaluates and recommends defensive moves.
 *
 * Utility function: U(action) = E(threat_reduction) - C(action) - P(false_positive)
 */
public class DefenderAgent extends Agent {

    // ── Move sets ─────────────────────────────────────────────────────────
    private static final String[] MOVES = {"PATCH", "SCAN", "BLOCK"};

    // ── E(threat_reduction) table ─────────────────────────────────────────
    private double expectedReduction(String move, String threat) {
        return switch (threat.toUpperCase()) {
            case "PHISHING"   -> move.equals("BLOCK") ? 1.0 : 0.1;
            case "BRUTEFORCE" -> move.equals("PATCH") ? 1.0 : 0.1;
            case "MALWARE"    -> move.equals("SCAN")  ? 1.0 : 0.1;
            default -> 0.1;
        };
    }

    // ── C(action) table ───────────────────────────────────────────────────
    private double cost(String move) {
        return switch (move) {
            case "PATCH"   -> 0.30;
            case "SCAN"    -> 0.10;
            case "BLOCK"   -> 0.20;
            default -> 1.0;
        };
    }

    // ── P(false_positive) table ───────────────────────────────────────────
    private double falsePositiveRisk(String move) {
        return switch (move) {
            case "PATCH"   -> 0.05;
            case "SCAN"    -> 0.10;
            case "BLOCK"   -> 0.20;
            default -> 1.0;
        };
    }

    // ── U(action) ─────────────────────────────────────────────────────────
    private double utility(String move, String threat) {
        return expectedReduction(move, threat) - cost(move) - falsePositiveRisk(move);
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Returns the move with the highest utility score for the given threat.
     * This is what DEFENDER returns when the player types HELP.
     */
    public String recommendMove(String threatType) {
        String best = MOVES[0];
        double bestScore = utility(MOVES[0], threatType);
        for (int i = 1; i < MOVES.length; i++) {
            double score = utility(MOVES[i], threatType);
            if (score > bestScore) {
                bestScore = score;
                best = MOVES[i];
            }
        }
        return best;
    }

    /**
     * Evaluates effectiveness of a move against the given threat.
     * Returns: "SUPER_EFFECTIVE", "NORMAL", or "WEAK"
     *
     * Also writes the outcome to GameStateBridge.
     */
    public String evaluateMove(String move, String threatType,
                                boolean helpUsed, boolean isRetry) {
        String result = computeEffectiveness(move, threatType);

        // Write to bridge (shared state)
        GameStateBridge bridge = GameStateBridge.getInstance();
        if (bridge != null) {
            bridge.writeDefenderOutcome(result, move, threatType, helpUsed, isRetry);
        }

        return result;
    }

    private String computeEffectiveness(String move, String threat) {
        boolean superEffective = switch (threat.toUpperCase()) {
            case "PHISHING"   -> move.equals("BLOCK");
            case "BRUTEFORCE" -> move.equals("PATCH");
            case "MALWARE"    -> move.equals("SCAN");
            default -> false;
        };
        if (superEffective) return "SUPER_EFFECTIVE";

        // Remaining moves that are not super-effective:
        // they do reduce the threat somewhat → NORMAL
        // except the explicitly weak pairings listed in the dialog templates
        boolean isWeakPairing = switch (threat.toUpperCase()) {
            case "PHISHING"   -> move.equals("PATCH") || move.equals("SCAN");
            case "BRUTEFORCE" -> move.equals("BLOCK") || move.equals("SCAN");
            case "MALWARE"    -> move.equals("PATCH");
            default -> false;
        };

        // BLOCK vs MALWARE is "had some effect but not optimal" → we treat as WEAK
        // (it forces retry with educational feedback per the dialog spec)
        if (threat.equalsIgnoreCase("MALWARE") && move.equals("BLOCK")) return "WEAK";

        return isWeakPairing ? "WEAK" : "NORMAL";
    }

    // ── JADE lifecycle ────────────────────────────────────────────────────
    @Override
    protected void setup() {
        System.out.println("[DefenderAgent] Starting up: " + getAID().getName());

        // Register with bridge
        GameStateBridge bridge = GameStateBridge.getInstance();
        if (bridge != null) {
            bridge.registerDefender(this);
        }

        // Register with JADE Directory Facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("defender");
        sd.setName("DefenderAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("[DefenderAgent] Registered with DF as service type 'defender'");
        } catch (FIPAException e) {
            System.err.println("[DefenderAgent] DF registration failed: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
        System.out.println("[DefenderAgent] Shutting down.");
    }
}
