import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import lpsolve.LpSolve;

/**
 * A branch-and-bound solver for the Traveling Couple Problem. A problem is
 * represented as an instance of the TCPProblem and an optimal TCP-tour is found
 * and printed in the constructor of BnB. The optimal TCP-tour can be retrieved
 * with the getOptimalTour-method.
 * 
 * The BnB method constructs a BnB-tree (represented by BnBNode's).
 * 
 * @author R.Fonseca
 */
public class BnB {
	private BnBNode optimal;
	private final TCPProblem problem;

	/** Constructs a solution to the TCPProblem */
	public BnB(TCPProblem problem) {
		this.problem = problem;

		// Create a node-pool that holds set of BnB-nodes sorted by lower bound.
		// Each node corresponds
		// to a path in the problem-graph.
		TreeSet<BnBNode> nodePool = new TreeSet<BnBNode>(
				new Comparator<BnBNode>() {
					public int compare(BnBNode n1, BnBNode n2) {
						int comp = Double.compare(n1.lowerBound, n2.lowerBound);
						if (comp == 0)
							return n1.hashCode()-n2.hashCode();
						else
							return comp;
					}
				});
		double upperbound = Double.POSITIVE_INFINITY;

		long totalNodes = 0;

		// Initialize the node-pool
		List<BnBNode> initialNodes = generateInitialNodes(0);
		totalNodes += initialNodes.size();
		nodePool.addAll(initialNodes);

		while (!nodePool.isEmpty()) {
			BnBNode n = nodePool.pollFirst(); // Remove the most promising path
												// from the node-pool

			// If the lower bound is higher (or equal) than the upper
			// bound the path can be ignored (this is the bounding part)
			if (n.lowerBound < upperbound) {
				// A path is complete if it has visited a vertex in the vicinity
				// of all vertices
				if (isComplete(n)) {
					// If the path is complete and better than the upper bound
					// it might be optimal
					optimal = n;
					upperbound = n.lowerBound;
				} else {
					// Branching
					List<BnBNode> children = branch(n);
					totalNodes += children.size();
					nodePool.addAll(children);
				}
			}
		}
		System.out.printf("Optimal tour length %.4f. Evaluated %d nodes\n",
				optimal.lowerBound, totalNodes);
	}

	/**
	 * Generate a set of initial nodes with no parents. For TSP it would be
	 * enough to generate one node for each edge out of the specified vertex.
	 * For TCP, however, it is necessary to generate nodes for all edges leaving
	 * nodes in the vicinity of the specified vertex.
	 */
	private List<BnBNode> generateInitialNodes(int vertex) {
		List<BnBNode> ret = new LinkedList<BnBNode>();
		for (int v : problem.getVicinity(vertex)) {
			for (Edge e : problem.edges[v]) {
				BnBNode n = new BnBNode(e);
				n.lowerBound = lowerbound(n);
				ret.add(n);
			}
		}
		return ret;
	}

	/**
	 * Return a list of children of n. In other words, n corresponds to a path
	 * and this method returns all edges leaving the last visited vertex and who
	 * does not revisit the path (no point in visiting a vertex twice).
	 */
	private List<BnBNode> branch(BnBNode n) {
		List<BnBNode> ret = new LinkedList<BnBNode>();
		// Go through all edges leaving n.edge.verts[1]
		for (Edge e : problem.edges[n.edge.v1]) {
			// Determine if the end of this edge has been visited previously
			boolean endInPath = false;
			BnBNode p = n;
			while (p != null) {
				if (p.edge.v0 == e.v1) {
					endInPath = true;
					break;
				}
				p = p.parent;
			}

			// If not, create a new node from it
			if (!endInPath) {
				BnBNode newNode = new BnBNode(n, e);
				newNode.lowerBound = lowerbound(newNode);
				ret.add(newNode);
			}
		}
		return ret;
	}

	/**
	 * Determine if n corresponds to a path that visits enough monuments such
	 * that all monuments have been seen.
	 */
	private boolean isComplete(BnBNode n) {
		// Determine which sites have been seen
		boolean[] seen = new boolean[problem.n];
		BnBNode p = n;
		for (int v : problem.getVicinity(p.edge.v1))
			seen[v] = true;
		while (true) {
			for (int v : problem.getVicinity(p.edge.v0))
				seen[v] = true;
			if (p.parent == null)
				break;
			p = p.parent;
		}
		// n is only complete if all vertices are seen
		for (int i = 0; i < problem.n; i++)
			if (!seen[i])
				return false;

		// n is only complete if there exists an edge from the last vertex to
		// the first
		boolean finalEdgeExists = false;
		int lastVertex = n.edge.v1;
		int firstVertex = p.edge.v0;
		for (Edge e : problem.edges[lastVertex])
			if (e.v1 == firstVertex) {
				finalEdgeExists = true;
				break;
			}
		if (!finalEdgeExists)
			return false;

		return true;
	}

	/** Returns a lower bound for the path corresponding to n. */
	private double lowerbound(BnBNode bnbn) {
		try {
			int n = problem.vertices.length;
			int numOfVar = n + (n*(n-1))/2;
			
			LpSolve solver = LpSolve.makeLp(0, numOfVar);
			
			String value = "";
			for(int i = 0; i < n; i++) {
				value += "0 ";
			}
			
			for(int i = 0; i < problem.edges.length; i++) {
				for(int j = i+1; j <= problem.edges[i].length; j++) {
					value += problem.edges[i][j-1].length + " ";
				}
			}
		
			solver.strSetObjFn(value.trim());
			
			for(int i = 0; i < n; i++) {
				value = "";
				int vics[] = problem.vicinities[i];
				for(int j = 0; j < n; j++) {
					boolean yes = false;
					for(int v : vics) {
						yes = yes || v == j;
					}
					
					if(yes) {
						value += "1 ";
					} else {
						value += "0 ";
					}	
				}
				
				for(int a = 0; a < (n*(n-1))/2; a++) {
					value += "0 ";
				}
				
				solver.strAddConstraint(value.trim(), LpSolve.GE, 1.0);
			}
			
			int hack[][] = new int[n+1][n+1];
			int k = n+1;
			
			for(int i = 1; i <= n - 1; i++) {
				for(int j = i+1; j <= n; j++) {
					hack[i][j] = k;
					hack[j][i] = k;
					k++;
				}
			}
					
			double data[] = new double[numOfVar];
			for(int i = 1; i <= n; i++) {
				Arrays.fill(data, 0.0);
				value = "";
				for(int j = 0; j < n; j++) {
					data[j] = 0.0;
				}
				
				for(int j = 1; j <= n; j++) {
					if(i != j) {
						data[hack[i][j]-1] = 1;
						data[hack[j][i]-1] = 1;
					}
				}
				
				data[i-1] = -2.0;
				
				for(int j = 0; j < data.length; j++) {
					value += "" + data[j] + " ";
				}
				
				solver.strAddConstraint(value.trim(), LpSolve.EQ, 0.0);
			}
			
			BnBNode tmp = bnbn;
			while(tmp != null) {
				value = "";
				Arrays.fill(data, 0.0);
				data[tmp.edge.v0] = 1.0;
				for(int j = 0; j < data.length; j++) {
					value += "" + data[j] + " ";
				}
				solver.strAddConstraint(value.trim(), LpSolve.GE, 1.0);
				
				value = "";
				Arrays.fill(data, 0.0);
				data[tmp.edge.v1] = 1.0;
				for(int j = 0; j < data.length; j++) {
					value += "" + data[j] + " ";
				}
				solver.strAddConstraint(value.trim(), LpSolve.GE, 1.0);
				
				value = "";
				Arrays.fill(data, 0.0);
				data[hack[tmp.edge.v0][tmp.edge.v1]] = 1.0;
				for(int j = 0; j < data.length; j++) {
					value += "" + data[j] + " ";
				}
				solver.strAddConstraint(value.trim(), LpSolve.GE, 1.0);
				
				tmp = tmp.parent;
			}
			
			solver.setMinim();
			solver.writeLp("here.lp");
			solver.setVerbose(LpSolve.CRASH_NOTHING);
			
			/*for(int i = 1; i <= numOfVar; i++) {
				solver.setUpbo(i, 1.0);
				solver.setLowbo(i, 0.0);
			}*/
			
			if(LpSolve.OPTIMAL == solver.solve()) {
				//System.out.println("OPTIMAL");
			}
			
			// print the solution
			if(false) {
				System.out.println("Value of objective function: " + solver.getObjective());
				double[] var = solver.getPtrVariables();
				for (int i = 0; i < var.length; i++) {
					System.out.println("Value of var[" + (i+1) + "] = " + var[i]);
				}
				System.out.println();
			}
		
			return solver.getObjective();
		} catch(Exception e) {
			System.out.println(e);
			return 0.0;
		}
	}
	int globali = 0;

	/** Return an array with all the vertices visited in the specified path */
	private int[] getVerticesInNode(BnBNode n) {
		BnBNode p = n;
		int c = 1;
		while (p != null) {
			c++;// Just for counting
			p = p.parent;
		}
		int[] ret = new int[c];

		c = 0;
		p = n;
		ret[c++] = p.edge.v1;
		while (p != null) {
			ret[c++] = p.edge.v0;
			p = p.parent;
		}
		return ret;
	}

	/** Return an array with the vertices in the optimal tour. */
	public int[] getOptimalSolution() {
		return getVerticesInNode(optimal);
	}

	public static void main(String[] args) {
		TCPProblem problem = new Graph1();
		//System.out.println(problem);
		BnB solver = new BnB(problem);
		int[] optTour = solver.getOptimalSolution();
		System.out.println(Arrays.toString(optTour));
	}

}
