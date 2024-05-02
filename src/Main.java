import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Main extends Application {
    // Definition of FXML elements to use in Java code
    @FXML
    MenuBar menuBar;
    @FXML
    public HBox mainBox;
    @FXML
    public HBox toolBox;
    @FXML
    public ToolBar toolBar;
    @FXML
    public HBox colorBox;
    @FXML
    public HBox RGBControls;
    @FXML
    public HBox colorDisplay;
    @FXML
    public HBox canvasBox;
    @FXML
    public Canvas canvas;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        // Injection of FXML elements
        menuBar = (MenuBar) loader.getNamespace().get("menuBar");
        mainBox = (HBox) loader.getNamespace().get("mainBox");
        toolBox = (HBox) loader.getNamespace().get("toolBox");
        toolBar = (ToolBar) loader.getNamespace().get("toolBar");
        colorBox = (HBox) loader.getNamespace().get("colorBox");
        canvasBox = (HBox) loader.getNamespace().get("canvasBox");
        RGBControls = (HBox) loader.getNamespace().get("RGBControls");
        colorDisplay = (HBox) loader.getNamespace().get("colorDisplay");
        // Binding of width and height properties, in order to fit all window sizes
        menuBar.prefWidthProperty().bind(primaryStage.widthProperty());
        mainBox.prefWidthProperty().bind(primaryStage.widthProperty());
        toolBox.prefWidthProperty().bind(Bindings.divide(mainBox.prefWidthProperty(),2));
        toolBar.prefWidthProperty().bind(toolBox.prefWidthProperty());
        colorBox.prefWidthProperty().bind(Bindings.divide(mainBox.prefWidthProperty(),2));
        canvasBox.prefWidthProperty().bind(primaryStage.widthProperty());
        canvasBox.prefHeightProperty().bind(primaryStage.heightProperty().subtract(menuBar.prefHeightProperty()).subtract(mainBox.prefHeightProperty()));
        colorDisplay.prefWidthProperty().bind(colorBox.prefWidthProperty().subtract(RGBControls.prefWidthProperty()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
