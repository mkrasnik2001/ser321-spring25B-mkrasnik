package server;

import buffers.ResponseProtos.Response;

/**
 * Helper class to map the response eval type to the provided code int val
 */
public final class EvalTypeMapper {

    private EvalTypeMapper() {}

    public static Response.EvalType mapFromCode(int code) {
        return switch (code) {
            case 0 -> Response.EvalType.UPDATE;
            case 1 -> Response.EvalType.PRESET_VALUE;
            case 2 -> Response.EvalType.DUP_ROW;
            case 3 -> Response.EvalType.DUP_COL;
            case 4 -> Response.EvalType.DUP_GRID;
            default -> Response.EvalType.UPDATE;
        };
    }

    public static Response.EvalType mapFromVal(int value) {
        return switch (value) {
            case 1 -> Response.EvalType.CLEAR_VALUE;
            case 2 -> Response.EvalType.CLEAR_ROW;
            case 3 -> Response.EvalType.CLEAR_COL;
            case 4 -> Response.EvalType.CLEAR_GRID;
            case 5 -> Response.EvalType.CLEAR_BOARD;
            case 6 -> Response.EvalType.RESET_BOARD;
            default -> Response.EvalType.CLEAR_VALUE;
        };
    }
}
