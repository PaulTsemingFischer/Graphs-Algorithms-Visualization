module com.fischerabruzese.graphs {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;


    opens com.fischerabruzese.graphsFX to javafx.fxml;
    exports com.fischerabruzese.graphsFX;
}