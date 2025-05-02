package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.SortRequest;
import service.SortResponse;
import service.Algo;
import service.SortGrpc;
import java.util.Arrays;

public class SortImpl extends SortGrpc.SortImplBase {
  @Override
  public void sort(SortRequest request, StreamObserver<SortResponse> responseObserver) {
    int[] arr = request.getDataList().stream().mapToInt(i -> i).toArray();
    Arrays.sort(arr);
    
    SortResponse.Builder response = SortResponse.newBuilder().setIsSuccess(true);
    for (int v : arr) {
      response.addData(v);
    }

    responseObserver.onNext(response.build());
    responseObserver.onCompleted();
  }
}
