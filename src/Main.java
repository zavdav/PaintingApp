import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class Main extends Application {
    // Definition of FXML elements to use in Java code
    @FXML
    private MenuBar menuBar;
    @FXML
    private MenuItem newImg;
    @FXML
    private MenuItem open;
    @FXML
    private MenuItem save;
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
    // ArrayList of Canvas snapshots, required for undo/redo
    private ArrayList<WritableImage> changeList;
    // Index of the currently selected snapshot
    private int currentIdx;
    // For determining if a file has been opened
    private boolean isFileOpened;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        // Injection of FXML elements
        menuBar = (MenuBar) loader.getNamespace().get("menuBar");
        newImg = (MenuItem) loader.getNamespace().get("newImg");
        open = (MenuItem) loader.getNamespace().get("open");
        save = (MenuItem) loader.getNamespace().get("save");
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
        // Initialization of MenuBar and Canvas
        changeList = new ArrayList<>();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        createNew();
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
        // Brush + eraser functionality
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
        // Create new, open & save images
        newImg.setOnAction(event -> createNew());
        open.setOnAction(event -> open(primaryStage));
        save.setOnAction(event -> save(primaryStage));
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
            canvas.setWidth(changeList.get(currentIdx).getWidth());
            canvas.setHeight(changeList.get(currentIdx).getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(changeList.get(currentIdx),0,0);
        }
    }
    // Jump to the next snapshot
    public void redo(){
        if(currentIdx >= 0 && currentIdx < changeList.size()-1){
            currentIdx++;
            canvas.setWidth(changeList.get(currentIdx).getWidth());
            canvas.setHeight(changeList.get(currentIdx).getHeight());
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(changeList.get(currentIdx),0,0);
        }
    }
    // Create a blank image
    public void createNew(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        changeList.clear();
        currentIdx = 0;
        changeList.add(takeSnapshot());
        isFileOpened = true;
    }
    // Open a selected image
    public void open(Stage stage){
        FileChooser openFile = new FileChooser();
        openFile.setTitle("Open File");
        File file = openFile.showOpenDialog(stage);
        if(isFileOpened){
            if(!showAlert().get()){
                open(stage);
            }
        }
        if(file != null){
            Image image = new Image(file.toURI().toString());
            canvas.setWidth(image.getWidth());
            canvas.setHeight(image.getHeight());
            canvas.getGraphicsContext2D().drawImage(image, 0, 0);
            changeList.clear();
            currentIdx = 0;
            WritableImage append = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
            changeList.add(append);
        }
    }
    // Save the current image
    public void save(Stage stage){
        FileChooser saveFile = new FileChooser();
        saveFile.setTitle("Save File");
        saveFile.setInitialFileName("Untitled.png");
        saveFile.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File file = saveFile.showSaveDialog(stage);
        if(file != null){
            try{
                WritableImage image = takeSnapshot();
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(renderedImage, "png", file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public AtomicBoolean showAlert(){
        AtomicBoolean canceled = new AtomicBoolean(true);
        Alert alert = new Alert(Alert.AlertType.NONE);
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image("resources/images/warning.png"));
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Would you like to save the changes to Untitled.png?");
        ButtonType save = new ButtonType("Save");
        ButtonType doNotSave = new ButtonType("Don't Save");
        ButtonType cancel = new ButtonType("Cancel");
        alert.getButtonTypes().addAll(save, doNotSave, cancel);
        alertStage.setOnCloseRequest(event -> alert.close());
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(chosen -> {
            if(chosen.equals(save)){
                isFileOpened = false;
                canceled.set(false);
            }else if(chosen.equals(doNotSave)){
                isFileOpened = false;
                canceled.set(false);
            }else if(chosen.equals(cancel)){
                alert.close();
            }
        });
        return canceled;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
