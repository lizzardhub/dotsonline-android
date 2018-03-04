package com.owllabs.iter.dotsonline;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by Saga on 28.10.2016.
 */


class Object2d
{
    private Bitmap image;
    public int x, y;

    public Object2d(Bitmap res) {
        this.image = res;
        this.x = 0;
        this.y = 0;
    }

    public void update() {

    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(image, x, y, null);
    }

    public void draw(Canvas canvas, int dx, int dy) {
        canvas.drawBitmap(image, x + dx, y + dy, null);
    }
}


class Point
{
    int x, y;
    boolean special = false;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(boolean special) {
        this.special = special;
    }

    public boolean equals(Point B) {
        return this.x == B.x && this.y == B.y;
    }
}


class Dot
{
    private GameSurface gameSurface;
    public int x, y, team;

    public Dot(GameSurface gameSurface, int team) {
        this.gameSurface = gameSurface;
        this.x = 0;
        this.y = 0;
        this.team = team;
    }

    public void update() {}

    public void draw(Canvas canvas) {
        if (team == 1) {
            canvas.drawBitmap(gameSurface.blue_dot, x, y, null);
        } else if (team == 2) {
            canvas.drawBitmap(gameSurface.red_dot, x, y, null);
        }
    }

    public void draw(Canvas canvas, int dx, int dy) {
        if (team == 1) {
            canvas.drawBitmap(gameSurface.blue_dot, x + dx, y + dy, null);
        } else if (team == 2) {
            canvas.drawBitmap(gameSurface.red_dot, x + dx, y + dy, null);
        }
    }
}