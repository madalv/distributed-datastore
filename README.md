## About

In-memory distributed datastore written in Kotlin, loosely based on the
Raft algorithm with the differences that 
1) no log commit, logs are sent whole instead of suffixes 
2) it's distributed 
3) it does not rely completely on the total message broadcast algorithm for delivering messages. 

This simplified "version" of Raft does not guarantee 100% the message order, 
as it does not rely on log replication for delivery (instead, log replication is used as a data sync mechanism). As
the cluster is a group of containers in a virtual Docker network, the probability that the messages will arrive out of order or get lost is next 
to none, thus I consider the deviation from the total message broadcat algorithm in the original Raft is acceptable.

Developed for academic purposes during the Networking Programming course.

## Steps to run

Run in root folder:
```
docker compose up --build
```

