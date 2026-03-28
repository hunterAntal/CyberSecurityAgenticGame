import bridge.GameStateBridge;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

/**
 * Entry point — starts the WebSocket bridge, then the JADE container with all agents.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("[Main] Starting Cyber Training Game...");

        // 1. Start WebSocket bridge first so agents can register with it on startup
        GameStateBridge bridge = new GameStateBridge(8887);
        bridge.start();

        // 2. Start JADE runtime
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "false");

        AgentContainer container = rt.createMainContainer(profile);

        // 3. Start attack agents (teammate's agents)
        AgentController phishing = container.createNewAgent(
                "phishing", "agents.PhishingAttackAgent", null);
        phishing.start();

        AgentController bruteforce = container.createNewAgent(
                "bruteforce", "agents.BruteForceAgent", null);
        bruteforce.start();

        AgentController malware = container.createNewAgent(
                "malware", "agents.MalwarePropagationAgent", null);
        malware.start();

        // 4. Start our agents
        AgentController defender = container.createNewAgent(
                "defender", "agents.DefenderAgent", null);
        defender.start();

        AgentController scoring = container.createNewAgent(
                "monitoring", "agents.MonitoringScoringAgent", null);
        scoring.start();

        System.out.println("[Main] All agents started. Open frontend/index.html to play.");
        System.out.println("[Main] WebSocket listening on ws://localhost:8887");

        // Cleanly release the WebSocket port on Ctrl-C / SIGTERM
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutting down...");
            try { bridge.stop(1000); } catch (Exception e) { /* ignore */ }
        }, "shutdown-hook"));
    }
}
