package com.b.hundredmiles;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class StartUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_start_up);
        Thread start_thread=new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Thread.sleep(2000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                Intent intent=new Intent(getApplicationContext(),MainActivity.class);//启动MainActivity
                startActivity(intent);
                finish();//关闭当前活动

            }
        });
        start_thread.start();
    }

    public void start_search_activity(View view){

    }
}