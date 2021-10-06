package com.dyh.LocationModify;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Environment;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
    private List<LocationStruct> locationList = new ArrayList<>();
    private int selectItem = -1;
    private MainActivity mainActivity = null;

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView ;
        RadioButton radioButton;
        View view;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;

            textView = itemView.findViewById(R.id.textView2);
            radioButton = itemView.findViewById(R.id.radioButton);
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelectItem(getAdapterPosition());
                    notifyItemRangeChanged(0, locationList.size());
                }
            };

            View.OnClickListener switchListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notifyItemRangeChanged(0, locationList.size());
                }
            };
            itemView.setOnClickListener(clickListener);
            mainActivity.findViewById(R.id.briefSwitch).setOnClickListener(switchListener);
//            textView.setOnClickListener(clickListener);
//            radioButton.setOnClickListener(clickListener);
        }
    }

    public LocationAdapter(List<LocationStruct> locationList, MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.locationList = locationList;
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File path = new File(documentsDir, "LocationModify");
        File file = new File(path, "tmp.txt");
        path.mkdirs();
        if (file.exists()){
            try {
                InputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);
                String str = new String(data);
                setSelectItem(Integer.parseInt(str));
                inputStream.close();
            } catch (IOException e) {
                setSelectItem(-1);
                e.printStackTrace();
            }
        }else {
            setSelectItem(-1);
        }

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationStruct locationStruct = locationList.get(position);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(locationStruct.getData(), 0, locationStruct.getData().length);
        parcel.setDataPosition(0);
        Location currentLocation = Location.CREATOR.createFromParcel(parcel);

        SwitchCompat briefSwitch = mainActivity.findViewById(R.id.briefSwitch);
        String textViewStr = "";
        if (briefSwitch.isChecked()){
            textViewStr = String.format(Locale.CHINA,
                    "位置%d: %s\n" +
                            "纬度:%f; 经度:%f\n\n" +
                            "%s",
                    position, locationStruct.getAlias(), currentLocation.getLatitude(),
                    currentLocation.getLongitude(), locationStruct.getName().split("\n")[0]);
        }else {
            textViewStr = String.format(Locale.CHINA,
                    "位置%d: %s\n" +
                            "纬度:%f; 经度:%f\n\n" +
                            "%s",
                    position, locationStruct.getAlias(), currentLocation.getLatitude(),
                    currentLocation.getLongitude(), locationStruct.getName());
        }


        holder.textView.setText(textViewStr);
        holder.radioButton.setChecked(position == selectItem);
    }

    @Override
    public int getItemCount() {
        if (locationList != null){
            return locationList.size();
        }else {
            return 0;
        }
    }
    public int removeItem() {
        if (selectItem>=0 && selectItem<locationList.size()){
            int reId = locationList.get(selectItem).getId();
            locationList.remove(selectItem);
            notifyItemRemoved(selectItem);
            notifyItemRangeChanged(selectItem, locationList.size());
            setSelectItem(-1);
            return reId;
        }else {
            return -1;
        }
    }

    public void addItem(LocationStruct locationStruct){
        locationList.add(locationStruct);
        notifyItemInserted(locationList.size());
    }

    public void reloadItem(List<LocationStruct> locationList){
        this.locationList = locationList;
        if (locationList != null) {
            notifyItemRangeChanged(0, locationList.size());
        }
    }

    public void changeAlias(LocationDatabase database){
        if (selectItem>=0 && selectItem<locationList.size()){
            EditText editText = new EditText(mainActivity);
            editText.setText(locationList.get(selectItem).getAlias());
            AlertDialog.Builder inputDialog =
                    new AlertDialog.Builder(mainActivity);
            inputDialog.setTitle("修改备注").setView(editText);
            inputDialog.setPositiveButton("确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            database.changeAliasById(locationList.get(selectItem).getId(), editText.getText().toString());
                            try {
                                mainActivity.reloadLocation();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            inputDialog.setNegativeButton("取消", null);
            inputDialog.show();
        }
    }

    public int getCurrentLocationId(){
        if (selectItem>=0){
            return locationList.get(selectItem).getId();
        }else {
            return -1;
        }
    }

//    public void setEnable(){
//        View view = LayoutInflater.from(mainActivity.getApplicationContext()).inflate(R.layout.layout, parent,false);
//    }

    public void setDisable(){

    }

    public void setSelectItem(int selectItem) {
        this.selectItem = selectItem;
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File path = new File(documentsDir, "LocationModify");
        File file = new File(path, "tmp.txt");
        path.mkdirs();
        try {
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(String.valueOf("1").getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        TextView indexView = mainActivity.findViewById(R.id.textView);
        indexView.setText("index: "+selectItem);
    }


    public List<LocationStruct> getLocationList() {
        return locationList;
    }
}
