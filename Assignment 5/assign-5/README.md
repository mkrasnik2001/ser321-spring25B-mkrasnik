# Assignment 5 - Michael Krasnik
## Commands to run the program
1. `cd` into `assign-5/`
2. Start the leader by running `gradle runLeader --console=plain -q` - this will have the client port defaulted to 9000 and the node port defaulted to 8000. Alternatively `gradle runLeader -PclientPort=9000 -PnodePort=8000` to provide other ports.
3. Start the client by running `gradle runClient --console=plain -q` - similar to the above, but defaults to `-Pleaderhost=localhost` and `-PclientPort=9000`.
4. Start a node by running `gradle runNode --console=plain -q` - similar to the above, but defaults to `-PnodePort=8000` and `-Pleaderhost=localhost`.
To simulate a faulty node, pass in `-PFault=true` in the arguments like this: `gradle runNode -PFault=true --console=plain-q`.
5. Pass your arguments from the client.

# Program explanation and workflow
This is an example of a simple distributed system where we have a client, a leader and nodes. the client sends a request sum a list of numbers with a delay for each sum to the leader, the leader distrubutes
this workload to its nodes, the leader then sends each nodes calcution to the next node next for validation. if all nodes come to a consensus the final result is computed and sent back to client from the leader. if at least one node disagress or votes no to a result, then an error response is sent to the client meaning not all the nodes came to a consensus. The leader also calculates the list without distribution in a seperate thread to compare computation time. The client receives both the result and total computation time (distributed and not distibuted)

## Protocol

### Header Breakdown

| Header      | Message Type | Direction                                       |
|-------------|--------------|-------------------------------------------------|
| `TASK`      | Request      | Client -> Leader<br>Leader -> Node              |
| `CONSENSUS` | Request      | Leader -> Node                                  |
| `RESPONSE`  | Response     | Node -> Leader                                  |
| `VOTE`      | Response     | Node -> Leader                                  |
| `RESULT`    | Response     | Leader -> Client                                |
| `ERROR`     | Response     | Leader -> Client                                |


*Client -> Leader Request Example:*
Submits the list of integers to sum and the delay in ms from the client to the leader
```json
{
  "header": "TASK",
  "payload": {
    "slice": [1, 2, 3, 4, 5], <array of ints>
    "delay": 100 <int>
  }
}
```
---
_*Leader -> Node Request Example:*_
The leader sends this request to the nodes with the partial slice of the original list for that node to compute with the given delay
```json
{
  "header": "TASK",
  "payload": {
    "intSlice": [1, 2], <array of ints>
    "delay": 100 <int>
  }
}
```
---
_*Node TASK Response Example:*_
```json
The response of the node and the result of that node summing the list that was given to it. This goes back to the leader
{
  "header": "RESPONSE",
  "payload": {
    "nodeResSum": 3
  }
}
```
---
_*Leader -> Node Consensus Request:*_
This request is sent out by the leader to a node with the reported sum value of a different node to check weather or not that node agrees with the reported sum
```json
{
  "header": "CONSENSUS",
  "payload": {
    "intSlice": [3, 4], <array of ints>
    "reportedSum": 7 <int>
  }
}
```
---
_*Node -> Leader CONSENSUS Response Example:*_
This is the response of the node back to the leader that votes weather or not they agree with the reportedSum provided by the leader of a different node.
```json
{
  "header": "VOTE",
  "payload": {
    "nodeCheckRes": true <boolean>
  }
}
```
---

_*Leader -> Client success result Response Example:*_
This is the final result that is sent from the leader back to the client with the distributed sum, the single threaded sum which was done for analysis and just by the leader
as well as the computation times it took to complete both distributed and single threaded
```json
{
  "header": "RESULT",
  "payload": {
    "distSum": 15, <int>
    "singleSum": 15, <int>
    "singleThreadResTime": 500, <int>
    "distribThreadResTime": 200 <int>
  }
}
```
---

_*Leader -> Client Error Responses*_
Not enough nodes connected to the leader to begin computation
```json
{
  "header": "ERROR",
  "payload": {
    "error": "Need at least 3 nodes, but only 1 connected"
  }
}

Consensus failed and did not pass
``` json
{
  "header": "ERROR",
  "payload": {
    "error": "Nodes did not come to a consensus"
  }
}
```



# Requirements List
- [X] Client accepts a list of numbers and a delay in ms 
- [X] Leader distributes portions of the list to nodes for calculation
- [X] Nodes perform calculation with the delay simulation comp time
- [X] The leader can be started via a gradle task
- [X] The client can be started via a gradle task
- [X] The nodes can be started via a gradle task
- [X] At least 3 nodes need to be connected to continue
- [X] Fewer than 3 nodes connected is handled and client is informed
- [X] Client input requirements satisfied
- [X] Single Sum Calculation satisfied
- [X] Leader divides the list
- [X] Distributed Sum Calculation satisfied
- [X] Leader compares performances
- [X] Faulty nodes simulation
- [X] Consensus check for result verification implemented
- [X] Client output is clear
- [X] Errors are handled


## Analysis
### Test cases:
1. list: 1,2,3,4,5 delay=1000
2. list: 10,20,30,40 delay=500
3. list: 1,1,1,1,1,1,1 delay=5000
4. list: 1,2,3,4,5 delay=1000 (5 nodes)
### Results:
1. Result: 15, Single Thread time: 5009 ms, Distributed time: 2043 ms, `[LEADER ANALYSIS] -> Distributed was 2966 ms faster (59.21% improvement)`
2. Result: 100, Single Thread time: 2007 ms, Distributed time: 1038 ms `[LEADER ANALYSIS] -> Distributed was 969 ms faster (48.28% improvement)`
3. Result: 7, Single Thread time: 35035 ms, Distributed time: 15045 ms `[LEADER ANALYSIS] -> Distributed was 19990 ms faster (57.06% improvement)`
4. Result: 15, Single Thread time: 5029 ms, Distributed time: 2043 ms `[LEADER ANALYSIS] -> Distributed was 2976 ms faster (59.18% improvement)`

### Conclusions:
The first 3 tests yield roughly that with 3 nodes we get around a 50% improvement on the computation time. This is impressive and shows that using a distirbuted approach (in this case) made the computation faster. What is interesting is the 4th test. The 4th test input is identical to the first one, but with 5 nodes instead of 3, we see roughly the same 50-60% improvement. This suggests the fundamental problem with
distributed systems that on a log scale the computation time flattens out overtime and the bottlenecks become things like throughput limitations due to hardware, server load balancing etc. So the more nodes does not mean an infite amount of speed up. The key is to find an optimal amount given your use case and budget.

# Screencast
[https://youtu.be/I9Awr57Vivk]