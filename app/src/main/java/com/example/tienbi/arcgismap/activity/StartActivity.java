package com.example.tienbi.arcgismap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.tienbi.arcgismap.R;

/**
 * Created by TienBi on 13/11/2016.
 */
public class StartActivity extends AppCompatActivity implements View.OnClickListener{
    public static final String TYPE_GAS="1";
    public static final String TYPE_ATM="2";
    public static final String TYPE_RESTAURANT="4";
    public static final String TYPE_SUPERMAKERT="3";
    public static final String TYPE_HOSPITAL="5";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_three_button);

        addControls();
    }

    private void addControls() {
        (findViewById(R.id.btnMap)).setOnClickListener(this);
        (findViewById(R.id.layoutATM)).setOnClickListener(this);
        (findViewById(R.id.layoutGas)).setOnClickListener(this);
        (findViewById(R.id.layoutHospital)).setOnClickListener(this);
        (findViewById(R.id.layoutSupermarket)).setOnClickListener(this);
        (findViewById(R.id.layoutRestaurant)).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnMap:
                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction("ALL");
                startActivity(intent);
                break;
            case R.id.layoutATM:
                intent = new Intent(this, DetailActivity.class);
                intent.setAction(TYPE_ATM);
                startActivity(intent);
                break;
            case R.id.layoutGas:
                intent = new Intent(this, DetailActivity.class);
                intent.setAction(TYPE_GAS);
                startActivity(intent);
                break;
            case R.id.layoutHospital:
                intent = new Intent(this, DetailActivity.class);
                intent.setAction(TYPE_HOSPITAL);
                startActivity(intent);
                break;
            case R.id.layoutRestaurant:
                intent = new Intent(this, DetailActivity.class);
                intent.setAction(TYPE_RESTAURANT);
                startActivity(intent);
                break;
            case R.id.layoutSupermarket:
                intent = new Intent(this, DetailActivity.class);
                intent.setAction(TYPE_SUPERMAKERT);
                startActivity(intent);
                break;
            default:
                break;
        }
    }
}