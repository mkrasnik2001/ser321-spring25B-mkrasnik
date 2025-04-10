# Pixel Movie Guessing Game - Michael Krasnik {mkrasnik} (Assignment 3 SER 321)

This project is a single player client - server application where the client connects to the server and is given images of different movies to guess. The client wins points by guessing correctly and has the option to get a clearer image from the server of the current movie. There are limitations to this and there is also a leaderboard that tracks the performance of all the players. Players play one at a time, but the leaderboard is persistent. The server is responsible for the game logic and state management of the round. It validates inputs, handles errors and uses a custom JSON protocol that will be described below to communcate with the client.

---

## Requirements Checklist

- [X] Client is "dumb" and does not handle any logic as far as the game goes.
- [X] The server receives a "hello" request from the client and asks the client to provide a name (socket opens).
- [X] Client sends their name and server greets client with that name.
- [X] Client is presented with a choice of logging out (quiting), starting a new game or viewing the leaderboard.
- [X] A DISCONNECT (Quit) button is implemented that fully disconnects the client from the server.
- [X] Gameplay options are implemented (Next, Skip, Remaining and Guesses).
- [X] Leaderboard is implemented + persistent.
- [X] At the end of the game, the client sees their score and absolute high score. Absolute highscore is defined as the sum of the points gathered and the highest they've reached (regardless of game duration). The true high score is recorded in the leaderboard (see more info on this below).
- [X] Game ends regardless of answer if time runs out.
- [X] DISCONNECT (Quit) button displays a quit image sent from the server and closes the connection.
- [X] Protocol is robust and handles errors well.

---

## JSON Protocol Description

Each message between the client and server is a JSON object with two main parts: a header and a payload. The protocol is desgiend so that every request and response clearly shows the type, status and the value (if there is one) in the payload.

### General Message Structure

```json
{
  "header": {
    "type": "<messageType>",      // See below for details.
    "playerName": "<String>",     // The player's name (if applicable).
    "ok": true/false             // true if the previous operation succeeded; false otherwise.
  },
  "payload": {
    // Additional information such as "value", "duration", "points", "imageBase64", etc.
  }
}
```
---
### All Message Types:
- start – Initiates the connection from the client.
- hello – Response from the server to request the player's name.
- name – Used when the client sends the player’s name.
- menuOpts – Sent by the server to present menu options (e.g., after name handling).
- promptGameLength – Used when the server prompts the client to choose a game duration.
- checkGameLength – Sent by the client with the selected game duration, and the server verifies and sets up the game.
- gameStart – Sent by the server to indicate that the game has started (includes initial image and game parameters).
- game – Used for in-game actions such as guessing, "skip", "next", or "remaining".
- gameUpdate – Used for any update within a game round (correct/incorrect guess, next image, skip response).
- gameOver – Sent by the server when the game time expires.
- gameWin – Sent by the server when the player wins (all movies guessed).
- quit – Used when the client chooses to quit the game.
- info – Used for informational messages (e.g., remaining skips).

---
### 1. HandShake
*Client Request:*
```json
{
  "header": {
    "type": "start",
    "player": "",
    "ok": true
  },
  "payload": {}
}
```

*Success Response:*
```json
{
  "header": {
    "type": "hello",
    "ok": true
  },
  "payload": {
    "value": "[MoviePixel Inc]: Hello, please tell me your name.",
    "imageBase64": "<Base64 string of hi.png>",
    "imageName": "hi.png"
  }
}
```
*Error Response:*
```json
{
  "header": {
    "type": "error",
    "ok": false
  },
  "payload": {
    "value": "[MoviePixel Inc]: Something went wrong while initiating connection. Please try again."
  }
}
```

### 2. Name
*Client Request:*
```json
{
  "header": {
    "type": "name",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "Bob"
  }
}
```

*Success Responses:*
```json
{
  "header": {
    "type": "menuOpts",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Hello Bob, welcome to the Movie Game! Please select above what you want to do..."
  }
}
```
*Returning player:*
```json
{
  "header": {
    "type": "menuOpts",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Welcome back Bob. Your absolute highest score is: 150. Please select what you want to do..."
  }
}
```

*Error Response:*
```json
{
  "header": {
    "type": "error",
    "ok": false
  },
  "payload": {
    "value": "[MoviePixel Inc]: Invalid name. Name cannot be a number or empty!"
  }
}
```

### 3. Prompt Game Length
*Client Request:*
```json
{
  "header": {
    "type": "promptGameLength",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "Requesting to start the game"
  }
}
```

*Success Response - Game Start:*
```json
{
  "header": {
    "type": "gameStart",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: 30 sec game started! Type in your guess (upper or lower case), type 'next' to view the next image, 'skip' to skip this movie, or 'remaining' to see remaining skips.",
    "duration": 30,
    "skipsAllowed": 2,
    "imageBase64": "<Base64 string of *.png>",
    "imageName": "TheDarkKnight1.png"
  }
}
```

*Error Response:*
```json
{
  "header": {
    "type": "error",
    "ok": false,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Invalid duration given. Please enter 'short (30 sec game)', 'medium (60 sec game)', or 'long (90 sec game)'.",
    "duration": 0
  }
}
```

### 4. Game Play Commands
After the game starts, the client sends in-game commands using "type": "game". The payload’s "value" determines the command.

*Client Request:*
```json
{
  "header": {
    "type": "game",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "TheDarkKnight"
  }
}
```

*Success Response (Correct Guess):*
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Correct! You've earned 10 points. Moving to next movie: back to the future.",
    "points": 10,
    "imageBase64": "<Base64 string of BackToTheFuture1.png>",
    "imageName": "BackToTheFuture1.png"
  }
}
```

*Success Response (Incorrect Guess):*
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Incorrect guess. Try again or type 'skip', 'next', or 'remaining'.",
    "points": 0
  }
}
```

### 4.b Skip Command
*Client Request:*
```json
{
  "header": {
    "type": "game",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "skip"
  }
}
```

*Success Response (Skips Available):*
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Movie skipped. Here is a new movie image.",
    "imageBase64": "<Base64 string of BackToTheFuture1.png>",
    "imageName": "BackToTheFuture1.png",
    "imageLevel": 1
  }
}
```

*Error Response (No Skips):*
Here the ok status is true even though it's an error, but handled via the payload.value
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: No skips remaining."
  }
}
```

### 4.c Next Command
*Client Request:*
```json
{
  "header": {
    "type": "game",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "next"
  }
}
```
*Success Response:*
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Here is a less pixelated image.",
    "imageBase64": "<Base64 string of TheDarkKnight2.png>",
    "imageName": "TheDarkKnight2.png",
    "imageLevel": 2
  }
}
```

*Error Response (No Nexts Left):*
Here the ok status is true even though it's an error, but handled via the payload.value
```json
{
  "header": {
    "type": "gameUpdate",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: No more 'nexts' available. You have been shown the clearest image."
  }
}
```

### 4.d Remaining Command
*Client Request:*
```json
{
  "header": {
    "type": "game",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {
    "value": "remaining"
  }
}
```

*Success Response:*
```json
{
  "header": {
    "type": "info",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Skips remaining: 1"
  }
}
```
### 5. Quit
*Client Request:*
```json
{
  "header": {
    "type": "quit",
    "playerName": "Bob",
    "ok": true
  },
  "payload": {}
}
```

*Success Response:*
```json
{
  "header": {
    "type": "quit",
    "ok": true
  },
  "payload": {
    "value": "[MoviePixel Inc]: Quitting game. Goodbye!",
    "imageBase64": "<Base64 string of quit.png>",
    "imageName": "quit.png"
  }
}
```
*Error Responses (General):*
When the server receives a request thats unknown from the protocol it responds with:
```json
{
  "header": {
    "type": "error",
    "ok": false
  },
  "payload": {
    "value": "[MoviePixel Inc]: Unknown request type."
  }
}
```
### 6. Game Win
When the game is won, the server responds with the following:
```json
{
  "header": {
    "type": "gameWin",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Congratulations! You've guessed all images correctly. You win!",
    "points": 70,
    "imageBase64": "<Base64 string of win image>",
    "imageName": "win.jpg"
  }
}
```
### 7. Game Over
When the time has run out for the client, the server responds with the following:
```json
{
  "header": {
    "type": "gameOver",
    "ok": true,
    "playerName": "Bob"
  },
  "payload": {
    "value": "[MoviePixel Inc]: Your time ran out. Game over! Final score: 50. Absolute Highest Score: 60",
    "points": 50,
    "imageBase64": "<Base64 string of lose image>",
    "imageName": "lose.jpg"
  }
}
```


## Leaderboard Information ##
During the game, there is a notion of "absolute highest score", this is just the sum of the points
for that player across all games they have played, but being the highest number. This is not the same
as the "leaderboard highest score" which is the score displayed on the leaderboard. The formula for the
leaderboard score is the amount of correct guesses / game duration * 100 (As per the suggested approach in the requirements). The leaderboard class assumes that a correct guess is equal to 10 points, because that is what we assign for the points
during the round when a correct guess is made.


## ScreenCast Link ##
LINK


## Robustness Information ##
1. Server handles all game flow logic and information parsing.
The client does not know or handle any of the logic or information of that round that they are playing,
but constintley sends requests for this information to the server.

2. The server handles all errors and returns appropriate messages to the client if something goes wrong. Neither the client side or server side crash because of this.

3. The protocol is designed to seperate the header and payload into seperate objects as well as have indivudal types
of requests (denoted in the "type") attribute of the headers for ease of computation on the server side.

4. The only thing the client handles on their side is the state in which they are in, which it updates based on the server responses it retrieves from its requests.

5. The leaderboard is persistent via an XML file that is refreshed, written to and read during leaderboard operations. If the server crashes, as long as the XML file exists in the working directory, the leaderboard will never be lost. Future iterations would involve implementing this in a RDS on AWS.


## Adapting UDP ##
UDP does not guarantee delivery of packets, meaning if we switched to UDP, we would have to
change the logic of transmitting the data to the client and verifying that all the pieces arrived, using various
methods within the metadata specifically the headers. Large messages like the base64 encoding of images would need
to be sent in chunks rather that in one response. UDP is also connectionless, meaning our JSON protocol would
need to be adjusted to include identifiers of previous responses and states of both the client and the server. Also, 
because of all of this, we would need much more detailed error handling, because of the leakages or issues we would run into when using a more unreliable transport layer protocol like UDP.

