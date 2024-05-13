import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.Cursor;
import javafx.scene.canvas.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import java.util.ArrayList;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class Main extends Application {
    // Definition of FXML elements to use in Java code
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem undo;
    @FXML
    private MenuItem redo;
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
    private Pane colorView;
    @FXML
    private HBox canvasBox;
    @FXML
    private Canvas canvas;
    // Cursor of the currently selected tool
    private ImageCursor currentCursor;
    // Current paint color
    private Paint currentPaint;
    // ArrayList for canvas snapshots, required for undo/redo
    private ArrayList<WritableImage> changeList;
    // Index of the current WritableImage in the ArrayList
    private int currentIdx;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        changeList = new ArrayList<>();
        currentIdx = 0;
        // Injection of FXML elements
        menuBar = (MenuBar) loader.getNamespace().get("menuBar");
        undo = (MenuItem) loader.getNamespace().get("undo");
        redo = (MenuItem) loader.getNamespace().get("redo");
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
        colorView = (Pane) loader.getNamespace().get("colorView");
        canvas = (Canvas) loader.getNamespace().get("canvas");
        txtRed.setTextFormatter(colorChannelFormatter());
        txtGreen.setTextFormatter(colorChannelFormatter());
        txtBlue.setTextFormatter(colorChannelFormatter());
        txtHex.setTextFormatter(hexFormatter());
        // Brush + eraser functionality
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        changeList.add(takeSnapshot());
        ToggleGroup toggleGroup = new ToggleGroup();
        brush.setToggleGroup(toggleGroup);
        eraser.setToggleGroup(toggleGroup);
        eyedropper.setToggleGroup(toggleGroup);
        addSelectionEventFilter(brush);
        addSelectionEventFilter(eraser);
        addSelectionEventFilter(eyedropper);
        toggleGroup.selectToggle(brush);
        brush.selectedProperty().set(true);
        currentCursor = new ImageCursor(new Image("resources/images/cursor_brush.png"));
        brush.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                currentCursor = new ImageCursor(new Image("resources/images/cursor_brush.png"));
            }
        });
        eraser.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                currentCursor = new ImageCursor(new Image("resources/images/cursor_eraser.png"));
            }
        });
        eyedropper.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                currentCursor = new ImageCursor(new Image("resources/images/cursor_eyedropper.png"));
            }
        });
        canvasBox.setOnMouseExited(event -> scene.setCursor(Cursor.DEFAULT));
        canvasBox.setOnMouseEntered(event -> scene.setCursor(currentCursor));
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
            event -> {
                changeList =  new ArrayList<>(changeList.subList(0, currentIdx+1));
                if(brush.isSelected()){
                    gc.setLineWidth(2);
                    gc.setStroke(currentPaint);
                }else if(eraser.isSelected()){
                    gc.setLineWidth(10);
                    gc.setStroke(Color.WHITE);
                }
                gc.beginPath();
                gc.moveTo(event.getX(), event.getY());
                gc.stroke();
            });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
            event -> {
                gc.lineTo(event.getX(), event.getY());
                gc.stroke();
                gc.closePath();
                gc.beginPath();
                gc.moveTo(event.getX(), event.getY());
            });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
            event -> {
                gc.lineTo(event.getX(), event.getY());
                gc.stroke();
                gc.closePath();
                changeList.add(takeSnapshot());
                currentIdx++;
            });
        // Color selection + binding of Sliders and TextBoxes
        currentPaint = Color.web("#000000");
        colorView.setStyle("-fx-background-color:#000000");
        txtHex.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!oldValue.equals(newValue)&& newValue.length() == 6){
                currentPaint = Color.web("#"+txtHex.getText());
                redSlider.setValue(Integer.parseInt(txtHex.getText(0,2),16));
                greenSlider.setValue(Integer.parseInt(txtHex.getText(2,4),16));
                blueSlider.setValue(Integer.parseInt(txtHex.getText(4,6),16));
                colorView.setStyle("-fx-background-color:#"+txtHex.getText());
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
        // Undo/redo functionality
        undo.setOnAction(event -> undo());
        redo.setOnAction(event -> redo());
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
    // Take a snapshot of the current state of the canvas
    public WritableImage takeSnapshot(){
        WritableImage wi = new WritableImage((int)Math.rint(canvas.getWidth()), (int)Math.rint(canvas.getHeight()));
        SnapshotParameters sp = new SnapshotParameters();
        sp.setDepthBuffer(true);
        return canvas.snapshot(sp, wi);
    }
    // Jump to the previous snapshot
    public void undo(){
        if(currentIdx > 0 && currentIdx < changeList.size()){
            currentIdx--;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(changeList.get(currentIdx),0,0);
        }
    }
    // Jump to the next snapshot
    public void redo(){
        if(currentIdx >= 0 && currentIdx < changeList.size()-1){
            currentIdx++;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(changeList.get(currentIdx),0,0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
