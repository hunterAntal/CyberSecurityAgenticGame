package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 * BruteForceAgent — simulates a brute-force credential attack.
 * Registers with the JADE DF under service type "bruteforce-attack".
 */
public class BruteForceAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("[BruteForceAgent] Starting up: " + getAID().getName());

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
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }
}
