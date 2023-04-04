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

        // Returns the cost from node u to node v
        public int getCost(int u, int v) {
            return nodes.get(u).getCost(v);
        }

        public int size() { return nodes.size(); }
        public node get(int index) { return nodes.get(index); }
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

        // Returns the cost between this node and the given node
        public int getCost(int other) {
            // If the nodes are not neighbors, return -1
            return (edges.get(other) != null) ? edges.get(other) : -1;
        }

        // Returns whether the given node is a neighbor of this node
        public Boolean isNeighbor(int other) {
            return edges.containsKey(other);
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

    // Performs dijkstra's algorithm starting at the given node
    public void dijkstra(int u) {
        // Initialization
        int step = 0;
        int[] dist = new int[graph.size()];
        int[] p = new int[graph.size()];
        HashSet<Integer> N = new HashSet<>();
        
        // for all nodes v
        for (int i = 0; i < graph.size(); i++) {
            // if v is a neighbor of u
            if (graph.getCost(u, i) >= 0) {
                // then D(v) = c(u,v)
                dist[i] = graph.getCost(u, i);
                p[i] = u;
            } else {
                // else D(v) = inf
                dist[i] = Integer.MAX_VALUE;
            }
        }

        // Loop until N' = N
        while (N.size() < graph.size()-1) {
            // find w not in N’ such that D(w) is a minimum
            int minDist = Integer.MAX_VALUE;
            int w = 0;
            for (int i = 1; i < dist.length; i++) {
                if (!N.contains(i) && dist[i] <= minDist) {
                    w = i;
                    minDist = dist[i];
                }
            }

            // If w is not updated in the loop, the algorithm is finished
            if (w <= 0) {
                System.out.println("# Dijkstra encountered an issue: Not all nodes were reached");
                break;
            }

            // add w to N’
            N.add(w);
            // update D(v) for each neighbor v of w and not in N’:
            for (Map.Entry<Integer, Integer> entry : graph.get(w).getEdges().entrySet()) {
                if(!N.contains(entry.getKey())) {
                    // D(v) = min(D(v), D(w)+ c(w,v) )
                    int v = entry.getKey();
                    int Dw = dist[w] + graph.getCost(w, v);
                    if (Dw < dist[v]) {
                        dist[v] = Dw;
                        p[v] = w;
                    }
                }
            }

            // Print step n
            System.out.print(step);
            for (int i = 1; i < dist.length; i++) {
                System.out.print("," + distToString(dist[i]) + "," + indexToString(p[i]));
            }
            System.out.println();
            step++;
        }
    }

    // Prints the contents of the graph
    public void print() {
        System.out.print("Step");
        for (int i = 1; i < graph.size(); i++) {
            System.out.print(",D" + i + ",P" + i);
        }
        System.out.println();
        dijkstra(1);
    }

    // Returns the given distance as a string
    public String indexToString(int dist) {
        return (dist > 0) ? String.valueOf(dist) : "-";
    }

    // Returns the given distance as a string
    public String distToString(int dist) {
        return (dist != Integer.MAX_VALUE) ? String.valueOf(dist) : "-";
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