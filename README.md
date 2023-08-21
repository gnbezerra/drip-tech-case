# Drip | Tech Case - George Bezerra

**Description**: this is an implementation of a transfer agent for Drip's backend engineer tech case.
This simulates money transfers between bank accounts, with fees for transfers between accounts in
different banks.

It was made in Kotlin, using Spring Boot as its framework.

## Stack

- Language: Kotlin
- Framework: Spring Boot (chosen because it requires less configuration than vanilla Spring,
  even though it's more opinionated)
- Build tool: Gradle
- Database: H2
- Web server: Tomcat
- Test framework: JUnit 5 + Mockito

### Additional external libraries used

- ModelMapper: a mapper between different classes with similar properties, used extensively to map
  from DTO to Entity and back.
- PODAM: POjo DAta Mocker, used in tests to build instances of entities and DTOs with randomized
  attribute values.

## Dependencies

To run this case, you'll need:

- JDK version 17 or higher;
- Gradle (most recent version recommended);
- Kotlin version 1.8.22 or higher, or and IDE that comes with a bundled Kotlin installation
  like IntelliJ IDEA.

Since the application is a REST API, it's also recommended to use Postman or an equivalent to test
inputs and outputs.

_(I know you could do this with `curl`, but... Postman is good.)_

## How to run

In a terminal, go to the project's root directory and run:

```shell
./gradlew bootRun
```

This will build and run the application. After a while, the web server will be up locally at
Spring Boot's default URL: http://localhost:8080.

## How to test

In a terminal, go to the project's root directory and run:

```shell
./gradlew test
```

This will run both unit and integration tests.

## Endpoints and usage

After starting the server, you can check the API Swagger documentation at
http://localhost:8080/swagger-ui.html.

**Warning: notice that we use H2 as a local database, which means data will only be persisted while the server
is up. A server restart will wipe the DB and create a new one.**

### General API info

http://localhost:8080/: basic server info, only method allowed is GET

For brevity, I just put the Swagger UI link in there.

### Bank operations

http://localhost:8080/bank/: accepts POST and GET methods

**GET**: will list all banks in the DB

**POST**: will save a new bank to the DB. It expects a JSON payload with the following format:

```json
{
  "name": "Itaú",
  "code": "341"
}
```

Where `code` is the standard Brazil COMPE code, with 3 digits. For simplicity, I didn't include
other identifiers, like SWIFT code and such.

If the payload is valid, it will return an "HTTP 201 Created" response with the following format:

```json
{
  "id": 1,
  "name": "Itaú",
  "code": "341",
  "createdAt": "2023-08-21T13:43:43.989718Z",
  "updatedAt": "2023-08-21T13:43:43.989718Z"
}
```

Bank codes are unique. If you try to save a new bank with the same code as an existing one,
the server will return an error.

**To make the tech case more simple, no other endpoints were provided.
It's not possible to query a single bank entry, update or delete a bank's information.**

### Customer operations

http://localhost:8080/customer/: accepts POST and GET methods

**GET**: will list all customers in the DB

**POST**: will save a new customer to the DB. It expects a JSON payload with the following format:

```json
{
  "fullName": "George Bezerra",
  "cpf": "32165498701"
}
```

For a customer we can have all sorts of data, like nationality, address, ID number (RG in Brazil)
and so on. Also, foreigners would use a passport number, instead of a CPF number. However, since
the representation of a customer is not the focus of this tech case, I tried to make it as simple
as possible by storing just two pieces of data: full name and CPF.

For the CPF, it must be passed in the format above: 11 digits, no dots or dashes.

If the payload is valid, it will return an HTTP 201 Created response with the following format:

```json
{
  "id": 1,
  "fullName": "George Bezerra",
  "cpf": "32165498701",
  "createdAt": "2023-08-21T13:43:43.989718Z",
  "updatedAt": "2023-08-21T13:43:43.989718Z"
}
```

CPF numbers are unique. If you try to save a new customer with the same CPF as an existing one,
the server will return an error.

**To make the tech case more simple, no other endpoints were provided.
It's not possible to query a single customer entry, update or delete a customer's information.**

### Account operations

http://localhost:8080/account/: accepts POST and GET methods

**GET**: will list all accounts in the DB. An account is related to a bank and a customer.

**POST**: will save a new account to the DB. It expects a JSON payload with the following format:

```json
{
  "bankCode": "341",
  "customerCpf": "32165498701",
  "branch": "4201",
  "accountNumber": "12345-X",
  "money": 2000.00
}
```

To be able to save a new account, the respective bank and customer must be saved to the database
beforehand. You can omit the `money` key/value pair in the request, in which case the account will
be started with R$ 0.

For production code we would probably need to store currency as well (BRL, USD, etc.), but for
simplicity we assume that all monetary values are in BRL and there's no currency conversion.

**To make the tech case more simple, no other endpoints were provided.
It's not possible to query a single account entry, update or delete an account's information,
including money inside the account.**

### Transfer operations

http://localhost:8080/transfer/: accepts only POST method

**POST**: will perform a transfer transaction from one account to another. It expects a JSON payload
in the following format:

```json
{
  "sourceAccount": {
    "bankCode": "341",
    "branch": "4201",
    "accountNumber": "12345-X"
  },
  "destinationAccount": {
    "bankCode": "260",
    "branch": "0001",
    "accountNumber": "0001-0"
  },
  "amount": 500.00
}
```

Both the source account and the destination account need to be saved in the database prior to the
transfer. It's not possible to transfer from one account to the same account.

Transfers between two accounts in the same bank have no fees. Meanwhile, transfers between accounts in
different banks have an R$ 5 transaction fee.

Several validations are made, like the existence of both accounts and if the source account has enough
funds for the transaction, including fees. In case of a successful transaction, an "HTTP 200 OK"
response is sent with a JSON payload in the following format:

```json
{
  "sourceAccount": {
    "bank": {
      "name": "Itaú",
      "code": "341",
      "id": 1,
      "createdAt": "2023-08-18T21:13:51.933824Z",
      "updatedAt": "2023-08-18T21:13:51.933824Z"
    },
    "customer": {
      "fullName": "George Bezerra",
      "cpf": "32165498701",
      "id": 1,
      "createdAt": "2023-08-18T21:13:53.802395Z",
      "updatedAt": "2023-08-18T21:13:53.802395Z"
    },
    "branch": "4201",
    "accountNumber": "12345-X",
    "money": 1495.00,
    "id": 1,
    "createdAt": "2023-08-18T21:13:58.983748Z",
    "updatedAt": "2023-08-18T21:14:15.181325Z"
  },
  "destinationAccount": {
    "bank": {
      "name": "Nubank",
      "code": "260",
      "id": 2,
      "createdAt": "2023-08-18T21:13:46.125437Z",
      "updatedAt": "2023-08-18T21:13:46.125437Z"
    },
    "customer": {
      "fullName": "George Bezerra",
      "cpf": "32165498701",
      "id": 1,
      "createdAt": "2023-08-18T21:13:53.802395Z",
      "updatedAt": "2023-08-18T21:13:53.802395Z"
    },
    "branch": "0001",
    "accountNumber": "0001-0",
    "money": 500.00,
    "id": 2,
    "createdAt": "2023-08-18T21:13:55.575060Z",
    "updatedAt": "2023-08-18T21:14:15.181325Z"
  },
  "amount": 500.00,
  "commission": 5.00,
  "performedAt": "2023-08-18T21:14:20.040517900Z",
  "id": 1
}
```

## What was done and what could not be done

For this, I made:

- Unit tests
- Integration tests
- List and Create operations for banks, customers and accounts
- Perform transfers
- Validations on inputs and inside controllers
- Swagger documentation
- Separation between DTOs and Entities
- Usual separation of Controllers/Repositories/Entities that Spring already provides
- 30% failure chance (implemented in the naïvest way possible, a `Random.nextDouble()` call)
  in the transfer service for interbank transfers, along with an exponential delay retry strategy.
  Exponential to not hammer away the service, included jitter in the exponential time as well to not
  have hundreds of processes hammering the transfer service every few seconds, all at the same time.

What I didn't do

- Any other operations on basic entities outside of List and Create. Should be easy to make,
  but would demand more tests and more time. I chose this because I believe those were not the focus of
  this test, and it's pretty easy to wipe the DB and start again by restarting the server.
- A way to list transfers (incoming and outgoing) by account. The model is prepared for that, since it
  has the FKs needed, but would take additional time.
- Different currencies.

## Last considerations

Finally, I know I took more time than I should have with this tech case. I blame both the preparations
for my move to another state that will happen this week (I'll move from SP to GO), and also the fact
that I was very rusty with Java, Kotlin and Spring (I've been coding in Python since 2019),
so I needed some time to ramp up again.

If the delay is unacceptable, I understand and accept it. However, I'm very stoked about the possibility
of working in Drip. I've put all the other hiring processes in standby since I started writing this
test, so I ask that, in case my application is refused, I receive a reply from Drip, so I can resume my
processes with other companies.
