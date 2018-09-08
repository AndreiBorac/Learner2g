/***
 * EtchTest.java
 * copyright (c) 2011 by andrei borac and silviu borac
 ***/

package gotm.etch.test;

import gotm.etch.*;

import zs42.parts.*;

import java.util.*;
import java.util.concurrent.*;

import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;

import javax.swing.*;

public class EtchTest
{
  
  static class GenerateEventsTest
  {
    static final int SIZE_W = 1280;
    static final int SIZE_H =  768;
    static final int PAGE_COUNT = 5;
    static final int DEFAULT_UPDATE_INTERVAL = 25;
    
    public static void main(String[] args) throws Exception
    {
      final ArrayList<byte[]> backdrops = (new ArrayList<byte[]>());
      
      try {
        File file = (new File("../../local/lr.png"));
        FileInputStream fis = (new FileInputStream(file));
        int file_length = (int)(file.length());
        byte[] bytes = (new byte[file_length]);
        int off = 0;
        while (off < file_length) {
          int readc = fis.read(bytes, off, file_length - off);
          if (readc < 0) throw null;
          off += readc;
        }
        for (int i = 0; i < PAGE_COUNT; i++) backdrops.add(bytes);
      } catch (IOException e) {
        throw null;
      }
      
      final LinkedBlockingQueue<Etch.OrganicInputEvent> capture = (new LinkedBlockingQueue<Etch.OrganicInputEvent>());
      
      final Etch etch = (new Etch(SIZE_H, SIZE_W, capture, DEFAULT_UPDATE_INTERVAL));
      
      (new Thread()
        {
          public void run()
          {
            try { Thread.sleep(10000); } catch (InterruptedException e) { }
            etch.appendBackdrop(backdrops.get(0));
          }
        }).start();
      
      final JFrame frame = (new JFrame("Etch"));
      
      frame.getContentPane().add(etch.getInterfaceElement());
      frame.pack();
      frame.setVisible(true);
      
      (new Thread() {
          public void run()
          {
            try {
              while (true) {
                Etch.OrganicInputEvent event = capture.take();
                etch.submit(event);
              }
            } catch (Throwable e) {
              e.printStackTrace();
              Runtime.getRuntime().halt(1);
            }
          }
        }).start();
    }
  }
  
  static class DrawingCurvesTest
  {
    static final int SIZE_W = 1280;
    static final int SIZE_H =  768;
    static final double AVG_RAD = 10;
    static final double MAX_EXP_FACTOR = 0.5f;
    static final int POLY_SEGC = 6;
    static final double DELTA_PHI = 2 * Math.PI / (4 * POLY_SEGC);
    static final Color color = Color.RED;
    static final double stroke_width = 4;
    static final long DURATION = (long)(30e9);
    
    static ArrayList<Point2D.Double> logSpiral(double xc, double yc, double start_r, double start_phi, double exp)
    {
      ArrayList<Point2D.Double> points= (new ArrayList<Point2D.Double>());
      int point_idx = 0;
      points.add(new Point2D.Double(xc + start_r * Math.cos(start_phi),
                                    yc + start_r * Math.sin(start_phi)));
      point_idx++;
      for (int i = 0; i < POLY_SEGC; i++) {
        for (int j = 0; j < 3; j++) {
          double r = start_r * Math.exp(exp * point_idx * DELTA_PHI);
          double phi = start_phi + point_idx * DELTA_PHI;
          points.add(new Point2D.Double(xc + r * Math.cos(phi),
                                        yc + r * Math.sin(phi)));
          point_idx++;
        }
      }
      return points;
    }
    
    public static void main(String[] args) throws Exception
    {
      System.out.print("args: length " + args.length);
      for (String arg : args) System.out.print(" " + arg);
      System.out.println();
      
      final int spiralc = Integer.parseInt(args[0]);
      final int default_update_interval = Integer.parseInt(args[1]);
      final long seed = (long)(Integer.parseInt(args[2]));
      
      System.out.println("spiralc " + spiralc + " default_update_interval " + default_update_interval + " seed " + seed);
      
      final ArrayList<byte[]> backdrops = (new ArrayList<byte[]>());

      try {
        File file = (new File("../../local/lr.png"));
        FileInputStream fis = (new FileInputStream(file));
        int file_length = (int)(file.length());
        byte[] bytes = (new byte[file_length]);
        int off = 0;
        while (off < file_length) {
          int readc = fis.read(bytes, off, file_length - off);
          if (readc < 0) throw null;
          off += readc;
        }
        backdrops.add(bytes);
      } catch (IOException e) {
        throw null;
      }

      final Etch etch = (new Etch(SIZE_H, SIZE_W, null, default_update_interval));
      etch.appendBackdrop(backdrops.get(0));
    
      final JFrame frame = (new JFrame("Etch"));
      
      frame.getContentPane().add(etch.getInterfaceElement());
      frame.pack();
      frame.setVisible(true);
      
      if (true) {
        (new Thread()
          {
            public void run()
            {
              try {
                final Random rand_gen = (new Random());
                rand_gen.setSeed(seed);
                
                int total_pointc = spiralc * (3 * POLY_SEGC + 1);
                long delta_t = (long)(DURATION / (double)total_pointc);
                System.err.println("delta_t " + delta_t);
                int point_idx = 0;

                long now = MicroTime.now();
                long start_time = now;
                long time = now;
                //etch.submit((new BackdropInputEvent(now, 0)));
                
                for (int i = 0; i < spiralc; i++) {
                  double xc = SIZE_W * rand_gen.nextDouble();
                  double yc = SIZE_H * rand_gen.nextDouble();
                  double start_r = AVG_RAD * (0.5f + rand_gen.nextDouble());
                  double start_phi = 2.0f * Math.PI * rand_gen.nextDouble();
                  double exp = MAX_EXP_FACTOR * rand_gen.nextDouble();
                  ArrayList<Point2D.Double> points = logSpiral(xc, yc, start_r, start_phi, exp);
                  int pointc = points.size();
                  time +=delta_t;
                  etch.submit(new Etch.FirstTabletInputEvent(time, points.get(0).y, points.get(0).x));
                  for (int j = 1; j < pointc - 1; j++) {
                    time += delta_t;
                    etch.submit(new Etch.PointTabletInputEvent(time, points.get(j).y, points.get(j).x));
                  }
                  time += delta_t;
                  etch.submit(new Etch.CloseTabletInputEvent(time, points.get(pointc-1).y, points.get(pointc-1).x));
                }
                Thread.sleep((long)(DURATION/1e6));
                System.exit(0);
              } catch (Throwable e) {
                throw null;
              }
            }
          }).start();
      }
    }
  }
}
