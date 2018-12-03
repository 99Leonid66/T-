package ru.mtuci.tp_cw;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class App extends Application {

    public static final void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("App.fxml"));
        var scene = new Scene(root);

        primaryStage.setScene(scene);

        primaryStage.setTitle("TP Course Work");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("app_icon.png")));
        primaryStage.setWidth(1200);
        primaryStage.setMinWidth(800);
        primaryStage.setHeight(800);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
	}
}
