package com.owllabs.iter.dotsonline;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.hardware.camera2.TotalCaptureResult;
import android.provider.Settings;
import android.view.SurfaceHolder;

/**
 * Created by Saga on 27.10.2016.
 */
public class MainThread extends Thread
{
    private int FPS = 10;
    private float averageFPS = 0;
    private SurfaceHolder surfaceHolder;
    private GameSurface gameSurface;
    private boolean running;
    public static Canvas canvas;

    public MainThread(SurfaceHolder surfaceHolder, GameSurface gameSurface)
    {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gameSurface = gameSurface;
    }

    @Override
    public void run()
    {
        long startTime, timeMillis, waitTime, totalTime, targetTime;
        totalTime = 0;
        int frameCount = 0;
        targetTime = 1000 / FPS;

        while (running) {
            startTime = System.nanoTime();
            canvas = null;

            try
            {
                canvas = this.surfaceHolder.lockCanvas();
                synchronized (surfaceHolder)
                {
                    this.gameSurface.update();
                    this.gameSurface.draw(this.canvas);
                }
            } catch (Exception e) {}
            finally
            {
                if (canvas != null) {
                    try
                    {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }


            timeMillis = (System.nanoTime() - startTime) / 1000000;
            waitTime = targetTime - timeMillis;
            try
            {
                this.sleep(waitTime);
            } catch (Exception e) {};

            totalTime += System.nanoTime() - startTime;
            frameCount++;
            if (frameCount == FPS) {
                averageFPS = frameCount / (1f * totalTime / 1000000000);
                //System.out.println(averageFPS);
                frameCount = 0;
                totalTime = 0;
            }
        }
    }

    public void setRunning(boolean flag) {
        running = flag;
    }
}
