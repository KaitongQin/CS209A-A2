package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Controller {

    public Button resetButton;
    @FXML
    public Label scoreLabel2; //Opponent
    @FXML
    public Label scoreLabel1; //Me

    Socket Player1;
    PrintWriter Player1Output;
    @FXML
    private GridPane gameBoard;

    public static Game game;

    public int score1 = 0; //Me
    public int score2 = 0; //Opponent

    int[] position = new int[3];

    boolean isPlayerTurn = true;
    boolean gameOver = false;
    int ID;
    int OpponentID;

    boolean is = true;

    @FXML
    public void initialize() {

    }

    public void createGameBoard() {

        gameBoard.getChildren().clear();

        for (int row = 0; row < game.row; row++) {
            for (int col = 0; col < game.col; col++) {
                Button button = new Button();
                button.setPrefSize(40, 40);
                ImageView imageView = addContent(game.board[row][col]);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);
                imageView.setPreserveRatio(true);
                button.setGraphic(imageView);
                int finalRow = row;
                int finalCol = col;
                button.setOnAction( _ -> handleButtonPress(finalRow, finalCol));
                gameBoard.add(button, col, row);
            }
        }
    }


    public void handleButtonPress (int row, int col) {
        if (!is) {
            return;
        }
        if (position[0] == 0) {
            position[1] = row;
            position[2] = col;
            position[0] = 1;
        } else {
            if (isPlayerTurn) {
                this.Player1Output.println("Button pressed at: " + position[1] + " " + position[2] + " " + row + " " + col);
                is = false;
            }
            boolean change = game.judge(position[1], position[2], row, col);
            position[0] = 0;
            if (change) {
                for (int i = 0; i < game.moves.size(); i++) {
                    System.out.println(game.moves.get(i));
                }
                draw(12);
                PauseTransition pause = new PauseTransition(Duration.millis(500));
                pause.setOnFinished(event -> {
                    game.board[position[1]][position[2]] = 0;
                    game.board[row][col] = 0;
                    draw(0);
                    createGameBoard();
                    checkGameOver();
                });
                pause.play();
                if (isPlayerTurn) {
                    updateScore(1);
                } else {
                    updateScore(2);
                }
            } else {
                // wrong connection tip
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("connection wrong!");
                alert.setHeaderText(null);
                alert.setContentText("Wrong connection!");
                alert.show();
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0.5), e -> alert.close()));
                timeline.setCycleCount(1);
                timeline.play();
            }
        }
    }

    private void updateScore(int x) {
        if (x == 1) {
            for (int i = 0; i < game.moves.size(); i++) {
                score1 += 1 + Math.abs(game.moves.get(i).row1 - game.moves.get(i).row2) + Math.abs(game.moves.get(i).col1 - game.moves.get(i).col2);
            }
            scoreLabel1.setText(String.valueOf(score1));
        } else {
            for (int i = 0; i < game.moves.size(); i++) {
                score2 += 1 + Math.abs(game.moves.get(i).row1 - game.moves.get(i).row2) + Math.abs(game.moves.get(i).col1 - game.moves.get(i).col2);
            }
            scoreLabel2.setText(String.valueOf(score2));
        }
    }

    private void checkGameOver() {
        boolean isGameOver = true;
        for (int i = 0; i < game.row; i++) {
            for (int j = 0; j < game.col; j++) {
                if (game.board[i][j] != 0) {
                    isGameOver = false;
                    break;
                }
            }
        }
        if (isGameOver) {
            gameOver = true;
            this.Player1Output.println("Game over! Client " + ID + " get " + score1 + " Client " + OpponentID + " get " + score2);
        }
    }

    private void draw(int x) {
        for (int i = 0; i < game.moves.size(); i++) {
            int row1 = game.moves.get(i).row1;
            int col1 = game.moves.get(i).col1;
            int row2 = game.moves.get(i).row2;
            int col2 = game.moves.get(i).col2;
            int minRow = Math.min(row1, row2);
            int maxRow = Math.max(row1, row2);
            int minCol = Math.min(col1, col2);
            int maxCol = Math.max(col1, col2);
            if (row1 == row2) {
                for (int j = minCol; j <= maxCol; j++) {
                    game.board[minRow][j] = x;
                }
            }
            if (col1 == col2) {
                for (int j = minRow; j <= maxRow; j++) {
                    game.board[j][minCol] = x;
                }
            }
        }
        createGameBoard();
    }

    public int[][] getGameBoard() {
        return game.board;
    }

    @FXML
    private void handleReset() {

    }

    public ImageView addContent(int content){
        return switch (content) {
            case 0 -> new ImageView(imageCarambola);
            case 1 -> new ImageView(imageApple);
            case 2 -> new ImageView(imageMango);
            case 3 -> new ImageView(imageBlueberry);
            case 4 -> new ImageView(imageCherry);
            case 5 -> new ImageView(imageGrape);
            case 6 -> new ImageView(imageKiwi);
            case 7 -> new ImageView(imageOrange);
            case 8 -> new ImageView(imagePeach);
            case 9 -> new ImageView(imagePear);
            case 10 -> new ImageView(imagePineapple);
            case 11 -> new ImageView(imageWatermelon);
            case 12 -> new ImageView(imageYes);
            default -> null;
        };
    }

    public static Image imageApple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/apple.png")).toExternalForm());
    public static Image imageMango = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/mango.png")).toExternalForm());
    public static Image imageBlueberry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/blueberry.png")).toExternalForm());
    public static Image imageCherry = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/cherry.png")).toExternalForm());
    public static Image imageGrape = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/grape.png")).toExternalForm());
    public static Image imageCarambola = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/carambola.png")).toExternalForm());
    public static Image imageKiwi = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/kiwi.png")).toExternalForm());
    public static Image imageOrange = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/orange.png")).toExternalForm());
    public static Image imagePeach = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/peach.png")).toExternalForm());
    public static Image imagePear = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pear.png")).toExternalForm());
    public static Image imagePineapple = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/pineapple.png")).toExternalForm());
    public static Image imageWatermelon = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/watermelon.png")).toExternalForm());
    public static Image imageYes = new Image(Objects.requireNonNull(Game.class.getResource("/org/example/demo/yes.png")).toExternalForm());
}
