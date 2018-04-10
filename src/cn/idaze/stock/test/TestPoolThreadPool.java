/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.idaze.stock.test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author HHQ
 */
public class TestPoolThreadPool implements Runnable {

    private String name;

    public TestPoolThreadPool(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(5000);
            System.out.println(name + "||" + this.hashCode());
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    public static void main(String[] args) {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable> ();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, queue);
        
        for(int i = 0; i < 50; i++){
            TestPoolThreadPool t = new TestPoolThreadPool("第" + i + "个线程~");
            queue.add(t);
        }
        
        
        exec.shutdown();
    }
}
