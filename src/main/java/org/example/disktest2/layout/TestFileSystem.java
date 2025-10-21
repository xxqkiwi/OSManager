package org.example.disktest2.layout;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.disktest2.Controller.FileModel;
import org.example.disktest2.Controller.OSManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestFileSystem implements Initializable {
    @FXML
    private TextArea inputArea;
    @FXML
    private GridPane diskPane;
    @FXML
    private GridPane FatTitlePane;
    @FXML
    private GridPane FatPane;
   /* public static void main(String[] args) {
        try{
            OSManager manager = new OSManager();
            menu(manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

    @FXML
    private TreeView<String> dirTree;   // 放目录树中目录和文件的名字

    /** 只在 UI 初始化时调用一次 */
    private void initializeTree(OSManager osManager){
        TreeItem<String> rootItem = createNode(osManager.getRoot());
        dirTree.setRoot(rootItem);
        dirTree.setShowRoot(true);

    }

    /** 递归建节点 */
    private TreeItem<String> createNode(FileModel model){
        TreeItem<String> item = new TreeItem<>(model.getName());
        if(model.getAttr() == 3){       // 目录
            for(FileModel child : model.subMap.values()){
                item.getChildren().add(createNode(child));
            }
        }
        return item;
    }

    private void refreshTree(OSManager osManager){
        dirTree.setRoot(createNode(osManager.getRoot()));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        OSManager manager = new OSManager();
        menu(manager);
        initDisk();
        checkDisk(manager);
        initFat(manager);
        checkFat(manager);
        initializeTree(manager);
    }

    public void menu(OSManager manager) {
        /*Scanner sc = new Scanner(System.in);
        String str = null;
        System.out.println("***********" + "欢迎使用文件模拟操作系统" + "***********");
        System.out.println();*/
        manager.showFile();

        inputArea.setPrefColumnCount(10);
        inputArea.setPrefRowCount(15);
        inputArea.setWrapText(true);
        inputArea.setEditable(true);
        inputArea.setPromptText("请输入命令");
        //System.out.println("请输入命令（输入help查看命令表）：");
        inputArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    // 当按下回车键时，执行的操作
                    String str = inputArea.getText();

                    if (str.equals("exit")) {
                        System.out.println("感谢您的使用！");
                    }

                    int res = 0;

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("指令错误");
                    alert.setHeaderText("您所输入的命令有误，请检查。");
                    alert.setContentText("请重新输入");
                    String[] strs = parseCommand(str);
                    switch (strs[0]) {
                        case "create":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.createFileByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("当前目录下已存在同名文件，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2){
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("当前磁盘空间已满。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "delete":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.deleteFileByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if (res == 2) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("该目录不为空，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("当前磁盘为空，请检查。");
                                    alert.showAndWait();
                                } else if(res == 4) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "type":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.typeFile(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3){
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("当前操作对象不是文件是目录，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "copy":
                            if (strs.length < 3) {
                                alert.showAndWait();
                            } else {
                                res = manager.copyFile(strs[1], strs[2]);
                                if(res == 1) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("源文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("目标路径错误，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("磁盘空间不足，请检查。");
                                    alert.showAndWait();
                                } else if(res == 4) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("目标文件已存在，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "mkdir":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.createDirectoryByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("目录已存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("磁盘空间不足，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "rmdir":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.removeDirectoryByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("目录不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("目录不为空，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        /*case "help": {
                            System.out.println("命令如下");
                            inputArea.setText("命令如下（空格不能省略）：");
                            break;
                        }*/
                        default: {
                            for (String st : strs)
                                System.out.println(st);
                            //System.out.println("您所输入的命令有误，请检查！");
                            alert.showAndWait();
                        }
                    }
                    //System.out.println("请输入命令（输入help查看命令表）：");
                    inputArea.clear();
                    inputArea.setPromptText("请输入命令：");
                    checkDisk(manager);
                    checkFat(manager);
                    refreshTree(manager);//目录树刷新
                }

            }
        });

    }

    // 解析命令格式，支持路径
    public static String[] parseCommand(String str) {
        String[] parts = str.trim().split("\\s+");
        ArrayList<String> result = new ArrayList<>();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public void initDisk() {
        diskPane.setHgap(12);
        diskPane.setVgap(12);
        for(int i = 0; i <8; i++) {
            for(int j = 0; j < 16; j++) {
                //System.out.println("blockPane");
                Rectangle rectangle = new Rectangle(16, 16);
                rectangle.setAccessibleText("i*j");
                if(i == 0 && j <= 1) {
                    rectangle.setFill(Color.RED); //前两块始终被占用
                } else {
                    rectangle.setFill(Color.GREEN);
                }

                Text text = new Text((i * 8 + j) + ""); // 计算序号
                text.setStyle("-fx-font-size: 12px; -fx-text-fill: white;");

                diskPane.add(rectangle, j, i);
                diskPane.add(text, j, i);
            }
        }
    }

    //把已被占用的磁盘块变为红色
    public void checkDisk(OSManager osManager) {
        int[] fat = osManager.getFat();
        for(int i = 2; i < 128 ; i++) {
            Rectangle rectangle = getRectangle(diskPane, i/16, i%16);
                if(fat[i] != 0) {
                    rectangle.setFill(Color.RED);
                } else {
                    rectangle.setFill(Color.GREEN);
                }
            }
    }

    public Rectangle getRectangle(GridPane gridPane, int row, int col) {
        // 遍历GridPane中的所有子节点
        for (Node node : gridPane.getChildren()) {
            // 获取节点在GridPane中的位置
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            // 检查位置是否匹配
            if (rowIndex != null && colIndex != null && rowIndex.equals(row) && colIndex.equals(col)) {
                // 如果匹配，检查节点是否为Rectangle
                if (node instanceof Rectangle) {
                    return (Rectangle) node;
                }
            }
        }
        return null; // 如果没有找到匹配的Rectangle，返回null
    }

    //初始化FAT表
    public void initFat(OSManager manager) {
        FatTitlePane.add(new Text("index"),0,0);
        FatTitlePane.add(new Text("next"),1,0);
        FatTitlePane.add(new Text("name"),2,0);

        ScrollPane scrollPane = new ScrollPane(FatPane);
        scrollPane.setFitToWidth(true); // 使 GridPane 的宽度适应 ScrollPane
        scrollPane.setFitToHeight(true);
        FatPane.setVgap(3);
        FatPane.setHgap(8);

        int[] fat = manager.getFat();
        for (int i = 0; i < 128; i++) {
            FatPane.add(new Text(""+i),0,i);
            FatPane.add(new Text(""+fat[i]),1,i);
            //System.out.println("Adding index " + i + " with value " + fat[i]);
        }
    }

    //FAT表状态改变
    public void checkFat(OSManager manager) {
        Map<String, FileModel> totalFiles = manager.getTotalFiles();
        int[] fat = manager.getFat();

        // 先清空FAT显示区域
        FatPane.getChildren().clear();

        // 重新初始化FAT表显示
        for (int i = 0; i < 128; i++) {
            FatPane.add(new Text(""+i),0,i);
            FatPane.add(new Text(""+fat[i]),1,i);
        }

        // 为每个文件添加名称显示
        for(FileModel fileModel : totalFiles.values()) {
            int i = fileModel.getStartNum();
            int count = 0; // 防止死循环的计数器
            int maxIterations = 128; // 最大迭代次数

            while(fat[i] != -1 && count < maxIterations) {
                // 更新FAT表显示
                Node node = FatPane.getChildren().get(i * 3 + 1); // 获取next列
                if(node != null) {
                    FatPane.getChildren().remove(node);
                }
                FatPane.add(new Text(""+fat[i]),1,i);

                // 添加文件名显示
                Node nameNode = FatPane.getChildren().get(i * 3 + 2); // 获取name列
                if(nameNode != null) {
                    FatPane.getChildren().remove(nameNode);
                }
                FatPane.add(new Text(""+fileModel.getName()),2,i);

                i = fat[i];
                count++;
            }

            // 处理最后一个节点
            if(count < maxIterations) {
                Node node = FatPane.getChildren().get(i * 3 + 1);
                if(node != null) {
                    FatPane.getChildren().remove(node);
                }
                FatPane.add(new Text(""+fat[i]),1,i);

                Node nameNode = FatPane.getChildren().get(i * 3 + 2);
                if(nameNode != null) {
                    FatPane.getChildren().remove(nameNode);
                }
                FatPane.add(new Text(""+fileModel.getName()),2,i);
            }
        }
    }


}
