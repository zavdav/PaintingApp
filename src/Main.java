import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class Main extends Application {
    // Definition of FXML elements to use in Java code
    @FXML
    private MenuBar menuBar;
    @FXML
    private HBox mainBox;
    @FXML
    private HBox toolBox;
    @FXML
    private ToolBar toolBar;
    @FXML
    private ToggleButton brush;
    @FXML
    private ToggleButton eraser;
    @FXML
    private ToggleButton eyedropper;
    @FXML
    private HBox colorBox;
    @FXML
    private HBox RGBControls;
    @FXML
    private Slider redSlider;
    @FXML
    private Slider greenSlider;
    @FXML
    private Slider blueSlider;
    @FXML
    private TextField txtRed;
    @FXML
    private TextField txtGreen;
    @FXML
    private TextField txtBlue;
    @FXML
    private TextField txtHex;
    @FXML
    private HBox colorDisplay;
    @FXML
    private HBox canvasBox;
    @FXML
    private Canvas canvas;

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
        brush = (ToggleButton) loader.getNamespace().get("brush");
        eraser = (ToggleButton) loader.getNamespace().get("eraser");
        eyedropper = (ToggleButton) loader.getNamespace().get("eyedropper");
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
        // Brush + eraser functionality
        GraphicsContext gc = canvas.getGraphicsContext2D();
        ToggleGroup toggleGroup = new ToggleGroup();
        brush.setToggleGroup(toggleGroup);
        eraser.setToggleGroup(toggleGroup);
        eyedropper.setToggleGroup(toggleGroup);
        addSelectionEventFilter(brush);
        addSelectionEventFilter(eraser);
        addSelectionEventFilter(eyedropper);
        toggleGroup.selectToggle(brush);
        canvas.setOnMouseDragged(event -> {
            if(brush.isSelected()){
                gc.fillOval(event.getX(), event.getY(), 10, 10);
            }
            else if(eraser.isSelected()){
                gc.clearRect(event.getX(), event.getY(), 10, 10);
            }
        });
        // Color selection + binding of Sliders and TextBoxes
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
        primaryStage.getIcons().add(new Image("resources/images/icon.png"));
        primaryStage.setTitle("PaintingApp");
        primaryStage.show();
    }
    // Custom TextFormatters, in order to limit input into TextBoxes
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
    // Prevent the currently selected ToggleButton from being deselected
    public void addSelectionEventFilter(ToggleButton toggleButton){
        toggleButton.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if(toggleButton.isSelected()){
                event.consume();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
