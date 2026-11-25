package com.coursework.ui;

import com.coursework.model.*;
import com.coursework.util.JsonUtil;
import com.google.gson.reflect.TypeToken;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * Єдиний клас JavaFX-клієнта (згідно з вимогами 2026 р.).
 * Увесь інтерфейс, HTTP-взаємодія та фоновий CSV-імпорт — тут.
 * Окремі View-класи та ApiClient відсутні.
 */
public class MainApp extends Application {

    private final String baseUrl;
    private final HttpClient http;

    // ===== Будівельні об’єкти =====
    private final TableView<BuildingObject> buildingsTable = new TableView<>();
    private final TextField bName = new TextField();
    private final TextField bAddress = new TextField();
    private final TextField bType = new TextField();
    private final TextField bYear = new TextField();
    private final TextField bFloors = new TextField();
    private BuildingObject selectedBuilding;

    // ===== Дефекти =====
    private final TableView<Defect> defectsTable = new TableView<>();
    private final ComboBox<BuildingObject> dBuildingCombo = new ComboBox<>();
    private final TextField dTitle = new TextField();
    private final TextArea dDesc = new TextArea();
    private final TextField dSeverity = new TextField();
    private final TextField dArea = new TextField();
    private final TextField dSafety = new TextField();
    private final Label dIntegral = new Label("I = —");
    private final Label dState = new Label("Стан: —");
    private final ComboBox<DefectState> dNextState = new ComboBox<>();
    private Defect selectedDefect;

    // ===== Обстеження =====
    private final TableView<Inspection> inspectionsTable = new TableView<>();
    private final ComboBox<BuildingObject> iBuildingCombo = new ComboBox<>();
    private final DatePicker iDate = new DatePicker(LocalDate.now());
    private final TextField iInspector = new TextField();
    private final TextArea iFindings = new TextArea();
    private final TextField iValue = new TextField();
    private final TextField iUnit = new TextField();
    private final TextField iNotes = new TextField();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label progressLabel = new Label("Готово");
    private final Button importBtn = new Button("Імпорт CSV…");
    private final Button cancelBtn = new Button("Скасувати");
    private Task<Integer> currentTask;
    private Inspection selectedInspection;

    // ===== Ремонтні завдання =====
    private final TableView<RepairTask> repairsTable = new TableView<>();
    private final ComboBox<Defect> rDefectCombo = new ComboBox<>();
    private final TextField rTitle = new TextField();
    private final TextArea rDesc = new TextArea();
    private final TextField rAssigned = new TextField();
    private final DatePicker rPlanned = new DatePicker();
    private final DatePicker rActual = new DatePicker();
    private final ComboBox<String> rStatus = new ComboBox<>();
    private final TextField rCost = new TextField();
    private RepairTask selectedRepair;

    public MainApp() {
        String env = System.getenv("API_BASE_URL");
        if (env == null || env.isBlank()) {
            env = System.getProperty("API_BASE_URL", "http://localhost:8080/building-defects-server");
        }
        this.baseUrl = env.endsWith("/") ? env.substring(0, env.length() - 1) : env;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public void start(Stage stage) {
        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                new Tab("Будівельні об’єкти", buildBuildingsPane()),
                new Tab("Дефекти", buildDefectsPane()),
                new Tab("Обстеження + CSV", buildInspectionsPane()),
                new Tab("Ремонтні завдання", buildRepairsPane())
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));

        Scene scene = new Scene(tabs, 1150, 720);
        stage.setTitle("Облік дефектів будівельних об’єктів — Варіант 16");
        stage.setScene(scene);
        stage.show();
    }

    // ===================== HTTP helpers (замість ApiClient) =====================

    private <T> List<T> getList(String path, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path)).GET().timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        Type type = TypeToken.getParameterized(List.class, clazz).getType();
        List<T> list = JsonUtil.gson().fromJson(resp.body(), type);
        return list != null ? list : Collections.emptyList();
    }

    private <T> T post(String path, Object body, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(body)))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return JsonUtil.fromJson(resp.body(), clazz);
    }

    private <T> T put(String path, Object body, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(body)))
                .timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return JsonUtil.fromJson(resp.body(), clazz);
    }

    private void delete(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path)).DELETE().timeout(Duration.ofSeconds(15)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
    }

    private Map<?, ?> importInspections(List<Inspection> items) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/inspections/import"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(items)))
                .timeout(Duration.ofSeconds(60)).build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        checkStatus(resp);
        return JsonUtil.fromJson(resp.body(), Map.class);
    }

    private void checkStatus(HttpResponse<String> response) throws IOException {
        int code = response.statusCode();
        if (code >= 200 && code < 300) return;
        String msg = response.body();
        try {
            Map<?, ?> err = JsonUtil.fromJson(msg, Map.class);
            if (err != null && err.containsKey("error")) msg = String.valueOf(err.get("error"));
        } catch (Exception ignored) {}
        throw new IOException("HTTP " + code + ": " + msg);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Помилка");
        a.showAndWait();
    }

    // ===================== Будівельні об’єкти =====================

    private BorderPane buildBuildingsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        TableColumn<BuildingObject, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);
        TableColumn<BuildingObject, String> nameCol = new TableColumn<>("Назва");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);
        TableColumn<BuildingObject, String> addrCol = new TableColumn<>("Адреса");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addrCol.setPrefWidth(250);
        TableColumn<BuildingObject, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("objectType"));
        typeCol.setPrefWidth(120);
        TableColumn<BuildingObject, Integer> yearCol = new TableColumn<>("Рік");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("yearBuilt"));
        yearCol.setPrefWidth(70);
        TableColumn<BuildingObject, Integer> floorsCol = new TableColumn<>("Поверхів");
        floorsCol.setCellValueFactory(new PropertyValueFactory<>("floors"));
        floorsCol.setPrefWidth(80);
        buildingsTable.getColumns().addAll(idCol, nameCol, addrCol, typeCol, yearCol, floorsCol);
        buildingsTable.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> {
            selectedBuilding = n;
            if (n != null) {
                bName.setText(n.getName());
                bAddress.setText(n.getAddress());
                bType.setText(n.getObjectType());
                bYear.setText(n.getYearBuilt() != null ? n.getYearBuilt().toString() : "");
                bFloors.setText(n.getFloors() != null ? n.getFloors().toString() : "");
            }
        });
        pane.setCenter(buildingsTable);

        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> reloadBuildings());
        Button clear = new Button("Очистити форму");
        clear.setOnAction(e -> clearBuildingForm());
        HBox top = new HBox(10, refresh, clear);
        top.setPadding(new Insets(0, 0, 10, 0));
        pane.setTop(top);

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(6); form.setPadding(new Insets(10, 0, 0, 0));
        form.add(new Label("Назва:"), 0, 0); form.add(bName, 1, 0);
        form.add(new Label("Адреса:"), 0, 1); form.add(bAddress, 1, 1);
        form.add(new Label("Тип:"), 0, 2); form.add(bType, 1, 2);
        form.add(new Label("Рік:"), 0, 3); form.add(bYear, 1, 3);
        form.add(new Label("Поверхів:"), 0, 4); form.add(bFloors, 1, 4);
        Button createBtn = new Button("Створити");
        createBtn.setOnAction(e -> createBuilding());
        Button updateBtn = new Button("Оновити");
        updateBtn.setOnAction(e -> updateBuilding());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> deleteBuilding());
        form.add(new HBox(10, createBtn, updateBtn, deleteBtn), 1, 5);
        pane.setBottom(form);

        reloadBuildings();
        return pane;
    }

    private void reloadBuildings() {
        new Thread(() -> {
            try {
                List<BuildingObject> list = getList("/api/buildings", BuildingObject.class);
                Platform.runLater(() -> {
                    buildingsTable.setItems(FXCollections.observableArrayList(list));
                    dBuildingCombo.setItems(FXCollections.observableArrayList(list));
                    iBuildingCombo.setItems(FXCollections.observableArrayList(list));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private void createBuilding() {
        try {
            BuildingObject obj = buildingFromForm();
            if (obj.getName() == null || obj.getName().isBlank()) {
                showError("Назва обов’язкова");
                return;
            }
            new Thread(() -> {
                try {
                    post("/api/buildings", obj, BuildingObject.class);
                    Platform.runLater(() -> { clearBuildingForm(); reloadBuildings(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void updateBuilding() {
        if (selectedBuilding == null) { showError("Оберіть запис"); return; }
        try {
            BuildingObject obj = buildingFromForm();
            obj.setId(selectedBuilding.getId());
            new Thread(() -> {
                try {
                    put("/api/buildings/" + obj.getId(), obj, BuildingObject.class);
                    Platform.runLater(this::reloadBuildings);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteBuilding() {
        if (selectedBuilding == null) { showError("Оберіть запис"); return; }
        new Thread(() -> {
            try {
                delete("/api/buildings/" + selectedBuilding.getId());
                Platform.runLater(() -> { clearBuildingForm(); reloadBuildings(); });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private BuildingObject buildingFromForm() {
        BuildingObject obj = new BuildingObject();
        obj.setName(bName.getText().trim());
        obj.setAddress(bAddress.getText().trim());
        obj.setObjectType(bType.getText().trim());
        if (!bYear.getText().isBlank()) obj.setYearBuilt(Integer.parseInt(bYear.getText().trim()));
        if (!bFloors.getText().isBlank()) obj.setFloors(Integer.parseInt(bFloors.getText().trim()));
        obj.setStatus("ACTIVE");
        return obj;
    }

    private void clearBuildingForm() {
        selectedBuilding = null;
        bName.clear(); bAddress.clear(); bType.clear(); bYear.clear(); bFloors.clear();
        buildingsTable.getSelectionModel().clearSelection();
    }

    // ===================== Дефекти =====================

    private BorderPane buildDefectsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        TableColumn<Defect, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);
        TableColumn<Defect, String> titleCol = new TableColumn<>("Назва");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(180);
        TableColumn<Defect, String> buildingCol = new TableColumn<>("Об’єкт");
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("buildingObjectName"));
        buildingCol.setPrefWidth(150);
        TableColumn<Defect, BigDecimal> sevCol = new TableColumn<>("F1");
        sevCol.setCellValueFactory(new PropertyValueFactory<>("severity"));
        sevCol.setPrefWidth(50);
        TableColumn<Defect, BigDecimal> areaCol = new TableColumn<>("F2");
        areaCol.setCellValueFactory(new PropertyValueFactory<>("areaScale"));
        areaCol.setPrefWidth(50);
        TableColumn<Defect, BigDecimal> safeCol = new TableColumn<>("F3");
        safeCol.setCellValueFactory(new PropertyValueFactory<>("safetyImpact"));
        safeCol.setPrefWidth(50);
        TableColumn<Defect, BigDecimal> iCol = new TableColumn<>("I");
        iCol.setCellValueFactory(new PropertyValueFactory<>("integralScore"));
        iCol.setPrefWidth(60);
        TableColumn<Defect, DefectState> stateCol = new TableColumn<>("Стан");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("state"));
        stateCol.setPrefWidth(140);
        defectsTable.getColumns().addAll(idCol, titleCol, buildingCol, sevCol, areaCol, safeCol, iCol, stateCol);
        defectsTable.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> fillDefectForm(n));
        pane.setCenter(defectsTable);

        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> { reloadBuildings(); reloadDefects(); });
        Button clear = new Button("Очистити");
        clear.setOnAction(e -> clearDefectForm());
        HBox top = new HBox(10, refresh, clear);
        top.setPadding(new Insets(0, 0, 8, 0));
        pane.setTop(top);

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(5); form.setPadding(new Insets(8, 0, 0, 0));
        form.add(new Label("Об’єкт:"), 0, 0); form.add(dBuildingCombo, 1, 0);
        dBuildingCombo.setPrefWidth(260);
        dBuildingCombo.setConverter(buildingConverter());
        form.add(new Label("Назва:"), 0, 1); form.add(dTitle, 1, 1);
        form.add(new Label("Опис:"), 0, 2); dDesc.setPrefRowCount(2); form.add(dDesc, 1, 2);
        form.add(new Label("F1 (тяжкість 0-100):"), 0, 3); form.add(dSeverity, 1, 3);
        form.add(new Label("F2 (масштаб 0-100):"), 0, 4); form.add(dArea, 1, 4);
        form.add(new Label("F3 (безпека 0-100):"), 0, 5); form.add(dSafety, 1, 5);
        form.add(dIntegral, 1, 6);
        form.add(dState, 1, 7);
        form.add(new Label("Новий стан:"), 0, 8); form.add(dNextState, 1, 8);
        Button createBtn = new Button("Створити");
        createBtn.setOnAction(e -> createDefect());
        Button updateBtn = new Button("Оновити");
        updateBtn.setOnAction(e -> updateDefect());
        Button changeStateBtn = new Button("Змінити стан");
        changeStateBtn.setOnAction(e -> changeDefectState());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> deleteDefect());
        form.add(new HBox(8, createBtn, updateBtn, changeStateBtn, deleteBtn), 1, 9);
        pane.setBottom(form);

        reloadDefects();
        return pane;
    }

    private void fillDefectForm(Defect n) {
        selectedDefect = n;
        if (n == null) return;
        dTitle.setText(n.getTitle());
        dDesc.setText(n.getDescription() != null ? n.getDescription() : "");
        dSeverity.setText(n.getSeverity() != null ? n.getSeverity().toPlainString() : "");
        dArea.setText(n.getAreaScale() != null ? n.getAreaScale().toPlainString() : "");
        dSafety.setText(n.getSafetyImpact() != null ? n.getSafetyImpact().toPlainString() : "");
        dIntegral.setText("I = " + (n.getIntegralScore() != null ? n.getIntegralScore() : "—"));
        dState.setText("Стан: " + n.getState());
        dNextState.getItems().setAll(DefectState.allowedNext(n.getState()));
        dNextState.getSelectionModel().clearSelection();
        // select building
        for (BuildingObject b : dBuildingCombo.getItems()) {
            if (b.getId() == n.getBuildingObjectId()) {
                dBuildingCombo.getSelectionModel().select(b);
                break;
            }
        }
    }

    private void reloadDefects() {
        new Thread(() -> {
            try {
                List<Defect> list = getList("/api/defects", Defect.class);
                Platform.runLater(() -> {
                    defectsTable.setItems(FXCollections.observableArrayList(list));
                    rDefectCombo.setItems(FXCollections.observableArrayList(list));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private void createDefect() {
        try {
            Defect d = defectFromForm();
            if (d.getTitle() == null || d.getTitle().isBlank()) {
                showError("Назва обов’язкова");
                return;
            }
            if (d.getBuildingObjectId() == null || d.getBuildingObjectId() <= 0) {
                showError("Оберіть будівельний об’єкт");
                return;
            }
            new Thread(() -> {
                try {
                    post("/api/defects", d, Defect.class);
                    Platform.runLater(() -> { clearDefectForm(); reloadDefects(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void updateDefect() {
        if (selectedDefect == null) { showError("Оберіть запис"); return; }
        try {
            Defect d = defectFromForm();
            d.setId(selectedDefect.getId());
            d.setState(selectedDefect.getState()); // стан не змінюємо через update
            new Thread(() -> {
                try {
                    put("/api/defects/" + d.getId(), d, Defect.class);
                    Platform.runLater(this::reloadDefects);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void changeDefectState() {
        if (selectedDefect == null) { showError("Оберіть запис"); return; }
        DefectState next = dNextState.getValue();
        if (next == null) { showError("Оберіть новий стан"); return; }
        new Thread(() -> {
            try {
                post("/api/defects/" + selectedDefect.getId() + "/state", Map.of("state", next.name()), Defect.class);
                Platform.runLater(this::reloadDefects);
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private void deleteDefect() {
        if (selectedDefect == null) { showError("Оберіть запис"); return; }
        new Thread(() -> {
            try {
                delete("/api/defects/" + selectedDefect.getId());
                Platform.runLater(() -> { clearDefectForm(); reloadDefects(); });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private Defect defectFromForm() {
        Defect d = new Defect();
        BuildingObject b = dBuildingCombo.getValue();
        if (b != null) d.setBuildingObjectId(b.getId());
        d.setTitle(dTitle.getText().trim());
        d.setDescription(dDesc.getText().trim());
        if (!dSeverity.getText().isBlank()) d.setSeverity(new BigDecimal(dSeverity.getText().trim()));
        if (!dArea.getText().isBlank()) d.setAreaScale(new BigDecimal(dArea.getText().trim()));
        if (!dSafety.getText().isBlank()) d.setSafetyImpact(new BigDecimal(dSafety.getText().trim()));
        d.setState(DefectState.ЗАФІКСОВАНО);
        return d;
    }

    private void clearDefectForm() {
        selectedDefect = null;
        dTitle.clear(); dDesc.clear(); dSeverity.clear(); dArea.clear(); dSafety.clear();
        dIntegral.setText("I = —"); dState.setText("Стан: —");
        dNextState.getItems().clear();
        dBuildingCombo.getSelectionModel().clearSelection();
        defectsTable.getSelectionModel().clearSelection();
    }

    // ===================== Обстеження + CSV =====================

    private BorderPane buildInspectionsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        TableColumn<Inspection, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);
        TableColumn<Inspection, String> buildingCol = new TableColumn<>("Об'єкт");
        buildingCol.setCellValueFactory(new PropertyValueFactory<>("buildingObjectName"));
        buildingCol.setPrefWidth(150);
        TableColumn<Inspection, LocalDate> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("inspectionDate"));
        dateCol.setPrefWidth(100);
        TableColumn<Inspection, String> inspCol = new TableColumn<>("Інспектор");
        inspCol.setCellValueFactory(new PropertyValueFactory<>("inspectorName"));
        inspCol.setPrefWidth(130);
        TableColumn<Inspection, String> findingsCol = new TableColumn<>("Висновки");
        findingsCol.setCellValueFactory(new PropertyValueFactory<>("findings"));
        findingsCol.setPrefWidth(220);
        TableColumn<Inspection, BigDecimal> valCol = new TableColumn<>("Значення");
        valCol.setCellValueFactory(new PropertyValueFactory<>("measuredValue"));
        valCol.setPrefWidth(80);
        TableColumn<Inspection, String> unitCol = new TableColumn<>("Од.");
        unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        unitCol.setPrefWidth(50);
        inspectionsTable.getColumns().addAll(idCol, buildingCol, dateCol, inspCol, findingsCol, valCol, unitCol);
        inspectionsTable.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> fillInspectionForm(n));
        pane.setCenter(inspectionsTable);

        progressBar.setPrefWidth(220);
        cancelBtn.setDisable(true);
        cancelBtn.setOnAction(e -> { if (currentTask != null) currentTask.cancel(); });
        importBtn.setOnAction(e -> startImport());

        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> { reloadBuildings(); reloadInspections(); });
        HBox top = new HBox(10, refresh, importBtn, cancelBtn, progressBar, progressLabel);
        top.setPadding(new Insets(0, 0, 8, 0));
        pane.setTop(top);

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(5); form.setPadding(new Insets(6, 0, 0, 0));
        form.add(new Label("Об'єкт:"), 0, 0);
        iBuildingCombo.setPrefWidth(260);
        iBuildingCombo.setConverter(buildingConverter());
        form.add(iBuildingCombo, 1, 0);
        form.add(new Label("Дата:"), 0, 1); form.add(iDate, 1, 1);
        form.add(new Label("Інспектор:"), 0, 2); form.add(iInspector, 1, 2);
        form.add(new Label("Висновки:"), 0, 3); iFindings.setPrefRowCount(2); form.add(iFindings, 1, 3);
        form.add(new Label("Значення:"), 0, 4); form.add(iValue, 1, 4);
        form.add(new Label("Одиниця:"), 0, 5); form.add(iUnit, 1, 5);
        form.add(new Label("Примітки:"), 0, 6); form.add(iNotes, 1, 6);
        Button createBtn = new Button("Створити");
        createBtn.setOnAction(e -> createInspection());
        Button updateBtn = new Button("Оновити");
        updateBtn.setOnAction(e -> updateInspection());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> deleteInspection());
        form.add(new HBox(8, createBtn, updateBtn, deleteBtn), 1, 7);

        VBox bottom = new VBox(6, form,
                new Label("CSV: buildingObjectId;inspectionDate(yyyy-MM-dd);inspectorName;findings;measuredValue;unit;notes"));
        bottom.getChildren().get(1).setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        pane.setBottom(bottom);

        reloadInspections();
        return pane;
    }

    private void fillInspectionForm(Inspection n) {
        selectedInspection = n;
        if (n == null) return;
        iDate.setValue(n.getInspectionDate());
        iInspector.setText(n.getInspectorName());
        iFindings.setText(n.getFindings() != null ? n.getFindings() : "");
        iValue.setText(n.getMeasuredValue() != null ? n.getMeasuredValue().toPlainString() : "");
        iUnit.setText(n.getUnit() != null ? n.getUnit() : "");
        iNotes.setText(n.getNotes() != null ? n.getNotes() : "");
        for (BuildingObject b : iBuildingCombo.getItems()) {
            if (b.getId() == n.getBuildingObjectId()) {
                iBuildingCombo.getSelectionModel().select(b);
                break;
            }
        }
    }

    private void reloadInspections() {
        new Thread(() -> {
            try {
                List<Inspection> list = getList("/api/inspections", Inspection.class);
                Platform.runLater(() -> inspectionsTable.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private void createInspection() {
        try {
            Inspection i = inspectionFromForm();
            if (i.getBuildingObjectId() == null || i.getBuildingObjectId() <= 0 || i.getInspectorName() == null || i.getInspectorName().isBlank()) {
                showError("Об'єкт і інспектор обов’язкові");
                return;
            }
            new Thread(() -> {
                try {
                    post("/api/inspections", i, Inspection.class);
                    Platform.runLater(() -> { clearInspectionForm(); reloadInspections(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void updateInspection() {
        if (selectedInspection == null) { showError("Оберіть запис"); return; }
        try {
            Inspection i = inspectionFromForm();
            i.setId(selectedInspection.getId());
            new Thread(() -> {
                try {
                    put("/api/inspections/" + i.getId(), i, Inspection.class);
                    Platform.runLater(this::reloadInspections);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteInspection() {
        if (selectedInspection == null) { showError("Оберіть запис"); return; }
        new Thread(() -> {
            try {
                delete("/api/inspections/" + selectedInspection.getId());
                Platform.runLater(() -> { clearInspectionForm(); reloadInspections(); });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private Inspection inspectionFromForm() {
        Inspection i = new Inspection();
        BuildingObject b = iBuildingCombo.getValue();
        if (b != null) i.setBuildingObjectId(b.getId());
        i.setInspectionDate(iDate.getValue());
        i.setInspectorName(iInspector.getText().trim());
        i.setFindings(iFindings.getText().trim());
        if (!iValue.getText().isBlank()) i.setMeasuredValue(new BigDecimal(iValue.getText().trim()));
        i.setUnit(iUnit.getText().trim());
        i.setNotes(iNotes.getText().trim());
        return i;
    }

    private void clearInspectionForm() {
        selectedInspection = null;
        iInspector.clear(); iFindings.clear(); iValue.clear(); iUnit.clear(); iNotes.clear();
        iDate.setValue(LocalDate.now());
        iBuildingCombo.getSelectionModel().clearSelection();
        inspectionsTable.getSelectionModel().clearSelection();
    }

    private void startImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Виберіть CSV файл обстежень");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File file = fc.showOpenDialog(importBtn.getScene().getWindow());
        if (file == null) return;

        currentTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                List<Inspection> items = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    String line;
                    boolean first = true;
                    int lineNo = 0;
                    while ((line = br.readLine()) != null) {
                        lineNo++;
                        if (first) { first = false; continue; }
                        if (line.isBlank()) continue;
                        if (isCancelled()) break;
                        try {
                            String[] p = line.split("[;,]", -1);
                            if (p.length < 4) throw new IllegalArgumentException("мало полів");
                            Inspection i = new Inspection();
                            i.setBuildingObjectId(Integer.parseInt(p[0].trim()));
                            i.setInspectionDate(LocalDate.parse(p[1].trim()));
                            i.setInspectorName(p[2].trim());
                            i.setFindings(p[3].trim());
                            if (p.length > 4 && !p[4].isBlank() && !"null".equalsIgnoreCase(p[4].trim())) {
                                i.setMeasuredValue(new BigDecimal(p[4].trim()));
                            }
                            if (p.length > 5) i.setUnit(p[5].trim());
                            if (p.length > 6) i.setNotes(p[6].trim());
                            items.add(i);
                        } catch (Exception ex) {
                            errors.add("Рядок " + lineNo + ": " + ex.getMessage());
                        }
                        updateProgress(lineNo, lineNo + 10);
                        updateMessage("Оброблено рядків: " + lineNo);
                    }
                }
                if (isCancelled()) return 0;
                updateMessage("Надсилання на сервер…");
                Map<?, ?> result = importInspections(items);
                int imported = result.containsKey("imported") ? ((Number) result.get("imported")).intValue() : items.size();
                updateMessage("Імпортовано: " + imported + (errors.isEmpty() ? "" : "; помилок розбору: " + errors.size()));
                return imported;
            }
        };

        progressBar.progressProperty().bind(currentTask.progressProperty());
        progressLabel.textProperty().bind(currentTask.messageProperty());
        importBtn.setDisable(true);
        cancelBtn.setDisable(false);

        currentTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressBar.setProgress(1);
            importBtn.setDisable(false);
            cancelBtn.setDisable(true);
            reloadInspections();
        });
        currentTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressLabel.setText("Помилка");
            importBtn.setDisable(false);
            cancelBtn.setDisable(true);
            showError(currentTask.getException() != null ? currentTask.getException().getMessage() : "Помилка імпорту");
        });
        currentTask.setOnCancelled(e -> {
            progressBar.progressProperty().unbind();
            progressLabel.textProperty().unbind();
            progressLabel.setText("Скасовано");
            importBtn.setDisable(false);
            cancelBtn.setDisable(true);
        });

        new Thread(currentTask).start();
    }

    // ===================== Ремонтні завдання =====================

    private BorderPane buildRepairsPane() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));

        TableColumn<RepairTask, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);
        TableColumn<RepairTask, String> titleCol = new TableColumn<>("Назва");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(180);
        TableColumn<RepairTask, String> assignedCol = new TableColumn<>("Виконавець");
        assignedCol.setCellValueFactory(new PropertyValueFactory<>("assignedTo"));
        assignedCol.setPrefWidth(120);
        TableColumn<RepairTask, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        TableColumn<RepairTask, BigDecimal> costCol = new TableColumn<>("Вартість");
        costCol.setCellValueFactory(new PropertyValueFactory<>("costEstimate"));
        costCol.setPrefWidth(90);
        repairsTable.getColumns().addAll(idCol, titleCol, assignedCol, statusCol, costCol);
        repairsTable.getSelectionModel().selectedItemProperty().addListener((o, old, n) -> fillRepairForm(n));
        pane.setCenter(repairsTable);

        Button refresh = new Button("Оновити");
        refresh.setOnAction(e -> { reloadDefects(); reloadRepairs(); });
        Button clear = new Button("Очистити");
        clear.setOnAction(e -> clearRepairForm());
        HBox top = new HBox(10, refresh, clear);
        top.setPadding(new Insets(0, 0, 8, 0));
        pane.setTop(top);

        rStatus.getItems().addAll("PLANNED", "IN_PROGRESS", "DONE", "CANCELLED");
        rStatus.setValue("PLANNED");
        rDefectCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Defect d) {
                return d == null ? "" : d.getId() + " — " + d.getTitle();
            }
            @Override public Defect fromString(String s) { return null; }
        });

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(5); form.setPadding(new Insets(6, 0, 0, 0));
        form.add(new Label("Дефект:"), 0, 0); form.add(rDefectCombo, 1, 0);
        rDefectCombo.setPrefWidth(300);
        form.add(new Label("Назва робіт:"), 0, 1); form.add(rTitle, 1, 1);
        form.add(new Label("Опис:"), 0, 2); rDesc.setPrefRowCount(2); form.add(rDesc, 1, 2);
        form.add(new Label("Виконавець:"), 0, 3); form.add(rAssigned, 1, 3);
        form.add(new Label("Планова дата:"), 0, 4); form.add(rPlanned, 1, 4);
        form.add(new Label("Фактична дата:"), 0, 5); form.add(rActual, 1, 5);
        form.add(new Label("Статус:"), 0, 6); form.add(rStatus, 1, 6);
        form.add(new Label("Вартість:"), 0, 7); form.add(rCost, 1, 7);
        Button createBtn = new Button("Створити");
        createBtn.setOnAction(e -> createRepair());
        Button updateBtn = new Button("Оновити");
        updateBtn.setOnAction(e -> updateRepair());
        Button deleteBtn = new Button("Видалити");
        deleteBtn.setOnAction(e -> deleteRepair());
        form.add(new HBox(8, createBtn, updateBtn, deleteBtn), 1, 8);
        pane.setBottom(form);

        reloadRepairs();
        return pane;
    }

    private void fillRepairForm(RepairTask n) {
        selectedRepair = n;
        if (n == null) return;
        rTitle.setText(n.getTitle());
        rDesc.setText(n.getDescription() != null ? n.getDescription() : "");
        rAssigned.setText(n.getAssignedTo() != null ? n.getAssignedTo() : "");
        rPlanned.setValue(n.getPlannedDate());
        rActual.setValue(n.getActualDate());
        rStatus.setValue(n.getStatus() != null ? n.getStatus() : "PLANNED");
        rCost.setText(n.getCostEstimate() != null ? n.getCostEstimate().toPlainString() : "");
        for (Defect d : rDefectCombo.getItems()) {
            if (d.getId() == n.getDefectId()) {
                rDefectCombo.getSelectionModel().select(d);
                break;
            }
        }
    }

    private void reloadRepairs() {
        new Thread(() -> {
            try {
                List<RepairTask> list = getList("/api/repairs", RepairTask.class);
                Platform.runLater(() -> repairsTable.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private void createRepair() {
        try {
            RepairTask t = repairFromForm();
            if (t.getTitle() == null || t.getTitle().isBlank() || t.getDefectId() == null || t.getDefectId() <= 0) {
                showError("Дефект і назва обов’язкові");
                return;
            }
            new Thread(() -> {
                try {
                    post("/api/repairs", t, RepairTask.class);
                    Platform.runLater(() -> { clearRepairForm(); reloadRepairs(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void updateRepair() {
        if (selectedRepair == null) { showError("Оберіть запис"); return; }
        try {
            RepairTask t = repairFromForm();
            t.setId(selectedRepair.getId());
            new Thread(() -> {
                try {
                    put("/api/repairs/" + t.getId(), t, RepairTask.class);
                    Platform.runLater(this::reloadRepairs);
                } catch (Exception ex) {
                    Platform.runLater(() -> showError(ex.getMessage()));
                }
            }).start();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteRepair() {
        if (selectedRepair == null) { showError("Оберіть запис"); return; }
        new Thread(() -> {
            try {
                delete("/api/repairs/" + selectedRepair.getId());
                Platform.runLater(() -> { clearRepairForm(); reloadRepairs(); });
            } catch (Exception ex) {
                Platform.runLater(() -> showError(ex.getMessage()));
            }
        }).start();
    }

    private RepairTask repairFromForm() {
        RepairTask t = new RepairTask();
        Defect d = rDefectCombo.getValue();
        if (d != null) t.setDefectId(d.getId());
        t.setTitle(rTitle.getText().trim());
        t.setDescription(rDesc.getText().trim());
        t.setAssignedTo(rAssigned.getText().trim());
        t.setPlannedDate(rPlanned.getValue());
        t.setActualDate(rActual.getValue());
        t.setStatus(rStatus.getValue());
        if (!rCost.getText().isBlank()) t.setCostEstimate(new BigDecimal(rCost.getText().trim()));
        return t;
    }

    private void clearRepairForm() {
        selectedRepair = null;
        rTitle.clear(); rDesc.clear(); rAssigned.clear(); rCost.clear();
        rPlanned.setValue(null); rActual.setValue(null);
        rStatus.setValue("PLANNED");
        rDefectCombo.getSelectionModel().clearSelection();
        repairsTable.getSelectionModel().clearSelection();
    }

    private StringConverter<BuildingObject> buildingConverter() {
        return new StringConverter<>() {
            @Override public String toString(BuildingObject b) {
                return b == null ? "" : b.getId() + " — " + b.getName();
            }
            @Override public BuildingObject fromString(String s) { return null; }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}
