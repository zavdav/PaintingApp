import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;

public class Main extends Application {
    @FXML
    public MenuBar menuBar;
    public HBox mainBox;
    public HBox toolBox;
    public HBox colorBox;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("main.fxml"));
        Parent root = loader.load();
        primaryStage.setScene(new Scene(root));
        primaryStage.widthProperty().addListener(listener);
        primaryStage.heightProperty().addListener(listener);
        primaryStage.show();
    }
    final ChangeListener<Number> listener = new ChangeListener<Number>() {
        final Timer timer = new Timer();
        TimerTask task = null;
        final long delayTime = 200;

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if(task != null){
                task.cancel();
            }
            task = new TimerTask() {
                @Override
                public void run() {
                    responsive();
                }
            };
            timer.schedule(task, delayTime);
        }
    };

    public void responsive(){
        menuBar.setPrefWidth(100);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
