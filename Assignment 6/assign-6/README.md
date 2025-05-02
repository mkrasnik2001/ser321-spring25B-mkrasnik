# Assignment 6 - Michael Krasnik
## Commands to run the program
1. `cd` into `assign-6/`
2. build the project using `gradle clean build` (PLEASE NOTE: comment test/ServerTest.java, because the server is not running, these tests will pass after you run the server)
3. Run the node with default values with `gradle runNode --console=plain -q`
4. Run the client with default values with `gradle runClient --console=plain -q`


# Program explanation and workflow
This is a project that uses google remote procedure calls middleware to route correct requests and responses from the client to the server for certain services
that are stub implemented on the client side, but the server actually implements the logic. I added the Fitness and Sort services for Task 1 and for Task 2 i created
a Quote service. The quote service allows the client to add quotes, get quotes by an id and return a list of quotes. The quotes on the server side are not persistent, but the other 3 requirements (see below)
have been met.

## How to work with the program:
For task 1 fitness service - you must first add an exercise by choosing the correct service and defining its type for example CARDIO, then the description,
after which to get that exercise, provide the correct type and that exercise will be returned.
For task 1 sort service - you must provide an array of ints to sort space delimited like (50 12 13). The service will return the sorted array.

For task 2 - the service created from scratch is the Quote service. This services allows the client to add quotes, get quotes by id and list the quotes
that already exist. Choose the correct service and add the quote when it prompts you the text. To get a quote by id (if that id exists), you must provide
the id when asked and the quote will be returned. to list all existing ones choose that service and all existing quotes will be returned (the quotes aren't persistent other requirements are met though)


# Requirements List
### Task 1
- [X] gradle runNode and gradle runClient works with default vals.
- [X] Fitness service implemented.
- [X] Sort service implemented.
- [X] User calls and interactions are easy to understand and designed well.
- [X] Nice terminal implemented for client to choose a service.
- [X] ALTERNATIVE option implemented: unit tests implemented for Task 1 services in `ServerTest.java`
- [X] Server and Client are robust and don't crash
### Task 2
- [X] Server allows at least 2 different requests (add quote, get quote by id, get quote list)
- [X] Each request has at least 1 input (add quote and get quote by id)
- [X] Response returns a repeated field (list of quotes when you get quote list)
- [X] Protocol well designed
- [X] Clients lets user choose this service and ask for needed information
- [X] Server is robust and described in readme


# Screencast
[https://youtu.be/zXrPbHpqaLk]