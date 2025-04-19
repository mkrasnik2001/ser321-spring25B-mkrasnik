# Description 
This is a simple Sudoku game. Many players can play a separate game but see the same leaderboard. Player can type "exit" at any point during the game state to quit the game and disconnect the client from the server.

## How to run the program
1. `cd` into `activity2` direc inside `Assignment\ 4`  
2. Build the gradle project by running `gradle clean build`  
3. Start the server by running `gradle runServer`  
4. Start the client(s) by running `gradle runClient`  
   - Default port is `8000` and default host is `localhost`  
   - Port and hostIP specification is optional.  
5. To run the grading version run `gradle runServerGrading`. This will run the same thing, but with the graded board that also shows up on the server side for convenience.
---
### PLEASE NOTE

* The unit test "Testing game flow to Leaderboard" was commented out because of the comment in it: _this test only passes when that person has not logged in yet!! Could be done better but here we are_
* For `gradle clean build` to work the unit tests have to pass and for them to pass the `gradle runServerGrading` needs to be running. So if these unit tests will be used for grading,
please comment out the whole unit test file, build the gradle project and then uncomment the unit tests and build again or run `gradle test`. Thank you!
---
## Screencast

---

# Requirements (✓ = Completed with debugging)
- [x] Project runs through gradle  
- [x] Given protocol implemented  
- [x] runServerGrading works  
- [x] Game can handle multiple clients  
- [x] Menu implemented server side  
- [x] Calls designed easily  
- [x] Leadboard implemented and persistent and safe  
- [x] Leaderboard has points, logins and name  
- [x] Difficuly implemented when starting game  
- [x] In game menu implemented and shown during game  
- [x] row col val format shown for client on how to add values to the board  
- [x] Server handles the users row col and val inputs and appropriately returns correct responses  
- [x] All clears work
- [x] Clients win when no Xs are left  
- [x] Points added as needed  
- [x] Game quits gracefully  
- [x] user typing exist during the game graceully quits  
- [x] server doesn't crash when client disconnects  
- [x] server/client doesn't crash when receiving wrong input  
- [x] deployed on AWS  
- [x] 2 other servers are tested  