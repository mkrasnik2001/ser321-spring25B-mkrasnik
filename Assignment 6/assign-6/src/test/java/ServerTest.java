import com.google.protobuf.Empty;
import example.grpcclient.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import service.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class ServerTest {

    ManagedChannel channel;
    private EchoGrpc.EchoBlockingStub blockingStub;
    private JokeGrpc.JokeBlockingStub blockingStub2;
    private SortGrpc.SortBlockingStub sortStub;
    private FitnessGrpc.FitnessBlockingStub fitnessStub;
    private QuoteGrpc.QuoteBlockingStub quoteStub;


    @org.junit.Before
    public void setUp() throws Exception {
        // assuming default port and localhost for our testing, make sure Node runs on this port
        channel = ManagedChannelBuilder.forTarget("localhost:8000").usePlaintext().build();

        blockingStub = EchoGrpc.newBlockingStub(channel);
        blockingStub2 = JokeGrpc.newBlockingStub(channel);
        sortStub    = SortGrpc.newBlockingStub(channel);
        fitnessStub = FitnessGrpc.newBlockingStub(channel);
        quoteStub = QuoteGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);

    }


    @Test
    public void parrot() {
        // success case
        ClientRequest request = ClientRequest.newBuilder().setMessage("test").build();
        ServerResponse response = blockingStub.parrot(request);
        assertTrue(response.getIsSuccess());
        assertEquals("test", response.getMessage());

        // error cases
        request = ClientRequest.newBuilder().build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());

        request = ClientRequest.newBuilder().setMessage("").build();
        response = blockingStub.parrot(request);
        assertFalse(response.getIsSuccess());
        assertEquals("No message provided", response.getError());
    }

    // For this test the server needs to be started fresh AND the list of jokes needs to be the initial list
    @Test
    public void joke() {
        // getting first joke
        JokeReq request = JokeReq.newBuilder().setNumber(1).build();
        JokeRes response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("Did you hear the rumor about butter? Well, I'm not going to spread it!", response.getJoke(0));

        // getting next 2 jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("What do you call someone with no body and no nose? Nobody knows.", response.getJoke(0));
        assertEquals("I don't trust stairs. They're always up to something.", response.getJoke(1));

        // getting 2 more but only one more on server
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(2, response.getJokeCount());
        assertEquals("How do you get a squirrel to like you? Act like a nut.", response.getJoke(0));
        assertEquals("I am out of jokes...", response.getJoke(1));

        // trying to get more jokes but out of jokes
        request = JokeReq.newBuilder().setNumber(2).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("I am out of jokes...", response.getJoke(0));

        // trying to add joke without joke field
        JokeSetReq req2 = JokeSetReq.newBuilder().build();
        JokeSetRes res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // trying to add empty joke
        req2 = JokeSetReq.newBuilder().setJoke("").build();
        res2 = blockingStub2.setJoke(req2);
        assertFalse(res2.getOk());

        // adding a new joke (well word)
        req2 = JokeSetReq.newBuilder().setJoke("whoop").build();
        res2 = blockingStub2.setJoke(req2);
        assertTrue(res2.getOk());

        // should have the new "joke" now and return it
        request = JokeReq.newBuilder().setNumber(1).build();
        response = blockingStub2.getJoke(request);
        assertEquals(1, response.getJokeCount());
        assertEquals("whoop", response.getJoke(0));
    }

    @Test
    public void sort() {
        // sort 3,1,2,5,4
        SortRequest request = SortRequest.newBuilder()
            .setAlgo(Algo.INTERN)
            .addAllData(Arrays.asList(3, 1, 2, 5, 4))
            .build();
        SortResponse response = sortStub.sort(request);
        assertTrue(response.getIsSuccess());
        assertEquals(Arrays.asList(1,2,3,4,5), response.getDataList());
        // check with empty request
        SortRequest emptyRequest = SortRequest.newBuilder()
            .setAlgo(Algo.INTERN)
            .build();
        SortResponse emptyResponse = sortStub.sort(emptyRequest);
        assertTrue(emptyResponse.getIsSuccess());
        assertTrue(emptyResponse.getDataList().isEmpty());
    }

    @Test
    public void fitness() {
        // requesting before any exercises added
        GetRequest getReq = GetRequest.newBuilder()
            .setExerciseType(ExerciseType.BALANCE)
            .build();
        GetResponse getResp = fitnessStub.getExercise(getReq);
        assertFalse(getResp.getIsSuccess());
        assertTrue(getResp.getError().contains("No exercises available"));

        // adding valid exercise
        Exercise ex = Exercise.newBuilder()
            .setDescription("Test Exercise")
            .setExerciseType(ExerciseType.CARDIO)
            .build();
        AddRequest addReq = AddRequest.newBuilder()
            .setExercise(ex)
            .build();
        AddResponse addResp = fitnessStub.addExercise(addReq);
        assertTrue(addResp.getIsSuccess());

        // retrieving added exercise
        getReq = GetRequest.newBuilder()
            .setExerciseType(ExerciseType.CARDIO)
            .build();
        getResp = fitnessStub.getExercise(getReq);
        assertTrue(getResp.getIsSuccess());
        assertEquals("Test Exercise", getResp.getExercise().getDescription());
        assertEquals(ExerciseType.CARDIO, getResp.getExercise().getExerciseType());

        // adding invalid exercise
        ex = Exercise.newBuilder()
            .setDescription("")
            .setExerciseType(ExerciseType.STRENGTH)
            .build();
        addReq = AddRequest.newBuilder()
            .setExercise(ex)
            .build();
        addResp = fitnessStub.addExercise(addReq);
        assertFalse(addResp.getIsSuccess());
        assertEquals("Description cannot be empty", addResp.getError());
    }

    @Test
    public void quoteService() {
        // add a quote successfully
        AddQuoteRequest addReq1 = AddQuoteRequest.newBuilder()
            .setText("To be or not to be")
            .build();
        AddQuoteResponse addRes1 = quoteStub.addQuote(addReq1);
        assertTrue(addRes1.getSuccess());
        int id1 = addRes1.getId();

        // add a quote failure (empty text)
        AddQuoteRequest addReq2 = AddQuoteRequest.newBuilder().setText("").build();
        AddQuoteResponse addRes2 = quoteStub.addQuote(addReq2);
        assertFalse(addRes2.getSuccess());
        assertEquals("The Quote cannot be empty", addRes2.getError());

        // fetch the first quote
        GetQuoteRequest getReq1 = GetQuoteRequest.newBuilder().setId(id1).build();
        GetQuoteResponse getRes1 = quoteStub.getQuote(getReq1);
        assertTrue(getRes1.getSuccess());
        assertEquals("To be or not to be", getRes1.getText());

        // fetch a quote that does not exist
        GetQuoteRequest getReq2 = GetQuoteRequest.newBuilder().setId(999).build();
        GetQuoteResponse getRes2 = quoteStub.getQuote(getReq2);
        assertFalse(getRes2.getSuccess());
        assertEquals("No quote with ID 999 found...", getRes2.getError());

        // add another quote and verify listing
        AddQuoteRequest addReq3 = AddQuoteRequest.newBuilder()
            .setText("Second quote")
            .build();
        AddQuoteResponse addRes3 = quoteStub.addQuote(addReq3);
        assertTrue(addRes3.getSuccess());
        int id2 = addRes3.getId();

        ListQuotesResponse listRes = quoteStub.listQuotes(Empty.newBuilder().build());
        assertEquals(2, listRes.getQuotesCount());

        boolean found1 = false, found2 = false;
        for (QuoteEntry entry : listRes.getQuotesList()) {
            if (entry.getId() == id1 && entry.getText().equals("To be or not to be")) {
                found1 = true;
            }
            if (entry.getId() == id2 && entry.getText().equals("Second quote")) {
                found2 = true;
            }
        }
        assertTrue(found1 && found2);
    }
}
