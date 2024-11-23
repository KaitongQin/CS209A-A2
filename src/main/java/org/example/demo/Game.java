package org.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Game {

    // row length
    int row;

    // col length
    int col;

    // board content
    int[][] board;

    ArrayList<Move> moves;

    public Game (int[][] board) {
        this.board = board;
        this.row = board.length;
        this.col = board[0].length;
    }

    // randomly initialize the game board
    public static int[][] SetupBoard (int row, int col) {
        int totalCells = row * col;
        if (totalCells % 2 == 1) {
            row += 1;
        }
        List<Integer> icons = new ArrayList<>();
        for (int i = 1; i < 12; i++) {
            if (icons.size() == totalCells) {
                break;
            }
            icons.add(i);
            icons.add(i);
            if (icons.size() < totalCells && i == 11) {
                i = 1;
            }
        }
        Collections.shuffle(icons);
        int count = 0;
        int[][] board = new int[row+2][col+2];
        for (int i = 1; i <= row; i++) {
            for (int j = 1; j <= col; j++) {
                board[i][j] = icons.get(count);
                count++;
            }
        }
        return board;
    }

    // judge the validity of an operation
    public boolean judge (int row1, int col1, int row2, int col2) {
        moves = new ArrayList<>();
        if ((board[row1][col1] != board[row2][col2]) || (row1 == row2 && col1 == col2) || (board[row1][col1] == 0 && board[row2][col2] == 0)) {
            return false;
        }

        // one line
        if (isDirectlyConnected(row1, col1, row2, col2, board)) {
            moves.add(new Move(row1, col1, row2, col2));
            return true;
        }

        // two lines
        if ((row1 != row2) && (col1 != col2)) {
            if(board[row1][col2] == 0 && isDirectlyConnected(row1, col1, row1, col2, board)
            && isDirectlyConnected(row1, col2, row2, col2, board)) {
                moves.add(new Move(row1, col1, row1, col2));
                moves.add(new Move(row1, col2, row2, col2));
                return true;
            }
            if(board[row2][col1] == 0 && isDirectlyConnected(row2, col2, row2, col1, board)
            && isDirectlyConnected(row2, col1, row1, col1, board)) {
                moves.add(new Move(row2, col2, row2, col1));
                moves.add(new Move(row2, col1, row1, col1));
                return true;
            }

        }

        // three lines
        if (row1 != row2)
            for (int i = 0; i < board[0].length; i++) {
                if (board[row1][i] == 0 && board[row2][i] == 0 &&
                        isDirectlyConnected(row1, col1, row1, i, board) && isDirectlyConnected(row1, i, row2, i, board)
                        && isDirectlyConnected(row2, col2, row2, i, board)) {
                    moves.add(new Move(row1, col1, row1, i));
                    moves.add(new Move(row1, i, row2, i));
                    moves.add(new Move(row2, col2, row2, i));
                    return true;
                }
            }
        if (col1 != col2)
            for (int j = 0; j < board.length; j++) {
                if (board[j][col1] == 0 && board[j][col2] == 0 &&
                        isDirectlyConnected(row1, col1, j, col1, board) && isDirectlyConnected(j, col1, j, col2, board)
                        && isDirectlyConnected(row2, col2, j, col2, board)){
                    moves.add(new Move(row1, col1, j, col1));
                    moves.add(new Move(j, col1, j, col2));
                    moves.add(new Move(row2, col2, j, col2));
                    return true;
                }
            }

        return false;
    }

    // judge whether
    private boolean isDirectlyConnected(int row1, int col1, int row2, int col2, int[][] board) {
        if (row1 == row2) {
            int minCol = Math.min(col1, col2);
            int maxCol = Math.max(col1, col2);
            for (int col = minCol + 1; col < maxCol; col++) {
                if (board[row1][col] != 0) {
                    return false;
                }
            }
            return true;
        } else if (col1 == col2) {
            int minRow = Math.min(row1, row2);
            int maxRow = Math.max(row1, row2);
            for (int row = minRow + 1; row < maxRow; row++) {
                if (board[row][col1] != 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    static class Move {
        int row1;
        int col1;
        int row2;
        int col2;

        public Move(int row1, int col1, int row2, int col2) {
            this.row1 = row1;
            this.col1 = col1;
            this.row2 = row2;
            this.col2 = col2;
        }

        public String toString() {
            return "(" + row1 + ", " + col1 + ") -> (" + row2 + ", " + col2 + ")";
        }
    }
}
