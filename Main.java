package sample;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends Application {

    String preFix = "data:image/png;base64,";

    private Label errorText = null;
    private TextField imagePathText = null;
    private TextField jsonPathText = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        GridPane gridpane = new GridPane();
        gridpane.setAlignment(Pos.CENTER);   //居中对齐
        gridpane.setPadding(new Insets(11, 12, 13, 14));
        gridpane.setHgap(6);
        gridpane.setVgap(6);

        gridpane.add(new Label("主路径 :"), 0, 0);  // columnindex, rowindex
        jsonPathText = new TextField();
        gridpane.add(jsonPathText, 1, 0);
        Button jsonPathBtn = new Button("选择");
        gridpane.add(jsonPathBtn, 2, 0);

        Button btAdd = new Button("一键转换");
        gridpane.add(btAdd, 1, 2);
        GridPane.setHalignment(btAdd, HPos.RIGHT);

        errorText = new Label("");
        errorText.setStyle("-fx-text-fill:red");
        gridpane.add(errorText, 1, 4);
        GridPane.setHalignment(errorText, HPos.RIGHT);
        GridPane.setMargin(errorText, new Insets(20, 0, 0, 0));

        primaryStage.setTitle("图片转字符工具");
        primaryStage.setScene(new Scene(gridpane, 400, 275));
        primaryStage.show();

        jsonPathBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                DirectoryChooser fileChooser = new DirectoryChooser();
                File selectedFile = fileChooser.showDialog(primaryStage);
                fileChooser.setTitle("选择主路径");
                if (selectedFile == null) {
                    return;
                }
                jsonPathText.setText(selectedFile.getPath());
            }
        });
        btAdd.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                convert(primaryStage);
            }
        });

    }

    private void convert(Stage stage) {
        String jsonPath = jsonPathText.getText();
        if (isEmpty(jsonPath)) {
            setErrorMessage("json 路径没有设置");
            return;
        }
        File file = new File(jsonPath);
        if (file == null) {
            setErrorMessage("请选择正确文件路径");
            return;
        }
        try {
            System.out.println("---------------》开始遍历文件");
            listDirectory(file);
            System.out.println("---------------》遍历结束！");
            setErrorMessage("转换成功!");
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage("出现错误，联系0004640");
        }
    }

    private void convertInner(File jsonFile, String imagePath) {
        try {
            if (isEmpty(jsonFile.getName())) {
                setErrorMessage("请选择正确 json 文件");
                return;
            }
            if (!jsonFile.getName().contains(".json")) {
                setErrorMessage("请选择正确 json 文件");
                return;
            }
            String content = FileUtils.fileRead(jsonFile);
            JSONObject jsonObject = JSON.parseObject(content);
            if (jsonObject == null) {
                setErrorMessage("json 文件解析错误");
                return;
            }
            JSONArray jsonArray = jsonObject.getJSONArray("assets");
            if (jsonArray == null) {
                setErrorMessage("json 文件解析错误");
                return;
            }
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject1 = (JSONObject) jsonArray.get(i);
                String image = jsonObject1.getString("p");
                if (isEmpty(image)) {
                    continue;
                }
                if (image.contains(preFix)){
                    setErrorMessage("json已经全部转换过了");
                    continue;
                }
                File fileInner = new File(imagePath + File.separator + image);
                if (fileInner == null || !fileInner.exists()) {
                    setErrorMessage(image + "图片获取不到！");
                    return;
                }
                String imageJsonIGet = FileUtils.imageToString(fileInner);
                if (isEmpty(imageJsonIGet)) {
                    setErrorMessage(image + "图片转json失败");
                    return;
                }
                jsonObject1.put("p", preFix + imageJsonIGet);
                System.out.println(image + " : " + preFix + imageJsonIGet);
            }
            String fileName = jsonFile.getPath();
            if (jsonFile.isFile()) {
                jsonFile.delete();
            }
            File file1 = new File(fileName);
            if (!file1.exists()) {
                file1.createNewFile();
            }
            FileWriter fr = new FileWriter(file1);
            content = JSON.toJSONString(jsonObject);
            char[] cs = content.toCharArray();
            fr.write(cs);
            fr.flush();
            fr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listDirectory(File dir) throws IOException {
        if (!dir.exists())
            throw new IllegalArgumentException("目录：" + dir + "不存在.");
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + "不是目录。");
        }
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    listDirectory(file);
                } else {
                    if (file.getName().contains(".json")) {
                        String imagePath = file.getParentFile() + File.separator + "images";
                        File imageFile = new File(imagePath);
                        if (imageFile.exists()) {
                            convertInner(file, imagePath);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private boolean isEmpty(String text) {
        return text == null || text.isEmpty();
    }

    private void setErrorMessage(String content) {
        errorText.setText(content);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        errorText.setText("");
                    }
                });
            }
        }, 2000);
    }
}
