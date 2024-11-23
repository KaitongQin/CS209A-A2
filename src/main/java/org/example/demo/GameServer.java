package org.example.demo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GameServer {
    private static final int PORT = 6667;
    private static final Queue<ClientHandler> waitingQueue = new LinkedList<>();
    private static final Random random = new Random();
    private static int clientCounter = 0;
    private static final Map<String, ClientHandler> clients = new HashMap<>();
    private static final List<User> users = new ArrayList<>();
    private static final List<User> playingUsers = new ArrayList<>();
    private static final List<competition> competitions = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started at port: " + PORT);

            // read users
            try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/demo/users.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] userInfo = line.split(",");
                    String fileUsername = userInfo[0];
                    String filePassword = userInfo[1];
                    User user = new User(fileUsername, filePassword);
                    users.add(user);
                }
            } catch (IOException e) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE, "something went wrong when read users: ", e);
            }

            // read histories
            try (BufferedReader reader = new BufferedReader(new FileReader("src/main/java/org/example/demo/history.txt"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    competition comp = parseHistory(line);  // 调用解析方法
                    competitions.add(comp);                // 添加到列表
                }
            } catch (IOException e) {
                Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE, "something went wrong when read histories: ", e);
            }

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clientHandler.start();
            }
        } catch (IOException e) {
            Logger.getLogger(GameServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private static competition parseHistory(String historyLine) {
        String[] parts = historyLine.split("=>");
        String[] players = parts[0].trim().replace("(", "").replace(")", "").split(" : ");
        String[] scores = parts[1].trim().split("\\)")[0].replace("(", "").split(" : ");
        String winnerPart = "";
        if (historyLine.contains("Winner")) {
            winnerPart = parts[1].split("Winner is")[1].trim().replace("!", "");
        }
        String player1 = players[0].trim();
        String player2 = players[1].trim();
        int score1 = Integer.parseInt(scores[0].trim());
        int score2 = Integer.parseInt(scores[1].trim());
        competition comp = new competition(player1, player2, score1, score2);
        if (winnerPart.equals(player1)) {
            comp.setWinner(player1);
            comp.setIs_draw(false);
        } else if (winnerPart.equals(player2)) {
            comp.setWinner(player2);
            comp.setIs_draw(false);
        } else {
            comp.setWinner("Draw");
            comp.setIs_draw(true);
        }

        return comp;
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private ClientHandler opponent; // 存储对手的信息
        private boolean isPlaying = false;
        private final int clientID;
        private boolean first = false;
        private boolean isWaiting = false;
        int score1 = 0;
        int score2 = 0;
        private User user;
        private static final Logger logger = Logger.getLogger(GameServer.class.getName());

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            synchronized (GameServer.class) {
                this.clientID = clientCounter++;
            }
            System.out.println("Client ID: " + clientID + " connected");
        }

        @Override
        public void run() {
            try {
                String message;
                if (socket.isClosed()) {
                    System.out.println("Client ID: " + clientID + " is closed");
                }
                label:
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from client " + clientID + ": " + message);

                    if (message.equals("Hello!")) {
                        out.println("ACK! Your ID is: " + clientID);
                    }

                    if (message.equals("request")) {
                        handleGameRequest();
                    }

                    if (message.startsWith("Button pressed at: ")) {
                        String[] location = message.split("Button pressed at: ")[1].split(" ");
                        int row1 = Integer.parseInt(location[0]);
                        int col1 = Integer.parseInt(location[1]);
                        int row2 = Integer.parseInt(location[2]);
                        int col2 = Integer.parseInt(location[3]);
                        opponent.out.println("Opponent button pressed at: " + row1 + " " + col1 + " " + row2 + " " + col2);
                    }

                    if (message.startsWith("Game over!")) {
                        String[] tokens = message.split(" ");
                        int c1 = Integer.parseInt(tokens[3]);
                        int c2 = Integer.parseInt(tokens[7]);
                        int c1_score = Integer.parseInt(tokens[5]);
                        int c2_score = Integer.parseInt(tokens[9]);
                        if (c1 == this.clientID) {
                            this.score1 = c1_score;
                            opponent.score2 = c2_score;
                        } else {
                            this.score2 = c2_score;
                            opponent.score1 = c1_score;
                        }
                        int winner = -1;
                        if ((this.score1 == this.score2) && (opponent.score1 == opponent.score2)) {
                            if (this.score1 > opponent.score2) {
                                winner = this.clientID;
                                this.out.println("Game over! Client " + clientID + " wins!");
                                opponent.out.println("Game over! Client " + opponent.clientID + " loses!");
                            } else if (this.score1 < opponent.score2) {
                                winner = this.opponent.clientID;
                                this.out.println("Game over! Client " + clientID + " loses!");
                                opponent.out.println("Game over! Client " + opponent.clientID + " wins!");
                            } else {
                                this.out.println("Game over! Client " + clientID + " evens!");
                                opponent.out.println("Game over! Client " + opponent.clientID + " evens!");
                            }

                            try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/demo/history.txt", true))) {
                                if (user != null) {
                                    String player1 = user.getName();
                                    String player2 = opponent.user.getName();
                                    String w = "";
                                    if (winner != -1) {
                                        if (this.clientID == winner) {
                                            w = player1;
                                        } else {
                                            w = player2;
                                        }
                                        writer.write("(" + player1 + " : " + player2 + ") => (" + this.score1 + " : " + opponent.score2 + ")  Winner is " + w + "!");
                                        writer.newLine();
                                    } else {
                                        w = "Draw!";
                                        writer.write("(" + player1 + " : " + player2 + ") => (" + this.score1 + " : " + opponent.score2 + ")  " + w);
                                        writer.newLine();
                                    }
                                }
                            } catch (IOException e) {
                                logger.log(Level.SEVERE, "something went wrong ", e);
                            }
                        }
                    }

                    if (message.contains("disconnected")) {
                        String[] parts = message.split(" ");
                        int disconnectedId = Integer.parseInt(parts[1]);
                        if (disconnectedId == this.clientID) {
                            opponent.out.println("Opponent disconnected!");
                        }
                    }

                    if (message.startsWith("register")) {
                        String[] tokens = message.split(" ");
                        String playerName = tokens[1];
                        String password = tokens[2];
                        for (User user : users) {
                            if (user.getName().equals(playerName)) {
                                this.out.println("This name is already existed! Please login!");
                                continue label;
                            }
                        }
                        User user = new User(playerName, password);
                        users.add(user);

                        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/demo/users.txt", true))) {
                            writer.write(user.getName() + "," + user.getPassword());
                            writer.newLine(); // 换行
                            this.out.println("Register successfully!");
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "something went wrong ", e);
                        }
                    }

                    if (message.startsWith("login")) {
                        String[] tokens = message.split(" ");
                        String username = tokens[1];
                        String password = tokens[2];

                        for (User user : users) {
                            if (user.getName().equals(username) && user.getPassword().equals(password)) {
                                playingUsers.add(user);
                                clients.put(username, this);
                                this.user = user;
                                this.out.println("Login successfully! Hello, " + username);
                                continue label;
                            } else if (user.getName().equals(username) && !password.equals(user.getPassword())) {
                                this.out.println("Wrong password!");
                                continue label;
                            }
                        }
                        this.out.println("No user found!");
                    }

                    if (message.equals("list")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Existing users: ");
                        for (User user: playingUsers) {
                            if (user.getName().equals(this.user.getName())) {
                                continue;
                            }
                            sb.append(user.getName()).append(", ");
                        }
                        this.out.println(sb);
                    }


                    if (message.startsWith("pair with ")) {
                        String[] tokens = message.split(" ");
                        String playerName = tokens[2];
                        int width = Integer.parseInt(tokens[3]);
                        int height = Integer.parseInt(tokens[4]);
                        ClientHandler opponentClient = clients.get(playerName);
                        startGameWith(opponentClient, width, height);
                    }


                    if (message.startsWith("find")) {
                        String[] tokens = message.split(" ");
                        String state = tokens[1];
                        String name = tokens[2];
                        if (state.equals("playing")) {
                            boolean isPlaying = false;
                            for (User user : playingUsers) {
                                if (user.getName().equals(name)) {
                                    isPlaying = true;
                                }
                            }
                            if (isPlaying) {
                                this.out.println("Answer: " + name + " is playing!");
                            } else {
                                this.out.println("Answer: " + name + " is not playing!");
                            }
                        } else if (state.equals("history")) {
                            StringBuilder output = new StringBuilder();
                            output.append("{");
                            boolean find = false;
                            for (competition competition : competitions) {
                                if (competition.getPlayer1().equals(name) || competition.getPlayer2().equals(name)) {
                                    find = true;
                                    if (competition.isIs_draw()) {
                                        output.append("[")
                                                .append("(").append(competition.getPlayer1()).append(" : ").append(competition.getPlayer2()).append(") => ")
                                                .append("(").append(competition.getScore1()).append(" : ").append(competition.getScore2()).append(")  Draw!], ");
                                    } else {
                                        output.append("[")
                                                .append("(").append(competition.getPlayer1()).append(" : ").append(competition.getPlayer2()).append(") => ")
                                                .append("(").append(competition.getScore1()).append(" : ").append(competition.getScore2()).append(")  Winner is ")
                                                .append(competition.getWinner()).append("!], ");
                                    }
                                }
                            }
                            if (!find) {
                                this.out.println("Answer: No user found!");
                            } else {
                                output.setLength(output.length() - 2);
                                this.out.println("Answer: " + output.append("}"));
                            }
                        }
                    }

                }
            } catch (IOException e) {
                if (this.isWaiting) {
                    Iterator<ClientHandler> iterator = waitingQueue.iterator();
                    while (iterator.hasNext()) {
                        ClientHandler handler = iterator.next();
                        if (handler.clientID == this.clientID) {
                            iterator.remove(); // Remove the handler from the queue
                            break; // Exit the loop once found
                        }
                    }
                    logger.log(Level.SEVERE, "Client " + this.clientID + " accidentally disconnected when waiting!");
                } else {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            } finally {
                closeConnection();
            }
        }

        private void startGameWith(ClientHandler opponentHandler, int width, int height) throws IOException {
            this.opponent = opponentHandler;
            opponentHandler.opponent = this;
            this.isPlaying = true;
            opponentHandler.isPlaying = true;
            int[][] board = new int[width+2][height+2];
            boolean check = true;

            while (check) {
                board = Game.SetupBoard(width, height);
                for (int i = 1; i < board.length-1; i++) {
                    for (int j = 1; j < board[i].length-1; j++) {
                        if (board[i][j] == board[i - 1][j] || board[i][j] == board[i][j - 1]) {
                            check = false;
                            break;
                        }
                    }
                }
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < width + 2; i++) {
                for (int j = 0; j < height + 2; j++) {
                    builder.append(board[i][j]).append(" ");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/java/org/example/demo/plays.txt"))) {
                writer.write(this.user.getName() + " " + this.opponent.user.getName());
                writer.newLine();
            }

            this.out.println("Board Size " + width + " " + height + " Game started! OpponentID is " + opponentHandler.clientID + " Board: " + builder.toString());
            opponentHandler.out.println("Board Size " + width + " " + height + " Game started! OpponentID is " + this.clientID + " Board: " + builder.toString());
        }

        private void handleGameRequest() {
            synchronized (waitingQueue) {
                if (waitingQueue.isEmpty()) {
                    waitingQueue.add(this);
                    this.isWaiting = true;
                    out.println("Waiting for opponent...");
                    this.first = true;
                } else {
                    this.isWaiting = false;
                    ClientHandler opponentClient = waitingQueue.remove();
                    opponentClient.isWaiting = false;
                    startGameWith(opponentClient);
                }

            }
        }

        private void startGameWith(ClientHandler opponentHandler) {
            this.opponent = opponentHandler;
            opponentHandler.opponent = this;
            this.isPlaying = true;
            opponentHandler.isPlaying = true;
            int[][] board = Game.SetupBoard(6, 8);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 10; j++) {
                    builder.append(board[i][j]).append(" ");
                }
            }

            this.out.println("Board Size 6 8 Game started! OpponentID is " + opponentHandler.clientID + " Board: " + builder.toString());
            opponentHandler.out.println("Board Size 6 8 Game started! OpponentID is " + this.clientID + " Board: " + builder.toString());
        }

        private void closeConnection() {
            try {
                if (opponent != null) {
                    opponent.out.println("Accidentally disconnected! EXIT NOW");
                    opponent.opponent = null;
                }
                socket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing connection", e);
            }
        }
    }
}
