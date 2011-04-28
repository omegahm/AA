import json
import pulp

with open('CopenhagenGraph.json') as f:
    graph = json.load(f)



#prob = LpProblem("carsInCopenhagen", LpMaximize)