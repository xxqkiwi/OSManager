package org.example.disktest2.Controller;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileModel { //文件或目录属性

    public Map<String,FileModel> subMap = new HashMap<String, FileModel>(); //存当前目录的子目录或子文件
    private String name;        //名
    private String type;      //类型
    private int size;        //大小
    private int attr;        //文件还是目录
    private int startNum;    //FAT表中的起始块
    private FileModel father = null;  //上级目录
    private FileOutputStream fos = null;  // 实际的流式文件

    public FileModel(String name, String type, int startNum, int size){
        this.name = name;
        this.type = type;
        this.attr = 2;
        this.startNum = startNum;
        this.size = size;
    }

    public FileModel(String name, int startNum){
        this.name = name;
        this.type = " ";
        this.attr = 3;
        this.startNum = startNum;
        this.size = 7;
    }

    public void setFos(String filePath){
        try(FileOutputStream fos = new FileOutputStream(filePath)) {
            this.fos = fos;
        } catch (IOException e) {
            throw new RuntimeException("setFos失败：" + e);
        }
    }

    public FileOutputStream getFos() {
        return fos;
    }

    @Override
    public String toString() {
        return name;   // 只返回名字即可
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getAttr() {
        return attr;
    }

    public void setAttr(int attr) {
        this.attr = attr;
    }

    public int getStartNum() {
        return startNum;
    }

    public void setStartNum(int startNum) {
        this.startNum = startNum;
    }

    public FileModel getFather() {
        return father;
    }

    public void setFather(FileModel father) {
        this.father = father;
    }
}
