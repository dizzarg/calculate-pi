# Calculate Pi using Leibniz's Formula

## Summary

Two solution to calculate Pi number with Java.

* **Singleton** - calculates PI number in a single JVM using Producer-Consumer pattern
* **Distributed** - calculates the PI number in a distributed manner using a master and some workers. 

All solutions use the Gottfried Leibniz formula for calculation of pi:
 
 1 -  1/3  + 1/5 - 1/7 + 1/9 - ... = π/4
 
Source: [Wikipedia](https://en.wikipedia.org/wiki/Leibniz_formula_for_π) - Leibniz formula for π

### Requirements:
  * Oracle Java SE Development Kit 8 
  * Apache Maven 3.x
  * Docker 18.03 

## Build
The project is built by a command `mvn clean package`.

## Run
### Singleton solution

Run application.
 ```bash
 [calculate-pi] java -jar standalone/target/standalone.jar                                                                                               
 12:54:39.677 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Calculate 3.141593
 12:54:39.683 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Math.PI   3.141593
 12:54:41.901 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Calculate 3.1415926
 12:54:41.901 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Math.PI   3.1415927
 12:55:04.030 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Calculate 3.14159265
 12:55:04.031 [main] INFO  r.d.c.p.s.StandaloneSolutionApp - Math.PI   3.14159265
 ```
### Distributed solution

* Run docker containers:
 ```bash
 [calculate-pi] docker-compose up 
 ```
### Run cluster
* Run master
 ```bash
[calculate-pi] java -jar distributed/master/target/master.jar localhost:2181
13:07:25.579 [main] INFO  r.d.c.pi.distributed.master.Master - Running for master
13:07:25.585 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Parent created
13:07:25.586 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Parent created
13:07:25.586 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Parent created
13:07:25.586 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Parent created
13:07:25.586 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Going for list of workers
13:07:25.591 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - I'm the leader 5e2c5534
13:07:25.591 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Succesfully got a list of workers: 0 workers
13:07:25.591 [main-EventThread] INFO  r.d.c.p.d.m.RecoveredAssignments - Getting tasks for recovery
13:07:25.592 [main-EventThread] INFO  r.d.c.p.d.m.RecoveredAssignments - Getting worker assignments for recovery: 0
13:07:25.592 [main-EventThread] WARN  r.d.c.p.d.m.RecoveredAssignments - Empty list of workers, possibly just starting
13:07:25.592 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assigning recovered tasks
 ```
* Run worker
 ```bash
 [calculate-pi] java -jar distributed/worker/target/worker.jar localhost:2181
 13:09:44.358 [main-EventThread] INFO  r.d.c.pi.distributed.worker.Worker - Assign node created
 13:09:44.358 [main-EventThread] INFO  r.d.c.pi.distributed.worker.Worker - Registered successfully: 448d3960
 13:09:44.359 [pool-1-thread-1] INFO  r.d.c.pi.distributed.worker.Worker - Looping into tasks
 ```
 
### Testing 
 
#### Console client

Run client which create task
  ```bash
[calculate-pi] java -jar distributed/client/target/client.jar localhost:2181
13:15:32.368 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000002, result = 3.1415926485894077 
13:15:32.373 [ForkJoinPool.commonPool-worker-1] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.1415926485894077
13:15:32.375 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000002
13:15:54.590 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000003, result = 3.1415926530880767 
13:15:54.591 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.1415926530880767
13:15:54.592 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000003
13:15:54.605 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000000, result = 3.141592153589724 
13:15:54.606 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.141592153589724
13:15:54.607 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000000
13:15:54.830 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000001, result = 3.141592603589817 
13:15:54.830 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.141592603589817
13:15:54.830 [main] INFO  r.d.c.p.d.common.ZKClientImpl - Closing
13:15:54.831 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000001
13:15:54.832 [main] INFO  org.apache.zookeeper.ZooKeeper - Session: 0x166f2fadefd00fd closed
13:15:54.832 [main-EventThread] INFO  org.apache.zookeeper.ClientCnxn - EventThread shut down for session: 0x166f2fadefd00fd
  ```  

After run client in the master's logs would be written:
 ```bash
13:15:30.019 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assigning recovered tasks
13:15:30.023 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assignment path: /assign/worker-448d3960/task-0000000002
13:15:30.023 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assignment path: /assign/worker-448d3960/task-0000000003
13:15:30.023 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assignment path: /assign/worker-448d3960/task-0000000000
13:15:30.023 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Assignment path: /assign/worker-448d3960/task-0000000001
13:15:30.025 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Task assigned correctly: /assign/worker-448d3960/task-0000000002
13:15:30.026 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Task assigned correctly: /assign/worker-448d3960/task-0000000003
13:15:30.026 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Task assigned correctly: /assign/worker-448d3960/task-0000000000
13:15:30.027 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Task assigned correctly: /assign/worker-448d3960/task-0000000001
13:15:30.028 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Successfully deleted /tasks/task-0000000002
13:15:30.029 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Successfully deleted /tasks/task-0000000003
13:15:30.029 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Successfully deleted /tasks/task-0000000000
13:15:30.029 [main-EventThread] INFO  r.d.c.pi.distributed.master.Master - Successfully deleted /tasks/task-0000000001
 ``` 
  
After run client in the worker's logs would be written:
 ```bash
13:15:32.368 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000002, result = 3.1415926485894077 
13:15:32.373 [ForkJoinPool.commonPool-worker-1] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.1415926485894077
13:15:32.375 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000002
13:15:54.590 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000003, result = 3.1415926530880767 
13:15:54.591 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.1415926530880767
13:15:54.592 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000003
13:15:54.605 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000000, result = 3.141592153589724 
13:15:54.606 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.141592153589724
13:15:54.607 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000000
13:15:54.830 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Task /status/task-0000000001, result = 3.141592603589817 
13:15:54.830 [ForkJoinPool.commonPool-worker-2] INFO  r.d.c.p.d.c.DistributedSolutionApp - Task completed. Result = 3.141592603589817
13:15:54.830 [main] INFO  r.d.c.p.d.common.ZKClientImpl - Closing
13:15:54.831 [main-EventThread] INFO  r.d.c.p.d.client.DistributedSolution - Successfully deleted /status/task-0000000001
13:15:54.832 [main] INFO  org.apache.zookeeper.ZooKeeper - Session: 0x166f2fadefd00fd closed
13:15:54.832 [main-EventThread] INFO  org.apache.zookeeper.ClientCnxn - EventThread shut down for session: 0x166f2fadefd00fd
 ``` 
 
#### Web client
 
Run web client 

```bash
[calculate-pi] java -jar spring/spring-master/target/spring-master-1.0-SNAPSHOT.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.6.RELEASE)

21:55:06.413 [main] INFO  r.d.c.p.s.master.SpringMasterApp - Starting SpringMasterApp on dkadyrov-PC with PID 15113 (/home/dkadyrov/Documents/projects/sandbox/calculate-pi/spring/spring-master/target/spring-master-1.0-SNAPSHOT.jar started by dkadyrov in /home/dkadyrov/Documents/projects/sandbox/calculate-pi)
21:55:06.420 [main] INFO  r.d.c.p.s.master.SpringMasterApp - No active profile set, falling back to default profiles: default
21:55:06.537 [main] INFO  o.s.b.w.r.c.AnnotationConfigReactiveWebServerApplicationContext - Refreshing org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext@2471cca7: startup date [Mon Nov 12 21:55:06 MSK 2018]; root of context hierarchy
21:55:08.544 [main] WARN  reactor.ipc.netty.tcp.TcpResources - [http] resources will use the default LoopResources: DefaultLoopResources {prefix=reactor-http, daemon=true, selectCount=4, workerCount=4}
21:55:08.544 [main] WARN  reactor.ipc.netty.tcp.TcpResources - [http] resources will use the default PoolResources: DefaultPoolResources {name=http, provider=reactor.ipc.netty.resources.PoolResources$$Lambda$249/231977479@551bdc27}
21:55:08.893 [main] INFO  org.mongodb.driver.cluster - Cluster created with settings {hosts=[localhost:27017], mode=SINGLE, requiredClusterType=UNKNOWN, serverSelectionTimeout='30000 ms', maxWaitQueueSize=500}
21:55:09.307 [cluster-ClusterId{value='5be9cc8cac71db3b09a878e2', description='null'}-localhost:27017] INFO  org.mongodb.driver.connection - Opened connection [connectionId{localValue:1, serverValue:15}] to localhost:27017
21:55:09.339 [cluster-ClusterId{value='5be9cc8cac71db3b09a878e2', description='null'}-localhost:27017] INFO  org.mongodb.driver.cluster - Monitor thread successfully connected to server with description ServerDescription{address=localhost:27017, type=STANDALONE, state=CONNECTED, ok=true, version=ServerVersion{versionList=[4, 0, 4]}, minWireVersion=0, maxWireVersion=7, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=24741341}
21:55:09.433 [main] INFO  org.mongodb.driver.cluster - Cluster created with settings {hosts=[localhost:27017], mode=SINGLE, requiredClusterType=UNKNOWN, serverSelectionTimeout='30000 ms', maxWaitQueueSize=500}
21:55:09.587 [cluster-ClusterId{value='5be9cc8dac71db3b09a878e3', description='null'}-localhost:27017] INFO  org.mongodb.driver.connection - Opened connection [connectionId{localValue:2, serverValue:16}] to localhost:27017
21:55:09.590 [cluster-ClusterId{value='5be9cc8dac71db3b09a878e3', description='null'}-localhost:27017] INFO  org.mongodb.driver.cluster - Monitor thread successfully connected to server with description ServerDescription{address=localhost:27017, type=STANDALONE, state=CONNECTED, ok=true, version=ServerVersion{versionList=[4, 0, 4]}, minWireVersion=0, maxWireVersion=7, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=1603537}
21:55:10.558 [main] INFO  o.s.w.r.r.m.a.RequestMappingHandlerMapping - Mapped "{[/tasks],methods=[POST]}" onto public reactor.core.publisher.Mono<ru.dkadyrov.calculate.pi.spring.master.model.Task> ru.dkadyrov.calculate.pi.spring.master.rest.TaskReportRest.create(ru.dkadyrov.calculate.pi.spring.master.model.Task)
21:55:10.560 [main] INFO  o.s.w.r.r.m.a.RequestMappingHandlerMapping - Mapped "{[/tasks],methods=[GET]}" onto public reactor.core.publisher.Flux<ru.dkadyrov.calculate.pi.spring.master.model.Task> ru.dkadyrov.calculate.pi.spring.master.rest.TaskReportRest.list()
21:55:10.604 [main] INFO  o.s.w.r.h.SimpleUrlHandlerMapping - Mapped URL path [/webjars/**] onto handler of type [class org.springframework.web.reactive.resource.ResourceWebHandler]
21:55:10.604 [main] INFO  o.s.w.r.h.SimpleUrlHandlerMapping - Mapped URL path [/**] onto handler of type [class org.springframework.web.reactive.resource.ResourceWebHandler]
21:55:10.663 [main] INFO  o.s.w.r.r.m.a.ControllerMethodResolver - Looking for @ControllerAdvice: org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext@2471cca7: startup date [Mon Nov 12 21:55:06 MSK 2018]; root of context hierarchy
21:55:11.252 [main] INFO  o.s.j.e.a.AnnotationMBeanExporter - Registering beans for JMX exposure on startup
21:55:11.388 [reactor-http-server-epoll-5] INFO  r.ipc.netty.tcp.BlockingNettyContext - Started HttpServer on /0:0:0:0:0:0:0:0%0:8080
21:55:11.389 [main] INFO  o.s.b.w.e.netty.NettyWebServer - Netty started on port(s): 8080
21:55:11.398 [main] INFO  r.d.c.p.s.master.SpringMasterApp - Started SpringMasterApp in 5.68 seconds (JVM running for 6.562)
21:55:18.168 [nioEventLoopGroup-2-2] INFO  org.mongodb.driver.connection - Opened connection [connectionId{localValue:3, serverValue:17}] to localhost:27017
```

##### Create task 
Create task example method:

```http request
POST http://localhost:8080/tasks
Content-Type: application/json

{
   "digits": 7
}
```
It will return result
```json
{
    "id": "5be9c88fac71db34accfa29c",
    "digits": 7,
    "result": 3.1415926485894077,
    "createdAt": "2018-11-12T18:38:07.724+0000",
    "updatedAt": "2018-11-12T18:38:10.814+0000"
  }
```

##### Get executed task's lists

```http request
GET http://localhost:8080/tasks
``` 

It will return result
```json
[
  {
    "id": "5be9c709ac71db31c5ccfa32",
    "digits": 3,
    "result": 3.1415426535898248,
    "createdAt": "2018-11-12T19:09:13.836+0000",
    "updatedAt": null
  },
  {
    "id": "5be9c827ac71db341231509f",
    "digits": 3,
    "result": 3.1415426535898248,
    "createdAt": "2018-11-12T18:36:23.621+0000",
    "updatedAt": "2018-11-12T18:36:23.752+0000"
  },
  {
    "id": "5be9c838ac71db34123150a0",
    "digits": 5,
    "result": 3.141592153589724,
    "createdAt": "2018-11-12T18:36:40.456+0000",
    "updatedAt": "2018-11-12T18:36:40.527+0000"
  },
  {
    "id": "5be9c850ac71db34123150a1",
    "digits": 7,
    "result": 3.1415926485894077,
    "createdAt": "2018-11-12T18:37:04.369+0000",
    "updatedAt": "2018-11-12T18:37:07.353+0000"
  },
  {
    "id": "5be9c88fac71db34accfa29c",
    "digits": 7,
    "result": 3.1415926485894077,
    "createdAt": "2018-11-12T18:38:07.724+0000",
    "updatedAt": "2018-11-12T18:38:10.814+0000"
  }
]
```