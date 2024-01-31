public class King extends ConcretePiece {

    public King(int initialX, int initialY, ConcretePlayer owner, int id) {
        super(initialX, initialY, owner, id);
    }

    @Override
    public String getType() {
        return "♔";
    }

    public String toString() {
        return "K" + this.getId();
    }
}
