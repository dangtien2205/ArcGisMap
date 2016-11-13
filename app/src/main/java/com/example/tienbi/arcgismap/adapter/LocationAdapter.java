package com.example.tienbi.arcgismap.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.example.tienbi.arcgismap.App;
import com.example.tienbi.arcgismap.R;
import com.example.tienbi.arcgismap.mode.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by TienBi on 13/09/2016.
 */
public class LocationAdapter extends ArrayAdapter<Location> {
    LayoutInflater layoutInflater;
    Activity context;
    int resource;
    ArrayList<Location> list;
    ArrayList<Location> listCopy;

    public LocationAdapter(Activity context, int resource, ArrayList<Location> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.list = objects;
        listCopy=new ArrayList<>();
        listCopy.addAll(objects);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Location getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        Holder holder;

        if (convertView == null) {
            layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(resource, viewGroup, false);
            holder = new Holder();
            holder.txtTitle = (TextView) convertView.findViewById(R.id.txtTitle);
            holder.txtDescription = (TextView) convertView.findViewById(R.id.txtDescription);

            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        Location location = getItem(i);

        holder.txtTitle.setText(location.getName_point());
        holder.txtDescription.setText(location.getDescription());

        return convertView;
    }

    private class Holder {
        TextView txtTitle;
        TextView txtDescription;
    }

    public void filter(String text) {
        list.clear();
        if(text.isEmpty()){
            list.addAll(listCopy);
        } else{
            text = text.toLowerCase();
            for(Location item: listCopy){
                if(item.getName_point().toLowerCase().contains(text) || item.getDescription().toLowerCase().contains(text)){
                    list.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}
