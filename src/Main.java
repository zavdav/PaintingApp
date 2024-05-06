import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

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
    public Slider redSlider;
    @FXML
    public Slider greenSlider;
    @FXML
    public Slider blueSlider;
    @FXML
    public TextField txtRed;
    @FXML
    public TextField txtGreen;
    @FXML
    public TextField txtBlue;
    @FXML
    public TextField txtHex;
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
        redSlider = (Slider) loader.getNamespace().get("redSlider");
        greenSlider = (Slider) loader.getNamespace().get("greenSlider");
        blueSlider = (Slider) loader.getNamespace().get("blueSlider");
        txtRed = (TextField) loader.getNamespace().get("txtRed");
        txtGreen = (TextField) loader.getNamespace().get("txtGreen");
        txtBlue =  (TextField) loader.getNamespace().get("txtBlue");
        txtHex = (TextField) loader.getNamespace().get("txtHex");
        colorDisplay = (HBox) loader.getNamespace().get("colorDisplay");
        canvas = (Canvas) loader.getNamespace().get("canvas");
        txtRed.setTextFormatter(colorChannelFormatter());
        txtGreen.setTextFormatter(colorChannelFormatter());
        txtBlue.setTextFormatter(colorChannelFormatter());
        txtHex.setTextFormatter(hexFormatter());
        txtHex.setText("000000");
        // Brush functionality
        GraphicsContext gc = canvas.getGraphicsContext2D();
        canvas.setOnMouseDragged(event -> gc.fillOval(event.getX(),event.getY(), 10, 10));
        // Color selection
        txtHex.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue.equals(newValue)&& newValue.length() == 6){
                gc.setFill(Color.web("#"+txtHex.getText()));
                redSlider.setValue(Integer.parseInt(txtHex.getText(0,2),16));
                greenSlider.setValue(Integer.parseInt(txtHex.getText(2,4),16));
                blueSlider.setValue(Integer.parseInt(txtHex.getText(4,6),16));
            }
        });
        redSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            redSlider.setValue(Math.floor((Double) newValue));
            txtRed.setText(String.valueOf((int) Math.floor((Double) newValue)));
            txtHex.setText(String.format("%02X", redSlider.valueProperty().intValue())+
                    txtHex.getText(2,6));
        });
        greenSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            greenSlider.setValue(Math.floor((Double) newValue));
            txtGreen.setText(String.valueOf((int) Math.floor((Double) newValue)));
            txtHex.setText(txtHex.getText(0,2)+
                    String.format("%02X", greenSlider.valueProperty().intValue())+
                    txtHex.getText(4,6));
        });
        blueSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            blueSlider.setValue(Math.floor((Double) newValue));
            txtBlue.setText(String.valueOf((int) Math.floor((Double) newValue)));
            txtHex.setText(txtHex.getText(0,4)+
                    String.format("%02X", blueSlider.valueProperty().intValue()));
        });
        Bindings.bindBidirectional(txtRed.textProperty(), redSlider.valueProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(txtGreen.textProperty(), greenSlider.valueProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(txtBlue.textProperty(), blueSlider.valueProperty(), new NumberStringConverter());
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
    public TextFormatter<String> hexFormatter(){
        Pattern hexPattern = Pattern.compile("[0-9a-fA-F]*");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if(hexPattern.matcher(newText).matches() && newText.length() <= 6) {
                return change;
            } else{
                return null;
            }
        };
        return new TextFormatter<>(filter);
    }
    public TextFormatter<String> colorChannelFormatter(){
        Pattern colorChannelPattern = Pattern.compile("[0-9]*");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if(colorChannelPattern.matcher(newText).matches() && newText.length() <= 3){
                return change;
            } else{
                return null;
            }
        };
        return new TextFormatter<>(filter);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
