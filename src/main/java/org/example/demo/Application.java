package org.example.demo;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Scanner;

import static org.example.demo.Game.SetupBoard;

public class Application extends javafx.application.Application {

    @Override
    public void start(Stage stage) throws IOException {
        int[] size = getBoardSizeFromUser();
        Controller.game = new Game(SetupBoard(size[0], size[1]));
        FXMLLoader fxmlLoader = new FXMLLoader(Application.class.getResource("board.fxml"));
        VBox root = fxmlLoader.load();
        Controller controller = fxmlLoader.getController();
        controller.createGameBoard();
        Scene scene = new Scene(root);
        stage.setTitle("Game Board");
        stage.setScene(scene);
        stage.show();
    }

    // let user choose board size
    public int[] getBoardSizeFromUser() {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Board Size");
        dialog.setHeaderText("Please enter the number of rows and columns:");

        // 创建输入字段
        TextField rowInput = new TextField();
        rowInput.setPromptText("Rows");
        TextField colInput = new TextField();
        colInput.setPromptText("Columns");

        // 布局
        GridPane grid = new GridPane();
        grid.add(new Label("Rows:"), 0, 0);
        grid.add(rowInput, 1, 0);
        grid.add(new Label("Columns:"), 0, 1);
        grid.add(colInput, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // 添加按钮
        ButtonType confirmButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == confirmButton) {
                try {
                    int row = Integer.parseInt(rowInput.getText());
                    int col = Integer.parseInt(colInput.getText());
                    return new int[]{row, col};
                } catch (NumberFormatException e) {
                    return new int[]{4, 4};  // 默认值
                }
            }
            return null;
        });

        // 显示对话框并获取结果
        return dialog.showAndWait().orElse(new int[]{4, 4});
    }


    public static void main(String[] args) {
        launch();
    }
}