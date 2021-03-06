import json
from pulp import *

# Lets define the LP-problem
prob = LpProblem("carsInCopenhagen", LpMaximize)

# We load the problem graph
with open('CopenhagenGraph.json') as f:
    graph = json.load(f)

SOURCES = [0,1,2,3,4,5,6] # The sources
SINKS   = [26,27,28,29] # The sinks
INF     = 10**6 # Infinite

constrains = { 0: 150,
               1: 150,
               2: 150,
               3: 150,
               4: 150,
               5:  30,
               6: 150,
               7: 150,
               8: 150,
               9: 150,
              10: 100,
              11: 100,
              12: 100,
              13: 100,
              14: 100,
              15: 100,
              16: 100,
              17:  30,
              18: 150,
              19:  30,
              20:  30,
              21:  30,
              22:  30,
              23:  30,
              24:  30,
              25:  30,
              26: INF,
              27: INF,
              28: INF,
              29: INF }

extra = SINKS[-1]+1

# We need to have all the ingoing and all the outgoing edges
out_edges = {}
in_edges = {}

# Initialize arrays
for e in graph['edgelist']:
    out_edges[e['u']] = []
    out_edges[e['v']] = []
    in_edges[e['u']]  = []
    in_edges[e['v']]  = []
    out_edges[extra]  = []
    in_edges[extra]   = []
    constrains[extra] = 0
    extra += 1
    
extra = SINKS[-1]+1

out_edges['supersource'] = []
in_edges['supersink']    = []
s = []
t = []

# For each e in E | 0 < x_e <= c_e
for e in graph['edgelist']:
    tmp = LpVariable('original (' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])
    
    out_edges[e['u']].append(tmp)
    in_edges[e['v']].append(tmp)
    
    if e['u'] in SOURCES and e['u'] not in s:
        tmp = LpVariable('supersource (s,' + str(e['u']) + ')', 0, INF)
        in_edges[e['u']].append(tmp)
        out_edges['supersource'].append(tmp)
        s.append(e['u'])
        
    if e['v'] in SINKS and e['v'] not in t:
        tmp = LpVariable('supersink (' + str(e['v']) + ',t)', 0, INF)
        out_edges[e['v']].append(tmp)
        in_edges['supersink'].append(tmp)
        t.append(e['v'])
    
    tmp = LpVariable('extra1 (' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])

    out_edges[e['v']].append(tmp)
    in_edges[extra].append(tmp)

    tmp = LpVariable('extra2 (' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])

    out_edges[extra].append(tmp)
    in_edges[e['u']].append(tmp)
    
    if e['v'] in SOURCES and extra not in s:
        tmp = LpVariable('supersource (s,' + str(extra) + ')', 0, INF)
        in_edges[extra].append(tmp)
        out_edges['supersource'].append(tmp)
        s.append(extra)
    
    if e['u'] in SINKS and e['u'] not in t:
        tmp = LpVariable('supersink (' + str(extra) + ',t)', 0, INF)
        out_edges[extra].append(tmp)
        in_edges['supersink'].append(tmp)
        t.append(extra)

    constrains[extra] = INF
    extra += 1

# Objective function
prob += lpSum(out_edges['supersource']), "Maximum number of cars"    

# For each v in V \ {s,t} | Sum_{e=(u,v) \in E}(x_e) = Sum_{e=(v,w) \in E}(x_e)
for n in range(0, extra):
    prob += lpSum(in_edges[n]) == lpSum(out_edges[n]), "Input needs to equal output " + str(n)

# For each v in V | Sum_{e=(u,v)} x_e <= c_v
for n in range(0, extra):
    prob += lpSum(in_edges[n]) <= constrains[n]

prob.writeLP("CopenhagenGraph.lp")
#print prob
status = prob.solve(GLPK(msg = 0))

print "Status:", LpStatus[prob.status]

for variable in prob.variables():
    if(variable.varValue != 0.0):
        print variable.name, "=", variable.varValue
    
print "Maximum number of cars to enter the city center", value(prob.objective)
