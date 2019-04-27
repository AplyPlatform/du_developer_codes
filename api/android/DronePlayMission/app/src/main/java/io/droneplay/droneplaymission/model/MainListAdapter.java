package io.droneplay.droneplaymission.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import java.util.ArrayList;

import io.droneplay.droneplaymission.R;

public class MainListAdapter extends BaseAdapter {

    public enum CLICK_TYPE {
        RUN, EDIT, DELETE
    }

    private ArrayList<MainListItem> listViewItemList = new ArrayList<MainListItem>() ;
    private ListBtnClickListener listBtnClickListener ;

    public MainListAdapter(ListBtnClickListener clickListener) {
        super();

        listBtnClickListener = clickListener;
    }

    public interface ListBtnClickListener {
        void onListBtnClick(CLICK_TYPE kind, String buttonId) ;
    }

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int i) {
        return listViewItemList.get(i) ;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        final Context context = parent.getContext();

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.layout_mainlist, parent, false);
        }

        final MainListItem item = listViewItemList.get(pos);

        Button btnMission = (Button) convertView.findViewById(R.id.btnMission) ;
        btnMission.setText(item.name);
        btnMission.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                listBtnClickListener.onListBtnClick(CLICK_TYPE.RUN, item.name);
            }
        });

        Button btnModify = (Button) convertView.findViewById(R.id.btnModify) ;
        btnModify.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                listBtnClickListener.onListBtnClick(CLICK_TYPE.EDIT, item.name);
            }
        });

        Button btnDelete = (Button) convertView.findViewById(R.id.btnDelete) ;
        btnDelete.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                listBtnClickListener.onListBtnClick(CLICK_TYPE.DELETE, item.name);
            }
        });

        return convertView;
    }

    public ArrayList<MainListItem> getList() {
        return listViewItemList;
    }

    public void setItems(ArrayList<MainListItem> items) {
        listViewItemList = items;
    }

    public void addItem(String name) {
        MainListItem item = new MainListItem();

        item.name  = name;

        listViewItemList.add(item);
    }

    public void removeItem(String name) {
        for(int i=0;i<listViewItemList.size();i++) {
            MainListItem item = listViewItemList.get(i);
            if (item.name == name) {
                listViewItemList.remove(i);
                return;
            }
        }
    }
}
