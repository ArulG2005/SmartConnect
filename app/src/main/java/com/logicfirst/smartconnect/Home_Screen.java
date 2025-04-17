package com.logicfirst.smartconnect;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Home_Screen extends Fragment {

    private TextView wifiCount, bluetoothCount, usbCount, emptyState;
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private final List<Device> deviceList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver networkReceiver;
    private BroadcastReceiver bluetoothReceiver;
    private BroadcastReceiver usbReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;

    // NotificationManager
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "device_notification_channel";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Initialize NotificationManager
        notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Device Connection Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home__screen, container, false);

        // Initialize Views
        wifiCount = view.findViewById(R.id.wifi_count);
        bluetoothCount = view.findViewById(R.id.bluetooth_count);
        usbCount = view.findViewById(R.id.usb_count);
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView = view.findViewById(R.id.deviceRecyclerView);

        // Setup RecyclerView
        adapter = new DeviceAdapter(deviceList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            bluetoothCount.setText("0");
        }

        // Register all receivers
        registerNetworkReceiver();
        registerBluetoothReceiver();
        registerUsbReceiver();

        // Initial update
        updateDeviceStatus();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister all receivers
        unregisterReceivers();
    }

    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateDeviceStatus();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        requireContext().registerReceiver(networkReceiver, filter);

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                requireActivity().runOnUiThread(() -> updateDeviceStatus());
            }

            @Override
            public void onLost(@NonNull Network network) {
                requireActivity().runOnUiThread(() -> updateDeviceStatus());
            }
        };
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    private void registerBluetoothReceiver() {
        bluetoothReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        requireActivity().runOnUiThread(() -> updateDeviceStatus());
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        requireContext().registerReceiver(bluetoothReceiver, filter);
    }

    private void registerUsbReceiver() {
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)
                        || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    requireActivity().runOnUiThread(() -> updateDeviceStatus());
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        requireContext().registerReceiver(usbReceiver, filter);
    }

    private void unregisterReceivers() {
        try {
            if (networkReceiver != null) {
                requireContext().unregisterReceiver(networkReceiver);
            }
            if (bluetoothReceiver != null) {
                requireContext().unregisterReceiver(bluetoothReceiver);
            }
            if (usbReceiver != null) {
                requireContext().unregisterReceiver(usbReceiver);
            }
            if (networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }

    private void updateDeviceStatus() {
        deviceList.clear();
        int wifiDevices = 0;
        int btDevices = 0;
        int usbDevices = 0;

        // WiFi (Unchanged)
        if (isWifiConnected()) {
            deviceList.add(new Device("WiFi Network", "WiFi", "Strong", "Safe"));
            wifiDevices = 1;
            sendNotification("WiFi Network Connected", "You are connected to WiFi.");
        }

        // Bluetooth
        if (isBluetoothEnabled()) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    // Only add the device if it is currently connected
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        String deviceName = device.getName() != null ? device.getName() : "Unknown Bluetooth Device";
                        deviceList.add(new Device(deviceName, "Bluetooth", "Connected", "Safe", device.getAddress()));
                        btDevices++;
                        sendNotification("Bluetooth Device Connected", "Device: " + deviceName);
                    }
                }
            }
        }

        // USB (Unchanged)
        usbDevices = getUsbDeviceCount();
        if (usbDevices > 0) {
            deviceList.add(new Device("USB Device", "USB", "N/A", "Safe"));
            sendNotification("USB Device Connected", "A USB device has been connected.");
        }

        // Update counts (Unchanged)
        wifiCount.setText(String.valueOf(wifiDevices));
        bluetoothCount.setText(String.valueOf(btDevices));
        usbCount.setText(String.valueOf(usbDevices));

        // Update UI (Unchanged)
        if (deviceList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }


    private boolean isWifiConnected() {
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.getActiveNetwork());
        return capabilities != null &&
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private int getUsbDeviceCount() {
        UsbManager usbManager = (UsbManager) requireContext().getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
            return deviceMap != null ? deviceMap.size() : 0;
        }
        return 0;
    }

    private boolean deviceListContains(String deviceName) {
        for (Device device : deviceList) {
            if (device.name.equals(deviceName)) {
                return true;
            }
        }
        return false;
    }

    // Send Notification
    private void sendNotification(String title, String message) {
        Notification notification = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.logo) // Use an appropriate icon here
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    // Device Model and Adapter classes remain the same as the previous version
    static class Device {
        String name, type, status, security, macAddress;

        Device(String name, String type, String status, String security) {
            this.name = name;
            this.type = type;
            this.status = status;
            this.security = security;
            this.macAddress = "N/A";
        }

        Device(String name, String type, String status, String security, String macAddress) {
            this.name = name;
            this.type = type;
            this.status = status;
            this.security = security;
            this.macAddress = macAddress;
        }
    }

    static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
        private List<Device> deviceList;

        public DeviceAdapter(List<Device> deviceList) {
            this.deviceList = deviceList;
        }

        @NonNull
        @Override
        public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
            return new DeviceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
            Device device = deviceList.get(position);
            holder.name.setText(device.name);
            holder.type.setText(device.type);
            holder.status.setText(device.status);
            holder.security.setText(device.security);
            holder.itemView.setOnClickListener(v -> {
                showDeviceDetails(v.getContext(), device);
            });
        }

        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        private void showDeviceDetails(Context context, Device device) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(device.name)
                    .setMessage("Type: " + device.type + "\nStatus: " + device.status + "\nSecurity: " + device.security + "\nMAC Address: " + device.macAddress)
                    .setPositiveButton("OK", null)
                    .show();
        }

        static class DeviceViewHolder extends RecyclerView.ViewHolder {
            TextView name, type, status, security;

            public DeviceViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.deviceName);
                type = itemView.findViewById(R.id.deviceType);
                status = itemView.findViewById(R.id.signalStrength);
                security = itemView.findViewById(R.id.deviceSecurity);
            }
        }
    }
}
