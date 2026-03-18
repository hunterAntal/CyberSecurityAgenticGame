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
 * PhishingAttackAgent — simulates a phishing attack on the network.
 * Registers with the JADE DF under service type "phishing-attack".
 * Runs the phishing ML model at startup and stores the attack confidence.
 */
public class PhishingAttackAgent extends Agent {

    private double confidence = 0.0;

    @Override
    protected void setup() {
        System.out.println("[PhishingAttackAgent] Starting up: " + getAID().getName());

        // Run ML model to obtain attack confidence
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "backend/predict_phishing.py");
            pb.redirectErrorStream(false);
            Process proc = pb.start();
            String line = new BufferedReader(new InputStreamReader(proc.getInputStream())).readLine();
            if (line != null && !line.isBlank()) {
                confidence = Double.parseDouble(line.trim());
            }
            proc.waitFor();
        } catch (Exception e) {
            System.err.println("[PhishingAttackAgent] ML inference failed: " + e.getMessage());
        }
        System.out.println("[PhishingAttackAgent] Confidence: " + confidence);

        // Register with JADE DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("phishing-attack");
        sd.setName("PhishingAttackAgent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("[PhishingAttackAgent] DF registration failed: " + e.getMessage());
        }

        // Register with bridge so GameFlowController can query confidence
        GameStateBridge bridge = GameStateBridge.getInstance();
        if (bridge != null) {
            bridge.registerPhishing(this);
        }
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }
}
