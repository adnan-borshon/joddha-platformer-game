package platformgame;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClass extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(MainClass.class.getResource("/FirstPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Joddha");
        stage.setScene(scene);
        stage.setResizable(false);       
        stage.setFullScreen(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }


}
