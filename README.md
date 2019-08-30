# AirTraffic Control System

### Problem Statement
A software subsystem of an air-traffic control system is defined to manage a queue
of aircraft (AC) in an airport. The aircraft queue is managed by a process that
responds to three types of requests:
1. System boot used to start the system.
2. Enqueue aircraft used to insert a new AC into the system.
3. Dequeue aircraft used to remove an AC from the system.

AC’s have at least (but are not limited to having) the following properties:
1. AC type: Emergency, VIP, Passenger or Cargo
2. AC size: Small or Large
The process that manages the queue of AC’s satisfies the following:
1. There is no limit to the number of AC’s it can manage.
2. Dequeue aircraft requests result in selection of one AC for removal such that:
a. VIP aircraft has precedence over all other ACs except Emergency.
Emergency aircraft has highest priority.
b. Passenger AC’s have removal precedence over Cargo AC’s.
c. Large AC’s of a given type have removal precedence over Small AC’s of
the same type.
d. Earlier enqueued AC’s of a given type and size have precedence over
later enqueued AC’s of the same type and size.

### Design 

#### Solution
Assign a priority for each criteria and the flight with highest priority will be the first to be removed from the system

say,
* Aircraft Type will be assigned the following priorities
  VIP(7000), PASSENGER(5000), CARGO(3000);

* Aircraft Size will be assigned the following priorities
  LARGE(70), SMALL(30);
  
* SpecialFlags denote the EMERGENCY priority as
  EMERGENCY(500000)
  
Add up all the priorities and the aircraft with the highest priority wins!
```bash

    ============================Aircraft==============================|==========PRIORITY_SUM=========|=======RANK====
    Aircraft (id: 1, type: PASSENGER, size: SMALL)                    |   5000 + 30          =   5030 |        3
    Aircraft (id: 2, type: VIP, size: SMALL)                          |   7000 + 30          =   7030 |        2
    Aircraft (id: 3, type: CARGO, size: LARGE, specialFlag: EMERGENCY)|   3000 + 70 + 500000 = 503070 |        1
   ===================================================================|===============================|================

NOTE: The current priorities have a digit left between them for future priorities
```
#### API Design
1. POST /airports/{airportCode}/reset <!-- Reset/Reboot all the aircrafts in the given airport -->

2. POST /aircrafts <!-- Enter a new aircraft into the system -->

3. GET /aircrafts/{aircraftId} <!-- Fetch aircraft with a given Id -->

4. PUT /aircrafts/{aircraftId} <!-- Update the aircraft. Usually to change the emergency -->

5. GET /aircrafts?airportCode=IAD&aircraftType=CARGO <!-- List all emergency flights of an airport -->

6. DELETE /airports/{airportCode}/dequeueAircraft <!-- Clears an AirCraft for takeoff -->

#### Dynamo Design
##### Table 1:
Aircraft Table
Partition Key: aircraftId

Airport GSI Table
Partition Key: airportCode

Note: As we build up the queries, we can add/modify the GSI

```json
{
  "aircraftId": "9ea1bc6b-9bc0-41df-b38f-4576e0711461",
  "airportCode": "IAD",
  "aircraftSize": "LARGE",
  "aircraftSpecialFlags": "EMERGENCY",
  "aircraftType": "CARGO",
  "arrivalTime": "2019-08-27T05:00Z",
  "priorityId": "IAD-503070"
}
```
Expected queries:
1. Fetch Aircraft with id
2. Fetch all Aircraft under airportCode IAD
 
##### Table 2:
PriorityAircrafts Table
HashKey: priorityId
RangeKey: aircraftId

PriorityId-ArrivalTimeIndex LSI Table
HashKey: priorityId
RangeKey: Date
```json
{
  "date": "2019-08-29T02:10Z",
  "hashKey": "IAD-7030",
  "rangeKey": "54266ca2-2515-426a-bc44-b3ce8ee04aa9"
}
```

By using the above two tables we can efficiently find the next plane to be removed from the queue from a particular airport

Expected queries:
1. Fetch all aircrafts with priority IAD-503070 and filter by ascending arrival time

Note: We can maintain a cache that saves the active priorityIds of an airport which will improve the efficiency of the system
But for now, Using the `PriorityAircrafts Table` to store all priorityIds of a given airport
i.e
HashKey: airportCode
RangeKey: priorityId
```json
{
  "hashKey": "IAD",
  "rangeKey": "IAD-7030",
  "date": "2019-08-29T02:10Z"
}
```


## Requirements

* AWS CLI already configured with at least PowerUser permission
* [Java SE Development Kit 8 installed](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Maven](https://maven.apache.org/install.html)
* [SAM CLI](https://github.com/awslabs/aws-sam-cli)
* [Python 3](https://docs.python.org/3/)

## Setup process

### Installing dependencies

We use `maven` to install our dependencies and package our application into a JAR file:

```bash
mvn package
```

## Packaging and deployment

```bash
export BUCKET_NAME=my_cool_new_bucket
aws s3 mb s3://$BUCKET_NAME
```

Next, run the following command to package our Lambda function to S3:

```bash
sam package \
    --template-file template.yaml \
    --output-template-file packaged.yaml \
    --s3-bucket $BUCKET_NAME
```

Next, the following command will create a Cloudformation Stack and deploy your SAM resources.

```bash
sam deploy \
    --template-file packaged.yaml \
    --stack-name AirTrafficControl \
    --capabilities CAPABILITY_IAM
```

> **See [Serverless Application Model (SAM) HOWTO Guide](https://github.com/awslabs/serverless-application-model/blob/master/HOWTO.md) for more details in how to get started.**

## Testing

### Running unit tests
We use `JUnit` for testing our code.
Unit tests in this sample package mock out the DynamoDBTableMapper class for Order objects.
Unit tests do not require connectivity to a DynamoDB endpoint. You can run unit tests with the
following command:

```bash
mvn test
```

## To-do / Improvements

1. Make the [following operations](https://github.com/varma-ksbh/AirTrafficControlSystem/blob/master/src/main/java/com/varma/airtraffic/control/handler/DequeueAircraftHandler.java#L73-L82) transcational

2. [AirportPriority model](https://github.com/varma-ksbh/AirTrafficControlSystem/blob/master/src/main/java/com/varma/airtraffic/control/model/AirportPriority.java) has a constant number of records and should be moved to redis or similar in-memory solutions for greater performance

3. Almost all of the API validation can be moved to swagger models insteaad of performing those checks in lambda. This will reduce the unneeded invocations of lambda 

4.. Start implementing the web interface for the application. This will put us more in the customer shoes and help us in designing better API's. 


## Few Choices & Decisions
Few choices and Decisions

choice 1: Submit an in-memory solution within a day or two and give a detailed explanation of how it can be extended.

Decision: An in-memory solution of using heaps is not directly transferable to persistent datastore, which means this is throw away work. Hence, discarded this choice

choice 2: Which datastore?
Decision: Looked for a managed service that can scale horizontally and was biased for DynamoDB as I have worked with it. With dynamo supporting transactions, I backed my decision.

Choice 3: How can you run real-time fast lookup against DynamoDB?
Decision: I have decided not to use Global secondary Indexes altogether for critical application functionality (Flight enqueue & dequeue) to avoid eventual consistency. 
A smart application decision of having the priority as single attribute allowed me to create an index on it, allowing me to do fast lookups. Read the readme.md for more info

Choice 4: DynamoDB streams vs DynamoDB Transcations
Decision: Go with DynamoDB transcations as streams in worst case have a high latency or eventual consistency

Choice 5: How to expand to future/unknown queries
Decision: All queries such as list/etc that are not part of critical application function should be implemented on GSI tables to avoid consuming read/write units of the critical operations.

I.e say List all emergency aircrafts should be served from a GSI table. We might show a stale record because of eventual consistency, but the overall application functionality will never be hampered. I assumed that its okay for the list call to be eventually consistent.

Choice 7: Operational metrics and alarms
Decision: We can use DLQ to store all failed transcations and alarma on that, and other resource consumption can be alarmed using cloudwatch. Say dynamo read/write consumed units, api faults and errors etc.
