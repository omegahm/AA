import json
from pulp import *

# Define the number of last source 
LAST_SOURCE = 7

# Define the number of first sink
FIRST_SINK = 26

# Lets define the LP-problem
prob = LpProblem("carsInCopenhagen", LpMaximize)

# We load the problem graph
with open('CopenhagenGraph.json') as f:
    graph = json.load(f)

# We need to have all the ingoing and all the outgoing edges
out_edges = [[] for i in range(30)]
in_edges = [[] for i in range(30)]

# For each e in E | 0 < x_e <= c_e
for e in graph['edgelist']:
    tmp = LpVariable('o(' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])
    
    out_edges[e['u']].append(tmp)
    in_edges[e['v']].append(tmp)

    
    tmp = LpVariable('i(' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])
    out_edges[e['v']].append(tmp)
    in_edges[e['u']].append(tmp)

# Objective function
prob += lpSum(out_edges[0:6]), "Maximum number of cars"    

# For each v in V \ {s,t} | Sum_{e=(u,v) \in E}(x_e) = Sum_{e=(v,w) \in E}(x_e)
for n in range(LAST_SOURCE, FIRST_SINK):
    prob += lpSum(in_edges[n]) == lpSum(out_edges[n]), "Input needs to equal output " + str(n)
        
prob.writeLP("CopenhagenGraph.lp")
#print prob
status = prob.solve(GLPK(msg = 0))

print "Status:", LpStatus[prob.status]

for variable in prob.variables():
    print variable.name, "=", variable.varValue
