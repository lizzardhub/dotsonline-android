package com.owllabs.iter.dotsonline;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class Game extends Activity {
    private GameSurface game = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Turn title off
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main_menu);

        game = new GameSurface(this);
    }

    protected void play_game(View view) {
        setContentView(game);
    }

    protected void load_game(View view) {
        setContentView(new GameSurface(this));
    }

    protected void reset_game(View view) {

    }

    protected void quit_game(View view) {
        this.finish();
    }
}
