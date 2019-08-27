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
1. POST /airports/{airportCode}/reset <!-- Reset all the aircrafts in the given airport -->

2. POST /aircrafts <!-- Enter a new aircraft into the system -->

3. GET /aircrafts/{aircraftId} <!-- Fetch aircraft with a given Id -->

4. PUT /aircrafts/{aircraftId} <!-- Update the aircraft.usually to change the emergency -->

5. GET /aircrafts?aircraftSpecialFlag=EMERGENCY&airportCode=IAD <!-- List all emergency flights of an airport -->

6. PUT /airports/{airportCode}/clearForTakeOff <!-- Clears an AirCraft for takeoff -->

#### Dynamo Design
#####Table 1:
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
2. Fetch all aircrafts under airportCode IAD
 
#####Table 2:
PriorityAircrafts Table
Partition Key: priorityId

PriorityId-ArrivalTimeIndex LSI Table
SecondaryIndex: Date
```json
{
  "priorityId": "IAD-503070",
  "arrivalTime": "2019-08-27T05:00Z",
  "aircraftId": "9ea1bc6b-9bc0-41df-b38f-4576e0711461"
}
```

By using the above two tables we can efficiently find the next plane to be removed from the queue from a particular airport

Expected queries:
1. Fetch all aircrafts with priority IAD-503070 and filter by ascending arrival time

Note: We can maintain a cache that saves the active priorityIds of an airport which will improve the efficiency of the system

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