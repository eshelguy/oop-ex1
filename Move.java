import java.util.ArrayList;

public record Move(ConcretePiece target, Position oldPosition, ArrayList<ConcretePiece> victims) {}
