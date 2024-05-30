import javafx.beans.binding.*;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
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
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
    private ToggleButton paintBucket;
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
    // File name of the current image
    private String fileName;
    // For determining if there are unsaved changes to a file;
    private boolean unsavedChanges;

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
        paintBucket = (ToggleButton) loader.getNamespace().get("paintBucket");
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
        initCanvas(primaryStage);
        ToggleGroup toggleGroup = new ToggleGroup();
        brush.setToggleGroup(toggleGroup);
        eraser.setToggleGroup(toggleGroup);
        paintBucket.setToggleGroup(toggleGroup);
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
        paintBucket.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                currentCursor = new ImageCursor(new Image("resources/images/cursor_paintbucket.png"));
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
                if(eyedropper.isSelected()){
                    eyedropperSetColor(event);
                }else if(paintBucket.isSelected()){
                    floodFill((int) event.getX(), (int) event.getY(), (Color) currentPaint);
                }else{
                    if(brush.isSelected()){
                        gc.setLineWidth(2);
                        gc.setStroke(currentPaint);
                    }else{
                        gc.setLineWidth(10);
                        gc.setStroke(Color.WHITE);
                    }
                    gc.beginPath();
                    gc.moveTo(event.getX(), event.getY());
                    gc.stroke();
                }
            });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,
            event -> {
                if(eyedropper.isSelected()){
                    eyedropperSetColor(event);
                }else if(!eyedropper.isSelected() && !paintBucket.isSelected()){
                    gc.lineTo(event.getX(), event.getY());
                    gc.stroke();
                    gc.closePath();
                    gc.beginPath();
                    gc.moveTo(event.getX(), event.getY());
                }
            });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED,
            event -> {
                if(paintBucket.isSelected()){
                    changeList.add(takeSnapshot());
                    currentIdx++;
                    unsavedChanges = true;
                }
                if(!paintBucket.isSelected() && !eyedropper.isSelected()){
                    gc.lineTo(event.getX(), event.getY());
                    gc.stroke();
                    gc.closePath();
                    changeList.add(takeSnapshot());
                    currentIdx++;
                    unsavedChanges = true;
                }
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
        newImg.setOnAction(event -> createNew(primaryStage));
        open.setOnAction(event -> open(primaryStage));
        save.setOnAction(event -> save(primaryStage));
        primaryStage.setOnCloseRequest(event -> handleCloseRequest(primaryStage, event));
        primaryStage.getIcons().add(new Image("resources/images/icon.png"));
        primaryStage.show();
        changeList.add(takeSnapshot());
        currentIdx++;
    }
    // Limit input into hex code TextBox to 6-digit hexadecimal numbers
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
    // Limit input into color channel TextBoxes to 3-digit integers
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
        Bounds bounds = canvas.getLayoutBounds();
        WritableImage wi = new WritableImage(
                (int) bounds.getWidth(),
                (int) bounds.getHeight()
        );
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
            unsavedChanges = true;
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
            unsavedChanges = true;
        }
    }
    // Create a new image
    public void createNew(Stage stage){
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(save(stage)){
                    initCanvas(stage);
                }
            }else if(result == 2){
                unsavedChanges = false;
                initCanvas(stage);
            }
        }else{
            initCanvas(stage);
        }
    }
    // Clear the canvas and reset the changeList
    public void initCanvas(Stage stage){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        changeList.clear();
        currentIdx = 0;
        changeList.add(takeSnapshot());
        fileName = "Untitled.png";
        stage.setTitle(String.format("%s - PaintingApp", fileName));
        unsavedChanges = false;
    }
    // Open the selected image
    public void open(Stage stage){
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(save(stage)){
                    writeOpenedFile(stage);
                }
            }else if(result == 2){
                if(writeOpenedFile(stage)){
                    unsavedChanges = false;
                }
            }
        }else{
            writeOpenedFile(stage);
        }
    }
    // Write the selected image to the canvas
    public boolean writeOpenedFile(Stage stage){
        boolean written = false;
        FileChooser openFile = new FileChooser();
        openFile.setTitle("Open File");
        File file = openFile.showOpenDialog(stage);
        if(file != null){
            Image image = new Image(file.toURI().toString());
            canvas.setWidth(image.getWidth());
            canvas.setHeight(image.getHeight());
            canvas.getGraphicsContext2D().drawImage(image, 0, 0);
            changeList.clear();
            currentIdx = 0;
            WritableImage append = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
            changeList.add(append);
            String rawPath = file.toURI().toString();
            fileName = rawPath.substring(rawPath.lastIndexOf("/")+1);
            stage.setTitle(String.format("%s - PaintingApp", fileName));
            written = true;
        }
        return written;
    }
    // Save the current image
    public boolean save(Stage stage){
        boolean fileSaved = false;
        FileChooser saveFile = new FileChooser();
        saveFile.setTitle("Save File");
        saveFile.setInitialFileName(fileName);
        saveFile.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File file = saveFile.showSaveDialog(stage);
        if(file != null){
            try{
                WritableImage image = takeSnapshot();
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(renderedImage, "png", file);
                fileSaved = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileSaved;
    }
    // Handle window close requests
    public void handleCloseRequest(Stage stage, Event event){
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(save(stage)){
                    stage.close();
                }
            }else if(result == 2){
                stage.close();
            }else{
                event.consume();
            }
        }else{
            stage.close();
        }
    }
    // Show an alert if changes have not been saved
    public AtomicInteger showAlert(){
        AtomicInteger state = new AtomicInteger(0);
        Alert alert = new Alert(Alert.AlertType.NONE);
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(new Image("resources/images/warning.png"));
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText(String.format("Do you want to save the changes to %s?", fileName));
        ButtonType save = new ButtonType("Save");
        ButtonType doNotSave = new ButtonType("Don't Save");
        ButtonType cancel = new ButtonType("Cancel");
        alert.getButtonTypes().addAll(save, doNotSave, cancel);
        alertStage.setOnCloseRequest(event -> alert.close());
        Optional<ButtonType> result = alert.showAndWait();
        result.ifPresent(chosen -> {
            if(chosen.equals(save)){
                state.set(1);
            }else if(chosen.equals(doNotSave)){
                state.set(2);
            }else if(chosen.equals(cancel)){
                alert.close();
            }
        });
        return state;
    }
    // Let the eyedropper set the current color
    public void eyedropperSetColor(MouseEvent event){
        WritableImage wi = takeSnapshot();
        txtHex.setText(String.format("%s",
                wi.getPixelReader().getColor(
                        (int) Math.round(event.getX()),
                        (int) Math.round(event.getY())
                )
        ).substring(2,8).toUpperCase());
    }
    // Flood fill algorithm used for the paint bucket tool
    public void floodFill(int x, int y, Color newColor){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        WritableImage image = takeSnapshot();
        PixelReader reader = image.getPixelReader();
        PixelWriter imgWriter = image.getPixelWriter();
        PixelWriter writer = gc.getPixelWriter();
        Color oldColor = reader.getColor(x, y);
        int w = (int) canvas.getWidth();
        int h = (int) canvas.getHeight();
        if(!oldColor.equals(newColor)){
            ArrayList<Point> queue = new ArrayList<>();
            queue.add(new Point(x, y));
            writer.setColor(x, y, newColor);
            while(queue.size() > 0){
                Point currentPixel = queue.get(0);
                queue.remove(0);
                int posX = currentPixel.x;
                int posY = currentPixel.y;
                if(isValid(reader, posX+1, posY, w, h, oldColor)){
                    imgWriter.setColor(posX+1, posY, newColor);
                    writer.setColor(posX+1, posY, newColor);
                    queue.add(new Point(posX+1, posY));
                }
                if(isValid(reader, posX-1, posY, w, h, oldColor)){
                    imgWriter.setColor(posX-1, posY, newColor);
                    writer.setColor(posX-1, posY, newColor);
                    queue.add(new Point(posX-1, posY));
                }
                if(isValid(reader, posX, posY+1, w, h, oldColor)){
                    imgWriter.setColor(posX, posY+1, newColor);
                    writer.setColor(posX, posY+1, newColor);
                    queue.add(new Point(posX, posY+1));
                }
                if(isValid(reader, posX, posY-1, w, h, oldColor)){
                    imgWriter.setColor(posX, posY-1, newColor);
                    writer.setColor(posX, posY-1, newColor);
                    queue.add(new Point(posX, posY-1));
                }
            }
        }
    }
    // Check if a pixel's color should be replaced with the fill color
    public boolean isValid(PixelReader reader, int x, int y, int w, int h, Color oldColor){
        return x >= 0 && x < w && y >= 0 && y < h && reader.getColor(x, y).equals(oldColor);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
