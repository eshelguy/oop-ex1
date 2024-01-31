import java.util.Stack;

public abstract class ConcretePiece implements Piece, Comparable<ConcretePiece> {
    private Position position;
    private final ConcretePlayer owner;
    private final int id;
    private int kills;
    private final Stack<Position> positionHistory;

    public ConcretePiece(Position initialPosition, ConcretePlayer owner, int id) {
        this.position = initialPosition;
        this.owner = owner;
        this.id = id;
        this.positionHistory = new Stack<>();

        this.reset();
    }

    public ConcretePiece(int initialX, int initialY, ConcretePlayer owner, int id) {
        this(new Position(initialX, initialY), owner, id);
    }

    @Override
    public ConcretePlayer getOwner() {
        return this.owner;
    }

    public boolean isAlly(ConcretePiece ally) {
        return ally != null && this.getOwner() == ally.getOwner();
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public int getId() {
        return this.id;
    }
    public void addKills(int kills) {
        this.kills += kills;
    }

    public int getKills() {
        return kills;
    }
    public void pushPosition(Position newPosition) {
        this.positionHistory.push(newPosition);
    }
    public void popPosition() {
        this.positionHistory.pop();
    }
    public int distanceTravelled() {
        if (this.positionHistory.size() <= 1) {
            return 0;
        }

        int distance = 0;
        for (int i = 0; i < this.positionHistory.size() - 1; i++) {
            distance += this.positionHistory.get(i).distance(this.positionHistory.get(i + 1));
        }

        return distance;
    }
    public void reset() {
        this.kills = 0;
        this.positionHistory.clear();
        this.pushPosition(this.position);
    }
    public String toString() {
        return (this.owner.isPlayerOne() ? "D" : "A") + this.getId();
    }

    public int compareTo(ConcretePiece other) {
        return Integer.compare(this.getId(), other.getId());
    }
}
