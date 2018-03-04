package com.owllabs.iter.dotsonline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**
 * Created by Saga on 27.10.2016.
 */

public class GameSurface extends SurfaceView implements SurfaceHolder.Callback
{
    protected Bitmap blue_dot = BitmapFactory.decodeResource(getResources(), R.drawable.blue_dot);
    protected Bitmap red_dot = BitmapFactory.decodeResource(getResources(), R.drawable.red_dot);
    protected Bitmap blue_dot_shade;
    protected Bitmap red_dot_shade;
    protected Point shaded_dot;
    protected static final Integer INF = 1000000000;
    protected MainThread thread;
    protected NetworkManagerThread networkManagerThread;
    protected Object2d[][] grid;
    protected Dot[][] dots;
    protected static final Integer GRID_IMAGE_WIDTH = 30;
    protected static final Integer GRID_IMAGE_HEIGHT = 30;
    protected static final Integer DOT_IMAGE_WIDTH = 10;
    protected static final Integer DOT_IMAGE_HEIGHT = 10;

    protected final Point not_visited = new Point(-2, -2);

    protected Point shadow = not_visited;
    protected Paint wallpaint_blue;
    protected Paint wallpaint_red;
    protected Paint wallpaint_border;
    protected ArrayList<Path> konquered_polygons;
    protected ArrayList<Integer> konquered_teams;
    protected Boolean[][] locked;
    protected Integer grid_width;
    protected Integer grid_height;
    protected ArrayList<Point>[][] arcs;

    protected int game_state = 0;
    // 0 - initializing (player communication, object preparation etc.
    // 1 - Player#1's turn
    // 2 - Player#2's turn
    // TEAMS:
    // 0 - unused
    // 1 - Player#1's dots
    // 2 - Player#2's dots
    protected int current_player = 1;
    
    protected boolean onstart_reset = false;
    protected boolean onstart_load = false;

    protected Point shift = new Point(-100, -100);


    protected void log(String s) {
        //networkManagerThread.log.add(s);
    }

    protected void log(int n) {
        //networkManagerThread.log.add(Integer.toString(n));
    }

    protected boolean[][] bfs(int x, int y, boolean inner_outer, boolean[][] barrier, boolean allow_diagonal) {
        Point s = new Point(x, y);
        boolean[][] visited = new boolean[grid_height][grid_width];
        Queue<Point> q = new LinkedList<Point>();
        q.add(s);
        visited[y][x] = true;
        while (!q.isEmpty()) {
            Point v = q.remove();
            for (int i = 0; i < arcs[v.y][v.x].size(); i++) {
                Point new_v = arcs[v.y][v.x].get(i);
                if ((!locked[new_v.y][new_v.x] || inner_outer) && !visited[new_v.y][new_v.x]
                        && (allow_diagonal || new_v.x == v.x || new_v.y == v.y)
                        && (dots[new_v.y][new_v.x].team == dots[y][x].team || inner_outer && !barrier[new_v.y][new_v.x])) {
                    q.add(new_v);
                    visited[new_v.y][new_v.x] = true;
                }
            }
        }
        return visited;
    }

    protected boolean[][] polygon;
    protected Point polygon_begin;
    protected Point[][] polygon_painted;
    protected int polygon_step;
    protected boolean dfs_cycle(Point v, Point parent, int dfs_depth) {
        dfs_depth++;
        log("DFS REPORT:");
        log(v.x);
        log(v.y);
        log("DFS REPORT.");
        polygon[v.y][v.x] = false;
        polygon_painted[v.y][v.x] = parent;
        for (int i = 0; i < arcs[v.y][v.x].size(); i++) {
            Point new_v = arcs[v.y][v.x].get(i);
            if (!polygon[new_v.y][new_v.x]) {
                if (new_v.equals(polygon_begin) && !parent.equals(polygon_begin) && dfs_depth >= polygon_step) {
                    log("I'm PARENT");
                    polygon_painted[new_v.y][new_v.x] = v;
                    return true;
                }
            } else if (dfs_cycle(new_v, v, dfs_depth)) {
                return true;
            }
        }
        polygon[v.y][v.x] = true;
        return false;
    }

    // encircle also paints the polygon right away
    protected void encircle(int x, int y, boolean[][] barrier) {
        log("BARRIER:");
        for (int i = 0; i < grid_height; i++) {
            String tmp = "";
            for (int j = 0; j < grid_width; j++) {
                if (barrier[i][j]) {
                    tmp += "1";
                } else {
                    tmp += "0";
                }
            }
            log(tmp);
        }
        polygon = new boolean[grid_height][grid_width];
        polygon_begin = not_visited;
        polygon_step = 0;
        for (int i = 0; i < grid_height; i++) {
            for (int j = 0; j < grid_width; j++) {
                polygon[i][j] = false;
            }
        }
        int team = dots[y][x].team;
        boolean[][] inner = bfs(x, y, true, barrier, false);
        for (int i = 0; i < grid_height; i++) {
            for (int j = 0; j < grid_width; j++) {
                if (inner[i][j]) {
                    log("INNER FOUND:");
                    log(j);
                    log(i);
                    log("INNER END");
                    locked[i][j] = true;
                    for (int k = 0; k < arcs[i][j].size(); k++) {
                        Point v = arcs[i][j].get(k);
                        if (!locked[v.y][v.x] && (3 - dots[v.y][v.x].team) == team && !polygon[v.y][v.x]) {
                            polygon_step++;
                            polygon[v.y][v.x] = true;
                            polygon_begin = v;
                        }
                    }
                }
            }
        }

        if (polygon_begin.equals(not_visited)) {
            log("UNEXPECTED SITUATION!!!");
            return;
        }

        polygon_painted = new Point[grid_height][grid_width];
        log("DFS start, polygon:");
        for (int i = 0; i < grid_height; i++) {
            String tmp = "";
            for (int j = 0; j < grid_width; j++) {
                if (polygon[i][j]) {
                    tmp += "1";
                } else {
                    tmp += "0";
                }
            }
            log(tmp);
        }

        log("DFS END.");
        if (polygon_begin.equals(not_visited) || !dfs_cycle(polygon_begin, not_visited, 0)) {
            log("Something weird happened, or was it planned?");
            return;
        }
        // Painting!:)
        Path wallpath = new Path();
        Point A = polygon_begin;
        Dot A_real = dots[A.y][A.x];
        wallpath.moveTo(A_real.x + DOT_IMAGE_WIDTH + shift.x, A_real.y + DOT_IMAGE_HEIGHT + shift.y); // used for first point
        Point B = polygon_painted[A.y][A.x];
        Dot B_real = dots[B.y][B.x];
        while (!(A.equals(B))) {
            wallpath.lineTo(B_real.x + DOT_IMAGE_WIDTH + shift.x, B_real.y + DOT_IMAGE_HEIGHT + shift.y);
            B = polygon_painted[B.y][B.x];
            B_real = dots[B.y][B.x];
        }
        wallpath.lineTo(A_real.x + DOT_IMAGE_WIDTH + shift.x, A_real.y + DOT_IMAGE_HEIGHT + shift.y); // there is a setLastPoint action but it doesn't work as expected

        konquered_polygons.add(wallpath);
        konquered_teams.add(3 - team);
    }

    protected void point_update(int x, int y) {
        // Print Locked
        for (int i = 0; i < grid_height; i++) {
            String tmp = "";
            for (int j = 0; j < grid_width; j++) {
                if (locked[i][j]) {
                    tmp += "1";
                } else {
                    tmp += "0";
                }
            }
            log(tmp);
        }

        if (dots[y][x].team == 0 && !locked[y][x]) {
            log("newteam");
            log(game_state);
            // Place a new dot
            dots[y][x].team = game_state;

            // Chains
            boolean[][] barrier = new boolean[grid_height][grid_width];
            boolean[][] middle = bfs(x, y, false, barrier, true);
            for (int i = 0; i < grid_height; i++) {
                for (int j = 0; j < grid_width; j++) {
                    if (middle[i][j]) {
                        barrier[i][j] = true;
                        log("BARRIER:");
                        log(j);
                        log(i);
                        log("END.");
                    }
                }
            }
            log("************************");
            boolean[][] outer = bfs(0, 0, true, barrier, false);
            for (int i = 0; i < grid_height; i++) {
                for (int j = 0; j < grid_width; j++) {
                    if (outer[i][j]) {
                        barrier[i][j] = true;
                    }
                }
            }
            for (int i = 0; i < grid_height; i++) {
                for (int j = 0; j < grid_width; j++) {
                    if (!locked[i][j] && !barrier[i][j] && dots[i][j].team != 0) {
                        log("ENCIRCLE");
                        log(i);
                        log(j);
                        encircle(j, i, barrier);
                    }
                }
            }

        }
    }

    public Bitmap makeTransparent(Bitmap src, int value) {
        // Copied from http://stackoverflow.com/questions/5118894/how-to-change-a-bitmaps-opacity
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap transBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(transBitmap);
        canvas.drawARGB(0, 0, 0, 0);
        // Config paint
        final Paint paint = new Paint();
        paint.setAlpha(value);
        canvas.drawBitmap(src, 0, 0, paint);
        return transBitmap;
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("players.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(Context context) {

        String read = "";

        try {
            InputStream inputStream = context.openFileInput("players.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                read = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return read;
    }

    public GameSurface (Context context) {
        super(context);

        // Add callback for touches, mouse, etc.
        getHolder().addCallback(this);
        thread = new MainThread(getHolder(), this);
        networkManagerThread = new NetworkManagerThread(this);


        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        // INITIALIZE
        grid_width = getWidth() / GRID_IMAGE_WIDTH + 4;
        grid_height = getHeight() / GRID_IMAGE_HEIGHT + 4;
        blue_dot = Bitmap.createScaledBitmap(blue_dot, DOT_IMAGE_WIDTH * 2, DOT_IMAGE_HEIGHT * 2, false);
        red_dot = Bitmap.createScaledBitmap(red_dot, DOT_IMAGE_WIDTH * 2, DOT_IMAGE_HEIGHT * 2, false);
        blue_dot_shade = makeTransparent(blue_dot, 70);
        red_dot_shade = makeTransparent(red_dot, 70);
        shaded_dot = new Point(-1, -1);
        wallpaint_blue = new Paint();
        wallpaint_blue.setColor(Color.BLUE);
        wallpaint_blue.setAlpha(50);
        wallpaint_blue.setStyle(Paint.Style.FILL);
        wallpaint_red = new Paint();
        wallpaint_red.setColor(Color.RED);
        wallpaint_red.setAlpha(50);
        wallpaint_red.setStyle(Paint.Style.FILL);
        wallpaint_border = new Paint();
        wallpaint_border.setColor(Color.BLACK);
        wallpaint_border.setStyle(Paint.Style.STROKE);
        wallpaint_border.setStrokeWidth(2.f);
        konquered_polygons = new ArrayList<Path>();
        konquered_teams = new ArrayList<Integer>();
        locked = new Boolean[grid_height][grid_width];
        grid = new Object2d[grid_height][grid_width];
        dots = new Dot[grid_height][grid_width];
        arcs = new ArrayList[grid_height][grid_width];
        for (int i = 0; i < grid_height; i++) {
            for (int j = 0; j < grid_width; j++) {
                grid[i][j] = new Object2d(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.grid), GRID_IMAGE_WIDTH, GRID_IMAGE_HEIGHT, false));
                grid[i][j].x = j * GRID_IMAGE_WIDTH;
                grid[i][j].y = i * GRID_IMAGE_HEIGHT;

                dots[i][j] = new Dot(this, 0);
                dots[i][j].x = (j - 1) * GRID_IMAGE_WIDTH - DOT_IMAGE_WIDTH;
                dots[i][j].y = (i - 1) * GRID_IMAGE_HEIGHT - DOT_IMAGE_HEIGHT;

                locked[i][j] = false;

                // Initialize the graph - arcs
                Point u = new Point(j, i);
                ArrayList<Point> v = new ArrayList<Point>();
                if (u.x - 1 >= 0) {
                    if (u.y - 1 >= 0) {
                        v.add(new Point(u.x - 1, u.y - 1));
                    }
                    if (u.y + 1 < grid_height) {
                        v.add(new Point(u.x - 1, u.y + 1));
                    }
                }
                if (u.x + 1 < grid_width) {
                    if (u.y - 1 >= 0) {
                        v.add(new Point(u.x + 1, u.y - 1));
                    }
                    if (u.y + 1 < grid_height) {
                        v.add(new Point(u.x + 1, u.y + 1));
                    }
                }

                if (u.x - 1 >= 0) {
                    v.add(new Point(u.x - 1, u.y));
                }
                if (u.y - 1 >= 0) {
                    v.add(new Point(u.x, u.y - 1));
                }
                if (u.y + 1 < grid_height) {
                    v.add(new Point(u.x, u.y + 1));
                }
                if (u.x + 1 < grid_width) {
                    v.add(new Point(u.x + 1, u.y));
                }
                arcs[u.y][u.x] = v;
            }
        }

        thread.setRunning(true);
        thread.start();
        networkManagerThread.setRunning(true);
        networkManagerThread.start();

        if (onstart_reset) {
            networkManagerThread.send("reset");
        }
        System.out.println(readFromFile(getContext()));
        writeToFile("Helloworld", getContext());

        game_state = 1;
        if (current_player != game_state) {
            networkManagerThread.receive();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN && game_state == current_player) {
            int touch_x = (int) event.getX();
            int touch_y = (int) event.getY();
            int dot_x = (touch_x + GRID_IMAGE_WIDTH / 2) / GRID_IMAGE_WIDTH * GRID_IMAGE_WIDTH;
            int dot_y = (touch_y + GRID_IMAGE_HEIGHT / 2) / GRID_IMAGE_HEIGHT * GRID_IMAGE_HEIGHT;
            int x = dot_x / GRID_IMAGE_WIDTH + 1;
            int y = dot_y / GRID_IMAGE_HEIGHT + 1;
            Point touch = new Point(x, y);
            if (!shadow.equals(touch) && !locked[y][x]) {
                shadow = touch;
                shaded_dot.x = dot_x - DOT_IMAGE_WIDTH;
                shaded_dot.y = dot_y - DOT_IMAGE_HEIGHT;
            } else {
                // Actual dot placed
                networkManagerThread.send(new Point(x, y));
                networkManagerThread.receive();
                point_update(x, y);
                // Next turn
                game_state = 3 - game_state;
                shaded_dot.x = -1;
                shaded_dot.y = -1;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    public void update() {
        if (game_state == (3 - current_player) && networkManagerThread.received && !networkManagerThread.receiving) {
            point_update(networkManagerThread.receive.x, networkManagerThread.receive.y);
            // Next turn
            game_state = 3 - game_state;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        /*for (int i = 0; i < grid_height; i++) {
            for (int j = 0; j < grid_width; j++) {
                grid[i][j].draw(canvas, shift.x, shift.y);
            }
        }*/
        if (shaded_dot.x >= 0 && shaded_dot.y >= 0) {
            if (game_state == 1) {
                canvas.drawBitmap(blue_dot_shade, shaded_dot.x + shift.x, shaded_dot.y + shift.y, null);
            } else {
                canvas.drawBitmap(red_dot_shade, shaded_dot.x + shift.x, shaded_dot.y + shift.y, null);
            }
        }
        for (int i = 0; i < konquered_polygons.size(); i++) {
            if (konquered_teams.get(i) == 1) {
                canvas.drawPath(konquered_polygons.get(i), wallpaint_blue);
            } else {
                canvas.drawPath(konquered_polygons.get(i), wallpaint_red);
            }
            canvas.drawPath(konquered_polygons.get(i), wallpaint_border);
        }
        for (int i = 0; i < grid_height; i++) {
            for (int j = 0; j < grid_width; j++) {
                dots[i][j].draw(canvas, shift.x, shift.y);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        boolean retry = true;
        while (retry)
        {
            try
            {
                thread.setRunning(false);
                thread.join();
                networkManagerThread.setRunning(false);
                networkManagerThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}