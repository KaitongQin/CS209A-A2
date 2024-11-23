package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameClient extends Application {
    Controller controller;
    public FXMLLoader fxmlLoader;
    public Stage stage;
    public Socket socket;
    private Alert alert;
    private static final Logger logger = Logger.getLogger(GameClient.class.getName());
    private int ID;
    private boolean isClosing = false;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Game Board");
//        stage.show();
        stage.setResizable(false);
        stage.setAlwaysOnTop(false);
        stage.setOnCloseRequest(event -> {
            isClosing = true;
            try {
                if (socket != null && !socket.isClosed()) {
                    sendCmd("Client " + ID + " disconnected.");
                    socket.close();
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error during socket closure: " + e);
            }
            Platform.exit();
        });
        alert = new Alert(Alert.AlertType.INFORMATION);
        try {
            socket = new Socket("127.0.0.1", 6667);
            Thread thread = new Thread(this::handle);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.toString());
            stage.close();
        }
    }


    private void handle() {
        logger.log(Level.INFO, "Client connected, waiting for messages...");
        try (Scanner in = new Scanner(socket.getInputStream())) {
            sendCmd("Hello!");
            String Ack = "";
            try {
                Ack = in.nextLine();
                if (Ack.startsWith("ACK")) {
                    System.out.println("From server: " + Ack);
                    String[] args = Ack.split(" ");
                    this.ID = Integer.parseInt(args[args.length - 1]);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to connect to server: " + e);
            }

//            sendCmd("GAME_REQUEST");
            typeIn(); //request: to request a game
            String message = "";
            while (true) {
                try {
                    message = in.nextLine();
                } catch (Exception e) {
                    if (isClosing) {
                        logger.log(Level.WARNING, "You are closing the window!");
                        break;
                    }
                    logger.log(Level.WARNING, "The connection to the server was lost!");
                    Platform.runLater(() -> {
                        if (stage.isShowing()) {
                            stage.close();
                        }
                        alert.setTitle("Connection Error");
                        alert.setHeaderText(null);
                        alert.setContentText("The connection to the server was lost!");
                        alert.showAndWait();
                    });
                    break;
                }
                System.out.println("From server: " + message);
                if (message.startsWith("Waiting for opponent...")) {
                    Platform.runLater(() -> {
                        alert.setTitle("Waiting");
                        alert.setHeaderText(null);
                        alert.setContentText("Client " + this.ID + " is waiting for opponent...");
                        alert.showAndWait();
                    });
                }

                if (message.contains("Game started!")) {
                    Platform.runLater(() -> {
                        alert.setTitle("success");
                        alert.setHeaderText(null);
                        alert.setContentText("Client " + this.ID + " has matched Successfully! Game started!");
                        alert.show();
                    });

                    PauseTransition delay = new PauseTransition(Duration.seconds(3));
                    String finalMessage = message;
                    delay.setOnFinished(e -> {
                        Platform.runLater(() -> {
                            if (alert.isShowing()) {
                                alert.close();
                            }
                            String[] boardIn = finalMessage.split(": ")[1].split(" ");
                            String[] tmp = finalMessage.split(": ")[0].split(" ");
                            int width = Integer.parseInt(tmp[2]);
                            int height = Integer.parseInt(tmp[3]);
                            int[][] board = new int[width+2][height+2];
                            int c = 0;
                            for (int i = 0; i < board.length; i++) {
                                for (int j = 0; j < board[i].length; j++) {
                                    board[i][j] = Integer.parseInt(boardIn[c]);
                                    c++;
                                }
                            }
                            String[] tmpSplit = finalMessage.split(": ")[0].split(" ");
                            Controller.game = new Game(board);
                            Platform.runLater(() -> {
                                try {
                                    fxmlLoader = new FXMLLoader(org.example.demo.Application.class.getResource("board.fxml"));
                                    VBox root = fxmlLoader.load();
                                    controller = fxmlLoader.getController();
                                    controller.createGameBoard();
                                    controller.Player1 = this.socket;
                                    controller.Player1Output = new PrintWriter(this.socket.getOutputStream(), true);

                                    controller.ID = this.ID;
                                    controller.OpponentID = Integer.parseInt(tmpSplit[tmpSplit.length - 2]);

                                    Scene scene = new Scene(root);
                                    stage.setTitle("Game Board at Client " + this.ID);
                                    stage.setScene(scene);
                                    stage.show();
                                } catch (IOException e1) {
                                    logger.log(Level.SEVERE, "Failed to load view: " + e1);
                                }
                            });
                        });
                    });
                    delay.play();

                }

                if (message.startsWith("Opponent button pressed at: ")) {
                    String[] location = message.split("Opponent button pressed at: ")[1].split(" ");
                    controller.isPlayerTurn = false;
                    int row1 = Integer.parseInt(location[0]);
                    int col1 = Integer.parseInt(location[1]);
                    int row2 = Integer.parseInt(location[2]);
                    int col2 = Integer.parseInt(location[3]);
                    controller.position[0] = 1;
                    controller.position[1] = row1;
                    controller.position[2] = col1;
                    Platform.runLater(() -> {
                        controller.isPlayerTurn = false;
                        controller.handleButtonPress(row2, col2);
                        controller.isPlayerTurn = true;
                    });
                    controller.is = true;
                }

                if (message.startsWith("Game over!")) {
                    String gameResult = message.split("Game over! ")[1];

                    Platform.runLater(() -> {
                        Alert gameOverAlert = new Alert(Alert.AlertType.INFORMATION);
                        gameOverAlert.setTitle("Game Over");
                        gameOverAlert.setHeaderText(null);
                        gameOverAlert.setContentText("Game over! " + gameResult);
                        gameOverAlert.showAndWait();
                    });
                }

                if (message.contains("disconnected")) {
                    Platform.runLater(() -> {
                        if (stage.isShowing()) {
                            stage.close();
                        }
                        alert.setTitle("Connection Error");
                        alert.setHeaderText(null);
                        alert.setContentText("Opponent is disconnected!");
                        alert.show();
                        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> alert.close()));
                        timeline.setCycleCount(1);
                        timeline.play();
                    });
                }


                //register case
                if (message.equals("Register successfully!") || message.equals("This name is already existed! Please login!")) {
                    typeIn();
                    continue;
                }

                //login case
                if (message.startsWith("Login successfully!") || message.equals("No user found!") || message.equals("Wrong password!")) {
                    typeIn();
                    continue;
                }


                if (message.startsWith("Existing users: ") || message.equals("No other opponents waiting!")) {
                    typeIn();
                    continue;
                }


                if (message.startsWith("Answer: ")) {
                    typeIn();
                    continue;
                }

                message = "";
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Connection closed or error occurred", e);
        }
    }

    public void sendCmd(String cmd) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            out.println(cmd);
            out.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send command", e);
        }
    }

    public void typeIn() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            Scanner sc = new Scanner(System.in);
            String cmd = sc.nextLine();
            out.println(cmd);
            out.flush();
        } catch (IOException e) {
            if (socket.isClosed() || !socket.isConnected()) {
                Platform.runLater(() -> {
                    alert.setTitle("Connection Error");
                    alert.setHeaderText(null);
                    alert.setContentText("The connection to the server was lost.");
                    alert.showAndWait();
                });
            } else {
                logger.log(Level.SEVERE, "Connection closed or error occurred", e);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
