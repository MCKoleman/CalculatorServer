import java.util.*;
import java.io.*;

public class linkstate {
    graph graph;

    private class graph {
        public ArrayList<node> nodes = new ArrayList<node>();

        // Prints the graph as a list of nodes
        public void print() {
            for (int i = 0; i < nodes.size(); i++) {
                node temp = nodes.get(i);
                if (temp == null) {
                    continue;
                }
                for (Map.Entry<Integer, Integer> entry : temp.getEdges().entrySet()) {
                    System.out.println(i + " " + entry.getKey() + " " + entry.getValue());
                }
            }
        }

        // Adds a new node or edge to the graph
        public void add(int startKey, int endKey, int cost) {
            // Make sure that each node is placed at the right index
            while (nodes.size() <= startKey) {
                nodes.add(null);
            }

            // If the node already exists, add another edge
            if (nodes.get(startKey) != null) {
                nodes.get(startKey).addEdge(endKey, cost);
            }
            // If the node doesn't exist, add it
            else {
                nodes.set(startKey, new node(startKey, endKey, cost));
            }
        }
    }
    
    private class node {
        private int key = 0;
        private HashMap<Integer, Integer> edges = new HashMap<>();

        // Inits the node with one edge
        public node(int key, int other, int cost) {
            this.key = key;
            addEdge(other, cost);
        }

        // Adds a new unique edge
        public void addEdge(int other, int cost) {
            if(!edges.containsKey(other)) {
                edges.put(other, cost);
            } else {
                System.out.println("#[NODE] Did not add edge " + other + " " + cost + " to node " + key + ": already exists.");
            }
        }

        public HashMap<Integer, Integer> getEdges() { return edges; }
    }

    public linkstate(String filename) {
        graph = new graph();

        // Read graph from file
        File file = new File(filename);
        BufferedReader reader;
        String inputLine;
        try {
            // Read each line as a node
            reader = new BufferedReader(new FileReader(file));
            while ((inputLine = reader.readLine()) != null) {
                String[] line = inputLine.split(" ");
                if (line.length < 3) {
                    break;
                }
                graph.add(Integer.parseInt(line[0]), Integer.parseInt(line[1]), Integer.parseInt(line[2]));
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Sender could not read file: " + e.toString());
            return;
        }
    }

    // Prints the contents of the graph
    public void print() {
        System.out.println("# Printing graph...");
        graph.print();
    }

    public static void main(String[] args) {
        // Only start program if a filename is given
        if (args.length < 1) {
            System.out.println("Could not start link-state router: No filename given.");
            return;
        }
        String filename = args[0];
        File file = new File(filename);
        if (!file.isFile()) {
            System.out.println("Could not start link-state router: Could not find file at " + filename);
            return;
        }

        linkstate network = new linkstate(filename);
        network.print();
    }
}