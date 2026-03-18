package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 * PhishingAttackAgent — simulates a phishing attack on the network.
 * Registers with the JADE DF under service type "phishing-attack".
 */
public class PhishingAttackAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("[PhishingAttackAgent] Starting up: " + getAID().getName());

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
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {}
    }
}
