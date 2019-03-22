package com.major.nio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NioDemo {

    public static void main(String[] args) {
        method2();
    }

    public static void method2() {
        try {
            FileInputStream fis = new FileInputStream("./README.md");
            FileOutputStream fos = new FileOutputStream("./copy_readme.md");

            FileChannel readFC = fis.getChannel();
            FileChannel writeFC = fos.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while (true) {
                buffer.clear();
                int len = readFC.read(buffer);
                if (len == -1) {
                    break;
                }
                buffer.flip();
                writeFC.write(buffer);
            }

            readFC.close();
            writeFC.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
