public record Position(int x, int y) {
    public int distance(Position other) {
        return Math.abs(this.x() - other.x()) + Math.abs(this.y() - other.y());
    }

    public String toString() {
        return "(" + this.x() + ", " + this.y() + ")";
    }
}