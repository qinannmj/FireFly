FireFly
=======

A strong consistency synchronization framework based on paxos algorithm.
FireFly provides three synchronized ways of paxos including paxos pipe to optimize rt in different machine room,route calc will be prior to use lowest cost node chain.
Provides strong consistently read for one class PaxosOperator in one tcp.
Can change the scale of node in runtime,but don't damage consistency.
You can benefit from paxos including error tolerance,high availability,but don't spend too high cost of develop.
FireFly has ability to tolerate master shut down during nearly Heart-Beat-Interval-Time * 2,if you set Heart-Beat-Interval-Time to 
2s that indicate the system will spent lower than 4s to tolerate master shutdown.
