package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.FitnessGrpc;
import service.AddRequest;
import service.AddResponse;
import service.GetRequest;
import service.GetResponse;
import service.Exercise;
import service.ExerciseType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FitnessImpl extends FitnessGrpc.FitnessImplBase {
    private final Map<ExerciseType, List<Exercise>> store = new EnumMap<>(ExerciseType.class);
    private final Random rand = new Random();

    public FitnessImpl() {
        ExerciseType[] types = ExerciseType.values();
        for (int i = 0; i < types.length; i++) {
            store.put(types[i], new ArrayList<>());
        }
    }

    @Override
    public void addExercise(AddRequest request, StreamObserver<AddResponse> responseObserver) {
        Exercise ex = request.getExercise();
        AddResponse.Builder response = AddResponse.newBuilder();

        if (ex.getDescription().isBlank()) {
            response.setIsSuccess(false)
                    .setError("Description cannot be empty");
        } else {
            store.get(ex.getExerciseType()).add(ex);
            response.setIsSuccess(true);
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getExercise(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        ExerciseType type = request.getExerciseType();
        List<Exercise> list = store.get(type);
        GetResponse.Builder response = GetResponse.newBuilder();

        if (list.isEmpty()) {
            response.setIsSuccess(false)
                    .setError("No exercises available for type: " + type);
        } else {
            Exercise pick = list.get(rand.nextInt(list.size()));
            response.setIsSuccess(true)
                    .setExercise(pick);
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
