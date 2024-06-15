package com.zavdav.PaintingApp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Scene scene = new Scene(loader.load());
        primaryStage.setScene(scene);
        Controller controller = loader.getController();
        controller.initCanvas();
        primaryStage.setOnCloseRequest(controller::handleCloseRequest);
        primaryStage.getIcons().add(new Image("resources/images/icon.png"));
        primaryStage.show();
    }

    public static void main(String[] args){
        System.setProperty("prism.allowhidpi", "false");
        System.setProperty("glass.win.uiScale", "100%");
        System.setProperty("glass.win.renderScale", "100%");
        System.setProperty("glass.gtk.uiScale", "100%");
        launch(args);
    }
}
