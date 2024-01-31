public class Pawn extends ConcretePiece {

    public Pawn(int initialX, int initialY, ConcretePlayer owner, int id) {
        super(initialX, initialY, owner, id);
    }

    @Override
    public String getType() {
        return this.getOwner().isPlayerOne() ? "♙" : "♟";
    }
}
