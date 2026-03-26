import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.MainWindow;

/**
 * Minimal demo runner for the Travel Planner domain model.
 */
public class Main extends Application {
//    public static void main(String[] args) {
//        Trip trip = new Trip(1, "Osaka Weekend",
//                LocalDateTime.of(2026, 4, 3, 9, 0),
//                LocalDateTime.of(2026, 4, 5, 18, 0));
//
//        Activity museum = new Activity(10, "Museum",
//                LocalDateTime.of(2026, 4, 4, 10, 0),
//                LocalDateTime.of(2026, 4, 4, 12, 0));
//
//        museum.addExpense(new Expense(100, "Ticket", 25.0f, Expense.Currency.USD, Expense.Type.ENTERTAINMENT));
//
//        try {
//            trip.addActivity(museum);
//        } catch (TimeIntervalConflictException e) {
//            System.out.println("Unexpected overlap: " + e.getMessage());
//        }
//
//        Activity overlap = new Activity(11, "Brunch",
//                LocalDateTime.of(2026, 4, 4, 11, 30),
//                LocalDateTime.of(2026, 4, 4, 12, 30));
//
//        try {
//            trip.addActivity(overlap);
//        } catch (TimeIntervalConflictException e) {
//            System.out.println("Overlap detected");
//        }
//
//        System.out.println("Total USD: " + trip.getTotalCost(Expense.Currency.USD));
//    }
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            BorderPane root = fxmlLoader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
