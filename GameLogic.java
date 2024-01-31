import java.util.*;

public class GameLogic implements PlayableLogic {
    private static final int BOARD_SIZE = 11;
    private static final Position DIR_UP = new Position(0, 1);
    private static final Position DIR_DOWN = new Position(0, -1);
    private static final Position DIR_LEFT = new Position(1, 0);
    private static final Position DIR_RIGHT = new Position(-1, 0);
    private final ArrayList<ConcretePiece> pieces;
    private final ConcretePlayer player1, player2;
    private boolean player2Move, isGameFinished;
    private final Stack<Move> player1Moves;
    private final Stack<Move> player2Moves;

    // HashSet will provide built-in uniqueness of items
    private final HashMap<Position, HashSet<ConcretePiece>> piecesPerSquare;

    public GameLogic() {
        this.pieces = new ArrayList<>();
        this.player1 = new ConcretePlayer(true);
        this.player2 = new ConcretePlayer(false);
        this.player1Moves = new Stack<>();
        this.player2Moves = new Stack<>();
        this.piecesPerSquare = new HashMap<>();

        this.reset();
    }

    private ConcretePiece attack(ConcretePiece attacker, Position direction) {
        // King cannot attack
        if (attacker instanceof King) {
            return null;
        }

        // Check if there is a piece occupying the square in the direction being evaluated
        ConcretePiece neighbor = this.getPieceAtPosition(new Position(attacker.getPosition().x() + direction.x(), attacker.getPosition().y() + direction.y()));
        if (neighbor == null) {
            return null;
        }

        // Check if neighbor is an enemy
        boolean enemies = !attacker.isAlly(neighbor);
        if (!enemies) {
            return null;
        }

        // Calculate position of square across from attacker
        Position eatBuddyPosition = new Position(attacker.getPosition().x() + (2 * direction.x()), attacker.getPosition().y() + (2 * direction.y()));

        // Get piece across from attacker which will assist in the kill
        ConcretePiece eatBuddy = this.getPieceAtPosition(eatBuddyPosition);

        if (neighbor instanceof King) {

            // Neighbor is king, attack him
            if (this.attackKing(attacker, eatBuddyPosition, eatBuddy, direction)) {
                return neighbor;
            }
        } else {

            // Try to kill enemy using corner, edge or eat buddy
            if (this.isCorner(eatBuddyPosition) || this.notInBoard(eatBuddyPosition) || (eatBuddy != null && !(eatBuddy instanceof King) && attacker.isAlly(eatBuddy))) {
                this.pieces.remove(neighbor);
                return neighbor;
            }
        }

        // Better luck next time
        return null;
    }

    private boolean attackKing(ConcretePiece attacker, Position eatBuddyPosition, ConcretePiece eatBuddy, Position direction) {
        // Get 2 diagonal pieces, if they exist
        Position diagonal1Pos = null, diagonal2Pos = null;
        if (direction.x() != 0) {
            diagonal1Pos = new Position(attacker.getPosition().x() + direction.x(), attacker.getPosition().y() + 1);
            diagonal2Pos = new Position(attacker.getPosition().x() + direction.x(), attacker.getPosition().y() - 1);
        }
        if (direction.y() != 0) {
            diagonal1Pos = new Position(attacker.getPosition().x() + 1, attacker.getPosition().y() + direction.y());
            diagonal2Pos = new Position(attacker.getPosition().x() - 1, attacker.getPosition().y() + direction.y());
        }
        ConcretePiece diagonal1 = this.getPieceAtPosition(diagonal1Pos), diagonal2 = this.getPieceAtPosition(diagonal2Pos);

        // For each of the 3 surrounding buddies, check if allies (will also handle literal "edge" cases)
        return (this.notInBoard(eatBuddyPosition) || attacker.isAlly(eatBuddy)) && (this.notInBoard(diagonal1Pos) || attacker.isAlly(diagonal1)) && (this.notInBoard(diagonal2Pos) || attacker.isAlly(diagonal2));
    }

    private boolean isCorner(Position pos) {
        return (pos.x() == this.getBoardSize() - 1 && pos.y() == this.getBoardSize() - 1) || (pos.x() == 0 && pos.y() == 0) || (pos.x() == 0 && pos.y() == this.getBoardSize() - 1) || (pos.x() == this.getBoardSize() - 1 && pos.y() == 0);
    }

    private void printMoveHistory(Stack<Move> moves) {
        // Create map between piece -> moves
        HashMap<ConcretePiece, ArrayList<Move>> movesPerPiece = new HashMap<>();
        for (Move move : moves) {
            movesPerPiece.computeIfAbsent(move.target(), k -> new ArrayList<>()).add(move);
        }

        movesPerPiece.entrySet().stream()
                // Sort map values by move list length and keys by ID in ascending order
                .sorted(Comparator.comparingInt((Map.Entry<ConcretePiece, ArrayList<Move>> o) -> o.getValue().size()).thenComparingInt(o -> o.getKey().getId()))
                // Print data
                .forEach(entry -> {
                    System.out.print(entry.getKey() + ": [");
                    ArrayList<Move> pieceMoves = entry.getValue();
                    for (Move move : pieceMoves) {
                        System.out.print(move.oldPosition());
                        System.out.print(", ");
                    }
                    System.out.print(entry.getKey().getPosition());
                    System.out.print("]\n");
                });
    }

    private void printKills() {
        this.pieces.stream()
                // Sort by kills
                .sorted(Comparator.comparingInt(ConcretePiece::getKills)
                        // Sort by ID in ascending order
                        .thenComparingInt(ConcretePiece::getId))
                // Remove zeroes
                .filter(piece -> piece.getKills() != 0)
                // Print data
                .forEach(piece -> System.out.println(piece + ": " + piece.getKills() + " kills"));
    }

    private void printDistances() {
        this.pieces.stream()
                // Sort by distance travelled
                .sorted(Comparator.comparingInt(ConcretePiece::distanceTravelled)
                        // Descending order
                        .reversed()
                        // Sort by ID in ascending order
                        .thenComparingInt(ConcretePiece::getId)).filter(piece -> piece.distanceTravelled() != 0)
                // Print
                .forEach(piece -> System.out.println(piece + ": " + piece.distanceTravelled() + " squares"));
    }

    private void printStepsPerSquare() {
        this.piecesPerSquare.entrySet().stream()
                // 2 and above
                .filter(entry -> entry.getValue().size() >= 2)
                // Sort by number of unique pieces
                .sorted(Comparator.comparingInt((Map.Entry<Position, HashSet<ConcretePiece>> o) -> o.getValue().size())
                        // In descending order
                        .reversed()
                        // Sort by x value
                        .thenComparingInt(o -> o.getKey().x())
                        // Sort by y value
                        .thenComparingInt(o -> o.getKey().y()))
                // Print
                .forEach(entry -> System.out.println(entry.getKey().toString() + entry.getValue().size() + " pieces"));
    }

    private void printStars() {
        for (int i = 1; i <= 75; i++) {
            System.out.print("*");
        }
        System.out.println();
    }

    private void winState() {
        this.isGameFinished = true;

        if (this.isSecondPlayerTurn()) {
            this.getSecondPlayer().win();
            this.printMoveHistory(this.player2Moves);
            this.printMoveHistory(this.player1Moves);
        } else {
            this.getFirstPlayer().win();
            this.printMoveHistory(this.player1Moves);
            this.printMoveHistory(this.player2Moves);
        }
        this.printStars();

        this.printKills();
        this.printStars();

        this.printDistances();
        this.printStars();

        this.printStepsPerSquare();
        this.printStars();
    }

    private boolean notInBoard(Position position) {
        if (position == null) {
            return true;
        }
        return position.x() < 0 || position.y() < 0 || position.x() >= this.getBoardSize() || position.y() >= this.getBoardSize();
    }

    @Override
    public boolean move(Position a, Position b) {
        // Check that a and b are on the board
        if (this.notInBoard(a) || this.notInBoard(b)) {
            return false;
        }

        // Check that a and b are different coords and that they're not diagonal to each other
        if (a.equals(b) || (a.x() != b.x() && a.y() != b.y())) {
            return false;
        }

        // Check if a is not empty
        ConcretePiece target = this.getPieceAtPosition(a);
        if (target == null) {
            return false;
        }

        // Check whose turn it is
        if ((this.player2Move && target.getOwner() != this.getSecondPlayer()) || (!this.player2Move && target.getOwner() != this.getFirstPlayer())) {
            return false;
        }

        // Check if path between a and b is empty
        if (a.x() == b.x()) {
            Position direction = a.y() < b.y() ? DIR_UP : DIR_DOWN;
            for (int y = a.y() + direction.y(); y != b.y() + direction.y(); y += direction.y()) {
                if (this.getPieceAtPosition(new Position(a.x(), y)) != null) {
                    return false;
                }
            }
        } else {
            Position direction = a.x() < b.x() ? DIR_LEFT : DIR_RIGHT;
            for (int x = a.x() + direction.x(); x != b.x() + direction.x(); x += direction.x()) {
                if (this.getPieceAtPosition(new Position(x, a.y())) != null) {
                    return false;
                }
            }
        }

        // Handle corners
        if (this.isCorner(b)) {
            if (target instanceof Pawn) {
                return false;
            } else if (target instanceof King) {
                target.setPosition(b);
                this.winState();

                return true;
            }
        }

        // If all checks passed, make the move
        Position oldPosition = target.getPosition();
        ArrayList<ConcretePiece> victims = new ArrayList<>();
        target.setPosition(b);

        // Kill if needed
        victims.add(this.attack(target, DIR_UP));
        victims.add(this.attack(target, DIR_DOWN));
        victims.add(this.attack(target, DIR_LEFT));
        victims.add(this.attack(target, DIR_RIGHT));

        // Remove null values
        victims.removeAll(Collections.singleton(null));

        // Check if king was cannibalized
        // Defer triggering win state until later so current move is completely processed
        boolean winState = false;
        for (ConcretePiece victim : victims) {
            if (victim instanceof King) {
                winState = true;
                break;
            }
        }

        Move move = new Move(target, oldPosition, victims);

        // Store move and check if attacker has been defeated
        target.pushPosition(b);
        this.piecesPerSquare.computeIfAbsent(b, k -> new HashSet<>()).add(target);
        if (this.isSecondPlayerTurn()) {
            this.player2Moves.push(move);
        } else {
            this.player1Moves.push(move);

            // Check if defender killed all attackers
            boolean attackerFound = false;
            for (ConcretePiece piece : this.pieces) {
                if (piece.getOwner() == this.getSecondPlayer()) {
                    attackerFound = true;
                    break;
                }
            }

            if (!attackerFound) {
                this.winState();
            }
        }

        if (winState) {
            // Trigger win state
            this.winState();
        } else {
            // Register kills
            target.addKills(victims.size());

            // End turn
            this.player2Move = !this.player2Move;
        }

        return true;
    }

    @Override
    public ConcretePiece getPieceAtPosition(Position position) {
        ConcretePiece found = null;
        for (ConcretePiece piece : this.pieces) {
            if (piece.getPosition().equals(position)) {
                if (found != null) {
                    System.out.println("Two pieces in the same position " + position);
                }

                found = piece;
            }
        }

        return found;
    }

    @Override
    public ConcretePlayer getFirstPlayer() {
        return this.player1;
    }

    @Override
    public ConcretePlayer getSecondPlayer() {
        return this.player2;
    }

    @Override
    public boolean isGameFinished() {
        return this.isGameFinished;
    }

    @Override
    public boolean isSecondPlayerTurn() {
        return this.player2Move;
    }

    @Override
    public void reset() {
        this.isGameFinished = false;
        this.player2Move = true;

        this.pieces.clear();
        this.player1Moves.clear();
        this.player2Moves.clear();

        this.pieces.add(new Pawn(3, 0, this.player2, 1));
        this.pieces.add(new Pawn(4, 0, this.player2, 2));
        this.pieces.add(new Pawn(5, 0, this.player2, 3));
        this.pieces.add(new Pawn(6, 0, this.player2, 4));
        this.pieces.add(new Pawn(7, 0, this.player2, 5));
        this.pieces.add(new Pawn(5, 1, this.player2, 6));
        this.pieces.add(new Pawn(0, 3, this.player2, 7));
        this.pieces.add(new Pawn(10, 3, this.player2, 8));
        this.pieces.add(new Pawn(0, 4, this.player2, 9));
        this.pieces.add(new Pawn(10, 4, this.player2, 10));
        this.pieces.add(new Pawn(0, 5, this.player2, 11));
        this.pieces.add(new Pawn(1, 5, this.player2, 12));
        this.pieces.add(new Pawn(9, 5, this.player2, 13));
        this.pieces.add(new Pawn(10, 5, this.player2, 14));
        this.pieces.add(new Pawn(0, 6, this.player2, 15));
        this.pieces.add(new Pawn(10, 6, this.player2, 16));
        this.pieces.add(new Pawn(0, 7, this.player2, 17));
        this.pieces.add(new Pawn(10, 7, this.player2, 18));
        this.pieces.add(new Pawn(5, 9, this.player2, 19));
        this.pieces.add(new Pawn(3, 10, this.player2, 20));
        this.pieces.add(new Pawn(4, 10, this.player2, 21));
        this.pieces.add(new Pawn(5, 10, this.player2, 22));
        this.pieces.add(new Pawn(6, 10, this.player2, 23));
        this.pieces.add(new Pawn(7, 10, this.player2, 24));

        this.pieces.add(new Pawn(5, 3, this.player1, 1));
        this.pieces.add(new Pawn(4, 4, this.player1, 2));
        this.pieces.add(new Pawn(5, 4, this.player1, 3));
        this.pieces.add(new Pawn(6, 4, this.player1, 4));
        this.pieces.add(new Pawn(3, 5, this.player1, 5));
        this.pieces.add(new Pawn(4, 5, this.player1, 6));
        this.pieces.add(new King(5, 5, this.player1, 7));
        this.pieces.add(new Pawn(6, 5, this.player1, 8));
        this.pieces.add(new Pawn(7, 5, this.player1, 9));
        this.pieces.add(new Pawn(4, 6, this.player1, 10));
        this.pieces.add(new Pawn(5, 6, this.player1, 11));
        this.pieces.add(new Pawn(6, 6, this.player1, 12));
        this.pieces.add(new Pawn(5, 7, this.player1, 13));

        this.piecesPerSquare.clear();
        for (ConcretePiece piece : this.pieces) {
            piece.reset();
            this.piecesPerSquare.computeIfAbsent(piece.getPosition(), k -> new HashSet<>()).add(piece);
        }
    }

    @Override
    public void undoLastMove() {
        Stack<Move> moves = this.player2Moves;

        // Need to undo previous turn so choose other player's stack
        if (this.player2Move) {
            moves = this.player1Moves;
        }

        if (moves.isEmpty()) {
            return;
        }

        Move lastMove = moves.pop();
        this.piecesPerSquare.get(lastMove.target().getPosition()).remove(lastMove.target());
        this.piecesPerSquare.get(lastMove.oldPosition()).add(lastMove.target());
        lastMove.target().popPosition();
        lastMove.target().setPosition(lastMove.oldPosition());
        this.pieces.addAll(lastMove.victims());

        this.player2Move = !this.player2Move;
    }

    @Override
    public int getBoardSize() {
        return GameLogic.BOARD_SIZE;
    }
}
