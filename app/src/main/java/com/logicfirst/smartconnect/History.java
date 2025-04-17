package com.logicfirst.smartconnect;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class History extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<DeviceHistory> deviceHistoryList;

    public History() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        deviceHistoryList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(deviceHistoryList);
        recyclerView.setAdapter(adapter);

        // Fetch Bluetooth and Wi-Fi history
        fetchBluetoothHistory();
        fetchWiFiHistory();

        // Listen to real-time Bluetooth connect/disconnect
        IntentFilter bluetoothFilter = new IntentFilter();
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        getActivity().registerReceiver(bluetoothReceiver, bluetoothFilter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(bluetoothReceiver);
    }

    private void fetchBluetoothHistory() {
        Context context = getContext();
        if (context == null) return;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                addDeviceToHistory(device.getName(), "Previously Paired", "Bluetooth");
            }
        }
    }

    private void fetchWiFiHistory() {
        Context context = getContext();
        if (context == null) return;

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            String ssid = wifiInfo.getSSID();
            if (ssid != null && !ssid.equals("<unknown ssid>")) {
                ssid = ssid.replace("\"", ""); // Remove quotes around SSID
                // Log to check if we are fetching the SSID correctly
                Log.d("WiFiHistory", "Connected SSID: " + ssid);
                addDeviceToHistory(ssid, "Currently Connected", "Wi-Fi");
            } else {
                // Log if SSID is unknown
                Log.d("WiFiHistory", "Unable to fetch SSID or device is not connected to a Wi-Fi network.");
            }
        }
    }


    private void addDeviceToHistory(String deviceName, String status, String type) {
        DeviceHistory deviceHistory = new DeviceHistory(deviceName, status, type);
        deviceHistoryList.add(deviceHistory);
        adapter.notifyItemInserted(deviceHistoryList.size() - 1);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        100);
                return;
            }

            String deviceName = (device != null) ? device.getName() : "Unknown Bluetooth Device";

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                addDeviceToHistory(deviceName, "Connected", "Bluetooth");
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                addDeviceToHistory(deviceName, "Disconnected", "Bluetooth");
            }
        }
    };

    static class DeviceHistory {
        String deviceName;
        String status;
        String type;

        DeviceHistory(String deviceName, String status, String type) {
            this.deviceName = deviceName;
            this.status = status;
            this.type = type;
        }
    }

    static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
        private final List<DeviceHistory> historyList;

        HistoryAdapter(List<DeviceHistory> historyList) {
            this.historyList = historyList;
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.device_history_item, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            DeviceHistory history = historyList.get(position);
            holder.deviceName.setText(history.deviceName);
            holder.status.setText(history.status);
            holder.type.setText(history.type);
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        static class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView deviceName, status, type;

            HistoryViewHolder(View itemView) {
                super(itemView);
                deviceName = itemView.findViewById(R.id.device_name);
                status = itemView.findViewById(R.id.device_status);
                type = itemView.findViewById(R.id.device_type);
            }
        }
    }
}
