import json
from pulp import *

INF = 10^10

prob = LpProblem("carsInCopenhagen", LpMaximize)

with open('CopenhagenGraph.json') as f:
    graph = json.load(f)

variable_array = []
#variable_array.append(LpVariable('s', 0, INF))
objective_value = 0
edges = [None]*30

for e in graph['edgelist']:
    if e['u'] <= 6 or e['v'] <= 6:
        objective_value += e['weight']

    tmp = LpVariable('(' + str(e['u']) + ',' + str(e['v']) + ')', 0, e['weight'])
    variable_array.append(tmp)

    if edges[e['u']] == None:
        edges[e['u']] = []
    edges[e['u']].append(tmp)

    tmp = LpVariable('(' + str(e['v']) + ',' + str(e['u']) + ')', 0, e['weight'])
    variable_array.append(tmp)
    
    if edges[e['v']] == None:
        edges[e['v']] = []
    edges[e['v']].append(tmp)
     
#for x in edges:
#    prob += lpSum(x) <= 0
#    prob += lpSum(x) >= lpSum(x)


prob += lpSum(variable_array[0:25])
print edges
    
#print variable_array


status = prob.solve(GLPK(msg = 0))