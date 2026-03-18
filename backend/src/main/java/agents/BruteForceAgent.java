package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import bridge.GameStateBridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * BruteForceAgent — simulates a brute-force credential attack.
 * Registers with the JADE DF under service type "bruteforce-attack".
 * Runs the brute-force ML model at startup and stores 5 calibrated confidences.
 */
public class BruteForceAgent extends Agent {

    private double[] confidences = new double[5];

    @Override
    protected void setup() {
        System.out.println("[BruteForceAgent] Starting up: " + getAID().getName());

        // Run ML model to obtain 5 calibrated attack confidence values
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "backend/predict_bruteforce.py");
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            for (int i = 0; i < confidences.length; i++) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    confidences[i] = Double.parseDouble(line.trim());
                }
            }
            proc.waitFor();
        } catch (Exception e) {
            System.err.println("[BruteForceAgent] ML inference failed: " + e.getMessage());
        }
        System.out.print("[BruteForceAgent] Confidences:");
        for (double c : confidences) System.out.printf(" %.4f", c);
        System.out.println();

        // Register with JADE DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bruteforce-attack");
        sd.setName("BruteForceAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("[BruteForceAgent] DF registration failed: " + e.getMessage());
        }

        // Register with bridge so GameFlowController can query confidences
        GameStateBridge bridge = GameStateBridge.getInstance();
        if (bridge != null) {
            bridge.registerBruteForce(this);
        }
    }

    public double[] getConfidences() {
        return confidences;
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }
}
