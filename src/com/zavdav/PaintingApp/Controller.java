package com.zavdav.PaintingApp;

import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class Controller implements Initializable {
    public AnchorPane anchorPane;
    public MenuBar menuBar;
    public MenuItem newImg;
    public MenuItem open;
    public MenuItem save;
    public MenuItem undo;
    public MenuItem redo;
    public GridPane mainBox;
    public HBox toolBox;
    public ToolBar toolBar;
    public ToggleButton brush;
    public ToggleButton eraser;
    public ToggleButton paintBucket;
    public ToggleButton eyedropper;
    public ToggleButton line;
    public ToggleButton ellipse;
    public ToggleButton rectangle;
    public ToggleButton resize;
    public HBox RGBControls;
    public Slider redSlider;
    public Slider greenSlider;
    public Slider blueSlider;
    public TextField txtRed;
    public TextField txtGreen;
    public TextField txtBlue;
    public TextField txtHex;
    public HBox colorDisplay;
    public Pane colorView;
    public ScrollPane scrollPane;
    public AnchorPane scrollAnchor;
    public HBox canvasBox;
    public Canvas canvas;
    public GridPane bottomBar;
    public HBox posDisplay;
    public Label lblCursorPos;
    public HBox dimDisplay;
    public Label lblDimensions;

    private GraphicsContext gc;
    private Cursor currentCursor;
    private Paint currentPaint;
    private ArrayList<WritableImage> changeList;
    private int currentIdx;
    private String fileName;
    private boolean unsavedChanges;
    private Point startCoords;
    private int scrollCount;
    private double resizeWidth;
    private double resizeHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources){
        txtRed.setTextFormatter(colorChannelFormatter());
        txtGreen.setTextFormatter(colorChannelFormatter());
        txtBlue.setTextFormatter(colorChannelFormatter());
        txtHex.setTextFormatter(hexFormatter());
        changeList = new ArrayList<>();
        gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(false);
        ToggleGroup toggleGroup = new ToggleGroup();
        toolBar.getItems().forEach(toggle -> {
            if(toggle instanceof ToggleButton){
                ((ToggleButton) toggle).setToggleGroup(toggleGroup);
                preventToggleDeselect((ToggleButton) toggle);
            }
        });
        toggleGroup.selectToggle(brush);
        brush.selectedProperty().set(true);
        currentCursor = new ImageCursor(new Image("resources/images/cursor_brush.png"));

        addToolCursor(brush, new ImageCursor(new Image("resources/images/cursor_brush.png")));
        addToolCursor(eraser, new ImageCursor(new Image("resources/images/cursor_eraser.png")));
        addToolCursor(paintBucket, new ImageCursor(new Image("resources/images/cursor_paintbucket.png")));
        addToolCursor(eyedropper, new ImageCursor(new Image("resources/images/cursor_eyedropper.png")));
        addToolCursor(line, Cursor.CROSSHAIR);
        addToolCursor(ellipse, Cursor.CROSSHAIR);
        addToolCursor(rectangle, Cursor.CROSSHAIR);
        addToolCursor(resize, Cursor.DEFAULT);

        canvas.widthProperty().addListener((observable, oldValue, newValue) -> {
            if((double) newValue < 1){
                canvas.setWidth((double) oldValue);
            }
            lblDimensions.setText(String.format("%d x %dpx", (int) canvas.getWidth(), (int) canvas.getHeight()));
        });
        canvas.heightProperty().addListener((observable, oldValue, newValue) -> {
            if((double) newValue < 1){
                canvas.setHeight((double) oldValue);
            }
            lblDimensions.setText(String.format("%d x %dpx", (int) canvas.getWidth(), (int) canvas.getHeight()));
        });

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

        addSliderBindings(redSlider, txtRed);
        addSliderBindings(greenSlider, txtGreen);
        addSliderBindings(blueSlider, txtBlue);
        Bindings.bindBidirectional(txtRed.textProperty(), redSlider.valueProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(txtGreen.textProperty(), greenSlider.valueProperty(), new NumberStringConverter());
        Bindings.bindBidirectional(txtBlue.textProperty(), blueSlider.valueProperty(), new NumberStringConverter());

        scrollAnchor.prefWidthProperty().bind(scrollPane.widthProperty());
        scrollAnchor.prefHeightProperty().bind(scrollPane.heightProperty());
        canvasBox.prefWidthProperty().bind(scrollAnchor.prefWidthProperty());
        canvasBox.prefHeightProperty().bind(scrollAnchor.prefHeightProperty());
    }

    public void setDefaultCursor(){
        Scene scene = anchorPane.getScene();
        scene.setCursor(Cursor.DEFAULT);
    }

    public void scrollAnchorMouseEntered(){
        Scene scene = anchorPane.getScene();
        if(resize.isSelected() && !scene.getCursor().toString().contains("_RESIZE")){
            scene.setCursor(Cursor.DEFAULT);
        }else{
            scene.setCursor(currentCursor);
        }
    }

    public void canvasMouseExited(MouseEvent event){
        Scene scene = anchorPane.getScene();
        if(resize.isSelected() && !event.isPrimaryButtonDown()){
            scene.setCursor(Cursor.DEFAULT);
        }else{
            scene.setCursor(currentCursor);
        }
    }

    public void canvasMouseMoved(MouseEvent event){
        if(resize.isSelected()){
            changeResizeCursor(event);
        }
        lblCursorPos.setText(String.format("Pos: %.0f, %.0fpx", Math.floor(event.getX()), Math.floor(event.getY())));
    }

    public void canvasMousePressed(MouseEvent event){
        if(!eyedropper.isSelected()){
            changeList =  new ArrayList<>(changeList.subList(0, currentIdx+1));
            gc.setStroke(currentPaint);
            gc.setLineWidth(1);
            if(brush.isSelected() || eraser.isSelected()){
                if(eraser.isSelected()){
                    gc.setLineWidth(10);
                    gc.setStroke(Color.WHITE);
                }
                gc.beginPath();
                gc.moveTo(event.getX(), event.getY());
                gc.stroke();
            }else if(paintBucket.isSelected()){
                floodFill((int) event.getX(), (int) event.getY(), (Color) currentPaint);
            }else if(line.isSelected()){
                drawLine(event);
            }else if(ellipse.isSelected()){
                drawEllipse(event);
            }else if(rectangle.isSelected()){
                drawRectangle(event);
            }else{
                resize(event);
            }
        }else{
            eyedropperSetColor(event);
        }
    }

    public void canvasMouseDragged(MouseEvent event){
        if(!paintBucket.isSelected()){
            if(!eyedropper.isSelected()){
                if(brush.isSelected() || eraser.isSelected()){
                    gc.lineTo(event.getX(), event.getY());
                    gc.stroke();
                    gc.closePath();
                    gc.beginPath();
                    gc.moveTo(event.getX(), event.getY());
                }else if(line.isSelected()){
                    drawLine(event);
                }else if(ellipse.isSelected()){
                    drawEllipse(event);
                }else if(rectangle.isSelected()){
                    drawRectangle(event);
                }else if(resize.isSelected()){
                    resize(event);
                }
            }else{
                eyedropperSetColor(event);
            }
        }
    }

    public void canvasMouseReleased(MouseEvent event){
        if(!eyedropper.isSelected()){
            if(brush.isSelected() || eraser.isSelected()){
                gc.lineTo(event.getX(), event.getY());
                gc.stroke();
                gc.closePath();
            }else if(resize.isSelected()){
                resize(event);
            }
            changeList.add(takeSnapshot());
            currentIdx++;
            unsavedChanges = true;
        }
    }

    public TextFormatter<String> hexFormatter(){
        Pattern hexPattern = Pattern.compile("[0-9a-fA-F]*");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if(hexPattern.matcher(newText).matches() && newText.length() <= 6){
                return change;
            }else{
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
            }else{
                return null;
            }
        };
        return new TextFormatter<>(filter);
    }

    public void addToolCursor(ToggleButton toggle, Cursor cursor){
        toggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                currentCursor = cursor;
            }
        });
    }

    public void preventToggleDeselect(ToggleButton toggleButton){
        toggleButton.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if(toggleButton.isSelected()){
                event.consume();
            }
        });
    }
    public void addSliderBindings(Slider slider, TextField textField){
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            slider.setValue(Math.floor((Double) newValue));
            textField.setText(String.valueOf((int) Math.floor((Double) newValue)));
            String value = String.format("%02X%02X%02X",
                    redSlider.valueProperty().intValue(),
                    greenSlider.valueProperty().intValue(),
                    blueSlider.valueProperty().intValue());
            txtHex.setText(value);
        });
    }

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

    public void undo(){
        if(currentIdx > 0 && currentIdx < changeList.size()){
            currentIdx--;
            canvas.setWidth(changeList.get(currentIdx).getWidth());
            canvas.setHeight(changeList.get(currentIdx).getHeight());
            gc.drawImage(changeList.get(currentIdx),0,0);
            unsavedChanges = true;
        }
    }

    public void redo(){
        if(currentIdx >= 0 && currentIdx < changeList.size()-1){
            currentIdx++;
            canvas.setWidth(changeList.get(currentIdx).getWidth());
            canvas.setHeight(changeList.get(currentIdx).getHeight());
            gc.drawImage(changeList.get(currentIdx),0,0);
            unsavedChanges = true;
        }
    }

    public void createNewImage(){
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(saveImage()){
                    initCanvas();
                }
            }else if(result == 2){
                unsavedChanges = false;
                initCanvas();
            }
        }else{
            initCanvas();
        }
    }

    public void initCanvas(){
        Stage stage = (Stage) anchorPane.getScene().getWindow();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        changeList.clear();
        currentIdx = 0;
        scrollCount = 0;
        canvasBox.getTransforms().clear();
        changeList.add(takeSnapshot());
        fileName = "Untitled.png";
        stage.setTitle(String.format("%s - PaintingApp", fileName));
        unsavedChanges = false;
    }

    public void openImage(){
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(saveImage()){
                    selectImage();
                }
            }else if(result == 2){
                if(selectImage()){
                    unsavedChanges = false;
                }
            }
        }else{
            selectImage();
        }
    }

    public boolean selectImage(){
        boolean imageWritten = false;
        Stage stage = (Stage) anchorPane.getScene().getWindow();
        FileChooser openFile = new FileChooser();
        openFile.setTitle("Open File");
        File file = openFile.showOpenDialog(stage);
        if(file != null){
            Image image = new Image(file.toURI().toString());
            canvas.setWidth(image.getWidth());
            canvas.setHeight(image.getHeight());
            gc.drawImage(image, 0, 0);
            changeList.clear();
            currentIdx = 0;
            WritableImage append = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
            changeList.add(append);
            String rawPath = file.toURI().toString();
            fileName = rawPath.substring(rawPath.lastIndexOf("/")+1);
            stage.setTitle(String.format("%s - PaintingApp", fileName));
            imageWritten = true;
        }
        return imageWritten;
    }

    public boolean saveImage(){
        boolean fileSaved = false;
        Stage stage = (Stage) anchorPane.getScene().getWindow();
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
                String rawPath = file.toURI().toString();
                fileName = rawPath.substring(rawPath.lastIndexOf("/")+1);
                stage.setTitle(String.format("%s - PaintingApp", fileName));
                unsavedChanges = false;
                fileSaved = true;
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }
        return fileSaved;
    }

    public void handleCloseRequest(Event event){
        Stage stage = (Stage) anchorPane.getScene().getWindow();
        if(unsavedChanges){
            int result = showAlert().get();
            if(result == 1){
                if(saveImage()){
                    stage.close();
                }else{
                    event.consume();
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

    public void eyedropperSetColor(MouseEvent event){
        WritableImage wi = takeSnapshot();
        txtHex.setText(String.format("%s",
                wi.getPixelReader().getColor(
                        (int) Math.round(event.getX()),
                        (int) Math.round(event.getY())
                )
        ).substring(2,8).toUpperCase());
    }

    public void floodFill(int x, int y, Color newColor){
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

    public boolean isValid(PixelReader reader, int x, int y, int w, int h, Color oldColor){
        return x >= 0 && x < w && y >= 0 && y < h && reader.getColor(x, y).equals(oldColor);
    }

    public void drawLine(MouseEvent event){
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED){
            startCoords = new Point((int) event.getX(), (int) event.getY());
        }
        gc.drawImage(changeList.get(currentIdx),0,0);
        gc.strokeLine(startCoords.x, startCoords.y, event.getX(), event.getY());
    }

    public void drawEllipse(MouseEvent event){
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED){
            startCoords = new Point((int) event.getX(), (int) event.getY());
        }
        gc.drawImage(changeList.get(currentIdx),0,0);
        gc.strokeOval(Math.min(startCoords.x, event.getX()),
                Math.min(startCoords.y, event.getY()),
                Math.abs(startCoords.x-event.getX()),
                Math.abs(startCoords.y-event.getY()));
    }

    public void drawRectangle(MouseEvent event){
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED){
            startCoords = new Point((int) event.getX(), (int) event.getY());
        }
        gc.drawImage(changeList.get(currentIdx),0,0);
        gc.strokeRect(Math.min(startCoords.x, event.getX()),
                Math.min(startCoords.y, event.getY()),
                Math.abs(startCoords.x-event.getX()),
                Math.abs(startCoords.y-event.getY()));
    }

    public void changeResizeCursor(MouseEvent event){
        Point coords = new Point((int) event.getX(), (int) event.getY());
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        if(coords.x >= 0 && coords.x <= 10 && coords.y >= 0 && coords.y <= 10){
            currentCursor = Cursor.NW_RESIZE;
        }else if(coords.x >= width-10 && coords.x <= width && coords.y >= 0 && coords.y <= 10){
            currentCursor = Cursor.NE_RESIZE;
        }else if(coords.x >= 0 && coords.x <= 10 && coords.y >= height-10 && coords.y <= height){
            currentCursor = Cursor.SW_RESIZE;
        }else if(coords.x >= width-10 && coords.x <= width && coords.y >= height-10 && coords.y <= height){
            currentCursor = Cursor.SE_RESIZE;
        }else if(coords.x >= 0 && coords.x <= 10){
            currentCursor = Cursor.W_RESIZE;
        }else if(coords.x >= width-10 && coords.x <= width){
            currentCursor = Cursor.E_RESIZE;
        }else if(coords.y >= 0 && coords.y <= 10){
            currentCursor = Cursor.N_RESIZE;
        }else if(coords.y >= height-10 && coords.y <= height){
            currentCursor = Cursor.S_RESIZE;
        }else{
            currentCursor = Cursor.DEFAULT;
        }
        Scene scene = anchorPane.getScene();
        scene.setCursor(currentCursor);
    }

    public void resize(MouseEvent event){
        if(event.getEventType() == MouseEvent.MOUSE_PRESSED){
            resizeWidth = canvas.getWidth();
            resizeHeight = canvas.getHeight();
        }
        else if(event.getEventType() == MouseEvent.MOUSE_DRAGGED){
            String cursor = currentCursor.toString();
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            switch(cursor){
                case "W_RESIZE" -> {
                    canvas.setWidth(canvas.getWidth() - Math.round(event.getX()));
                    gc.drawImage(changeList.get(currentIdx), canvas.getWidth() - resizeWidth, 0);
                } case "N_RESIZE" -> {
                    canvas.setHeight(canvas.getHeight() - Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), 0, canvas.getHeight() - resizeHeight);
                } case "E_RESIZE" -> {
                    canvas.setWidth(Math.round(event.getX()));
                    gc.drawImage(changeList.get(currentIdx), 0, 0);
                } case "S_RESIZE" -> {
                    canvas.setHeight(Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), 0, 0);
                } case "NW_RESIZE" -> {
                    canvas.setWidth(canvas.getWidth() - Math.round(event.getX()));
                    canvas.setHeight(canvas.getHeight() - Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), canvas.getWidth() - resizeWidth, canvas.getHeight() - resizeHeight);
                } case "NE_RESIZE" -> {
                    canvas.setWidth(Math.round(event.getX()));
                    canvas.setHeight(canvas.getHeight() - Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), 0, canvas.getHeight() - resizeHeight);
                } case "SW_RESIZE" -> {
                    canvas.setWidth(canvas.getWidth() - Math.round(event.getX()));
                    canvas.setHeight(Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), canvas.getWidth() - resizeWidth, 0);
                } case "SE_RESIZE" -> {
                    canvas.setWidth(Math.round(event.getX()));
                    canvas.setHeight(Math.round(event.getY()));
                    gc.drawImage(changeList.get(currentIdx), 0, 0);
                }
            }
        }else{
            changeResizeCursor(event);
        }
    }

    public void zoomCanvas(ScrollEvent event){
        double zoomFactor = 0;
        if(event.getDeltaY() < 0){
            if(scrollCount >= -20){
                zoomFactor = 0.95;
                scrollCount--;
            }
        }else{
            if(scrollCount <= 20){
                zoomFactor = 1.05;
                scrollCount++;
            }
        }
        if(scrollCount >= -20 && scrollCount <= 20){
            Scale scale = new Scale();
            scale.setPivotX(event.getX());
            scale.setPivotY(event.getY());
            scale.setX(canvasBox.getScaleX()*zoomFactor);
            scale.setY(canvasBox.getScaleY()*zoomFactor);
            canvasBox.getTransforms().add(scale);
        }
    }
}
