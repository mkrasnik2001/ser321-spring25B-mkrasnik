package example.grpcclient;

import io.grpc.stub.StreamObserver;
import com.google.protobuf.Empty;
import service.QuoteGrpc;
import service.AddQuoteRequest;
import service.AddQuoteResponse;
import service.GetQuoteRequest;
import service.GetQuoteResponse;
import service.ListQuotesResponse;
import service.QuoteEntry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class QuoteImpl extends QuoteGrpc.QuoteImplBase {
    private final Map<Integer, String> store = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public void addQuote(AddQuoteRequest request, StreamObserver<AddQuoteResponse> responseObserver) {
        AddQuoteResponse.Builder response = AddQuoteResponse.newBuilder();
        String text = request.getText().trim();
        if (text.isEmpty()) {
            response.setSuccess(false)
                    .setError("The Quote cannot be empty");
        } else {
            int id = nextId.getAndIncrement();
            store.put(id, text);
            response.setSuccess(true)
                    .setId(id);
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getQuote(GetQuoteRequest request, StreamObserver<GetQuoteResponse> responseObserver) {
        GetQuoteResponse.Builder response = GetQuoteResponse.newBuilder();
        String text = store.get(request.getId());
        if (text == null) {
            response.setSuccess(false)
                    .setError("No quote with ID " + request.getId() + "found...");
        } else {
            response.setSuccess(true)
                    .setText(text);
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listQuotes(Empty ignored, StreamObserver<ListQuotesResponse> responseObserver) {
        ListQuotesResponse.Builder response = ListQuotesResponse.newBuilder();
        for (Map.Entry<Integer, String> entry : store.entrySet()) {
            response.addQuotes(QuoteEntry.newBuilder()
                                        .setId(entry.getKey())
                                        .setText(entry.getValue())
                                        .build());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
