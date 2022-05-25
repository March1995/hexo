package _posts.java.coucurrent;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

public class TestThread extends Thread {

    public static void main(String[] args) {
        System.out.println(toBin(-1 << Integer.SIZE - 3));
        System.out.println(toBin(0 << Integer.SIZE - 3));
        System.out.println(toBin(1 << Integer.SIZE - 3));
        System.out.println(toBin(2 << Integer.SIZE - 3));
        System.out.println(toBin(3 << Integer.SIZE - 3));
        HashMap map = new HashMap();
        map.put(1, 1);
    }


    public static String toBin(int num) {
        char[] chs = new char[Integer.SIZE];
        for (int i = 0; i < Integer.SIZE; i++) {
            chs[Integer.SIZE - 1 - i] = (char) ((num >> i & 1) + '0');
        }
        return new String(chs);
    }

}
