package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Client that requests `parrot` method from the `EchoServer`.
 */
public class Client {
  private final EchoGrpc.EchoBlockingStub blockingStub;
  private final JokeGrpc.JokeBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final SortGrpc.SortBlockingStub sortStub;
  private final FitnessGrpc.FitnessBlockingStub fitnessStub;
  private final QuoteGrpc.QuoteBlockingStub quoteStub;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    sortStub = SortGrpc.newBlockingStub(channel);
    fitnessStub = FitnessGrpc.newBlockingStub(channel);
    quoteStub = QuoteGrpc.newBlockingStub(channel);


  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = EchoGrpc.newBlockingStub(channel);
    blockingStub2 = JokeGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
    sortStub = SortGrpc.newBlockingStub(channel);
    fitnessStub = FitnessGrpc.newBlockingStub(channel);
    quoteStub = QuoteGrpc.newBlockingStub(channel);
    

  }

  public void askServerToParrot(String message) {

    ClientRequest request = ClientRequest.newBuilder().setMessage(message).build();
    ServerResponse response;
    try {
      response = blockingStub.parrot(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e.getMessage());
      return;
    }
    System.out.println("Received from server: " + response.getMessage());
  }

  public void askForJokes(int num) {
    JokeReq request = JokeReq.newBuilder().setNumber(num).build();
    JokeRes response;

    // just to show how to use the empty in the protobuf protocol
    Empty empt = Empty.newBuilder().build();

    try {
      response = blockingStub2.getJoke(request);
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
    System.out.println("Your jokes: ");
    for (String joke : response.getJokeList()) {
      System.out.println("--- " + joke);
    }
  }

  public void setJoke(String joke) {
    JokeSetReq request = JokeSetReq.newBuilder().setJoke(joke).build();
    JokeSetRes response;

    try {
      response = blockingStub2.setJoke(request);
      System.out.println(response.getOk());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  // sort
  public void sortArray(List<Integer> arr){
    SortRequest.Builder request = SortRequest.newBuilder().setAlgo(Algo.INTERN);
    arr.forEach(request::addData);

    SortResponse response;
    try {
      response = sortStub.sort(request.build());
    } catch (Exception ex){
      System.err.println("RPC failed: " + ex);
      return;
    }
    System.out.println("Result Sorted: " + response.getDataList());
  }

  // fitness service
  public void addExercise(String description, ExerciseType type) {
    Exercise exercise = Exercise.newBuilder()
        .setDescription(description)
        .setExerciseType(type)
        .build();

    AddRequest request = AddRequest.newBuilder()
        .setExercise(exercise)
        .build();

    try {
        AddResponse response = fitnessStub.addExercise(request);
        if (response.getIsSuccess()) {
            System.out.println("Exercise added!");
        } else {
            System.out.println("Something went wrong while adding exercise: " + response.getError());
        }
    } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
    }
  }

  public void getExercise(ExerciseType type) {
    GetRequest request = GetRequest.newBuilder()
        .setExerciseType(type)
        .build();

    try {
        GetResponse response = fitnessStub.getExercise(request);
        if (response.getIsSuccess()) {
            Exercise ex = response.getExercise();
            System.out.println("Got exercise: " + ex.getDescription() +
                              " (" + ex.getExerciseType() + ")");
        } else {
            System.out.println("No exercise available: " + response.getError());
        }
    } catch (Exception e) {
        System.err.println("RPC failed: " + e.getMessage());
    }
  }


  // quote service
  public void addQuote(String text) {
    var req = AddQuoteRequest.newBuilder().setText(text).build();
    var res = quoteStub.addQuote(req);
    if (res.getSuccess()) System.out.println("Added id=" + res.getId());
    else                 System.out.println("Something went wrong: " + res.getError());
  }

  public void getQuote(int id) {
    var req = GetQuoteRequest.newBuilder().setId(id).build();
    var res = quoteStub.getQuote(req);
    if (res.getSuccess()) System.out.println("Quote: " + res.getText());
    else                 System.out.println("Something went wrong: " + res.getError());
  }

  public void listQuotes() {
    var res = quoteStub.listQuotes(Empty.newBuilder().build());
    for (QuoteEntry e : res.getQuotesList()) {
      System.out.printf("%d: %s%n", e.getId(), e.getText());
    }
  }



  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
        System.out.println("Expected: <host> <port> <regHost> <regPort> <message> <regOn>");
        System.exit(1);
    }
    String host = args[0];
    int port = Integer.parseInt(args[1]);
    String regHost = args[2];
    int regPort = Integer.parseInt(args[3]);
    boolean regOn = Boolean.parseBoolean(args[5]);

    ManagedChannel channel = ManagedChannelBuilder.forTarget(host + ":" + port)
        .usePlaintext().build();
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regHost + ":" + regPort)
        .usePlaintext().build();

    try {
        Client client = new Client(channel, regChannel);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            ServicesListRes listRes = client.blockingStub4.getServices(
                GetServicesReq.newBuilder().build());
            List<String> services = listRes.getServicesList();
            System.out.println("\nAvailable services:");
            for (int i = 0; i < services.size(); i++) {
                System.out.printf("%d) %s%n", i + 1, services.get(i));
            }
            System.out.println("0) Exit");
            System.out.print("> ");
            int choice;
            try {
                choice = Integer.parseInt(reader.readLine());
            } catch (Exception e) {
                continue;
            }
            if (choice == 0) break;
            if (choice < 1 || choice > services.size()) continue;

            String service = services.get(choice - 1);
            if (service.equals("services.Echo/parrot")) {
                System.out.print("Message: ");
                client.askServerToParrot(reader.readLine());
            } else if (service.equals("services.Joke/getJoke")) {
                System.out.print("Count: ");
                client.askForJokes(Integer.parseInt(reader.readLine()));
            } else if (service.equals("services.Joke/setJoke")) {
                System.out.print("Joke: ");
                client.setJoke(reader.readLine());
            } else if (service.equals("services.Sort/sort")) {
              System.out.print("Numbers (space separated): ");
              String line = reader.readLine().trim();
              String[] parts = line.split("\\s+");
              List<Integer> nums = new ArrayList<>();
              boolean allInts = true;
          
              for (String p : parts) {
                  try {
                      nums.add(Integer.parseInt(p));
                  } catch (NumberFormatException e) {
                      System.out.println("Invalid number: '" + p +
                                         "' – please enter only integers separated by spaces.");
                      allInts = false;
                      break;
                  }
              }
          
              if (!allInts) {
                  continue;
              }
          
              client.sortArray(nums);
          } else if (service.equals("services.Fitness/addExercise")) {
                System.out.print("Description: ");
                String desc = reader.readLine();
                System.out.print("Type: ");
                String typeStr = reader.readLine().trim().toUpperCase();
                ExerciseType type;
                try {
                    type = ExerciseType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid type. Use CARDIO, STRENGTH or BALANCE.");
                    continue;
                }
                client.addExercise(desc, type);
            } else if (service.equals("services.Fitness/getExercise")) {
                System.out.print("Type: ");
                String typeStr = reader.readLine().trim().toUpperCase();
                ExerciseType type;
                try {
                    type = ExerciseType.valueOf(typeStr);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid type. Use CARDIO, STRENGTH or BALANCE.");
                    continue;
                }
                client.getExercise(type);
            } else if (service.equals("services.Quote/addQuote")) {
              System.out.print("Text: ");
              client.addQuote(reader.readLine());
            }
            else if (service.equals("services.Quote/getQuote")) {
              System.out.print("ID: ");
              client.getQuote(Integer.parseInt(reader.readLine()));
            }
            else if (service.equals("services.Quote/listQuotes")) {
              client.listQuotes();
            }            
        }
    } finally {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        if (regOn) {
            regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
  }
}
