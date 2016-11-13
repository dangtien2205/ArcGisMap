package com.example.tienbi.arcgismap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.tienbi.arcgismap.R;
import com.example.tienbi.arcgismap.adapter.LocationAdapter;
import com.example.tienbi.arcgismap.manager.DatabaseManager;
import com.example.tienbi.arcgismap.mode.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by TienBi on 13/11/2016.
 */
public class DetailActivity extends AppCompatActivity implements View.OnClickListener, SearchView.OnQueryTextListener {
    private LocationAdapter locationAdapter;
    private ListView lvLocation;
    private ArrayList<Location> list;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spinner_listview);

        addControls();
        addEvents();
    }

    private void addEvents() {
        lvLocation.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(DetailActivity.this, MainActivity.class);
                intent.setAction("ONE_LOCATION");
                intent.putExtra("LOCATION", list.get(position));
                startActivity(intent);
            }
        });
    }

    private void addControls() {
        type = getIntent().getAction();
        int typ = Integer.parseInt(type);
        list = DatabaseManager.getInstance().getLocationByType(typ);
        locationAdapter = new LocationAdapter(this, R.layout.layout_item_location, list);
        lvLocation = (ListView) findViewById(R.id.lvLocation);
        lvLocation.setAdapter(locationAdapter);
        ((SearchView) findViewById(R.id.search_view)).setOnQueryTextListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        locationAdapter.filter(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        locationAdapter.filter(newText);
        return true;
    }
}