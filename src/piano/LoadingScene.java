package piano;

import exceptions.FileException;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class LoadingScene extends Scene implements Runnable {

    private static VBox pane = new VBox();
    private static ProgressBar ind = new ProgressBar(0);
    private static ImageView imageView;
    private Thread loadingSceneThread;

    public static void initializeLoadingPane() throws FileException {
        Image image;
        try {
            image = new Image(new FileInputStream("src/piano/HoP_Header.png"));
        } catch (FileNotFoundException e) {
            throw new FileException("HoP_Header.png not found.");
        }
        AnchorPane anchor = new AnchorPane();
        imageView = new ImageView(image);
        imageView.setLayoutX(121);
        imageView.setLayoutY(98);
        imageView.setFitWidth(782);
        imageView.setFitHeight(235);
        ind.setLayoutX(155);
        ind.setLayoutY(431);
        ind.setPrefSize(700, 30);
        anchor.getChildren().addAll(imageView, ind);
        pane.setStyle("-fx-background-color: #FFFFFF");
        pane.getChildren().addAll(anchor);
    }

    public LoadingScene(double v, double v1) {
        super(pane, v, v1);
        loadingSceneThread = new Thread(this);
        loadingSceneThread.start();
    }

    public synchronized void quitLoading() {
        loadingSceneThread.interrupt();
    }

    public synchronized void join() throws InterruptedException {
        loadingSceneThread.join();
    }

    @Override
    public void run() {
        try {
            for(int i = 0; i < 10; i++) {
                ind.setProgress(ind.getProgress() + 0.1F);
                Thread.sleep(150);
            }
        }
        catch(InterruptedException ie) {ie.printStackTrace();}
    }
}
