package com.example.busylightkotlin

import android.Manifest
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat.startActivityForResult
import android.util.Log
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import java.io.IOException
import java.util.*
import java.util.Set
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import android.bluetooth.BluetoothSocket
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import java.io.InputStream
import java.io.OutputStream


var devices = ArrayList<BluetoothDevice>()
var devicesMap = HashMap<String, BluetoothDevice>()
var mArrayAdapter: ArrayAdapter<String>? = null
val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
var message = ""

const val REQUEST_ENABLE_BT = 0;
const val REQUEST_DISCOVER_BT = 1;

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mArrayAdapter = ArrayAdapter(this, R.layout.dialog_select_device)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter) // Don't forget to unregister during onDestroy

        // Make sure bluetooth is available. Turn on Bluetooth if it's off.
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val editText = findViewById<EditText>(R.id.editText)
        if (bluetoothAdapter == null) {
            editText.setText("no bluetooth")
            Log.i("bluetooth", "not on")

        } else if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            Log.i("bluetooth", "enabling")

        }
        editText.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                message = s.toString()
            }
        })
    }


    override fun onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    fun onVideoCall(view: View) {
        message = "n"
        changeDisplay(view)
    }

    fun onAudioCall(view: View) {

    }

    fun allClear(view: View) {
        message = "f"
        changeDisplay(view)
    }

    fun changeDisplay(view: View) {
        if (BluetoothAdapter.getDefaultAdapter() == null) {

        } else {
            devicesMap = HashMap()
            devices = ArrayList()
            mArrayAdapter!!.clear()

            val editText = findViewById<EditText>(R.id.editText)
            // message = editText.text.toString()
            for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
                devicesMap.put(device.address, device)
                devices.add(device)
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter!!.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address + "\nPaired")
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

                Log.i("info", "No fine location permissions")

                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1)
            }


            // Start discovery process
            if (BluetoothAdapter.getDefaultAdapter().startDiscovery()) {
                val dialog = SelectDeviceDialog()
                dialog.show(supportFragmentManager, "select_device")
            }
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val pairedDevice = devicesMap[device.address]
                if (pairedDevice == null) {
                    var index = -1
                    for (i in devices.indices) {
                        val tmp = devices[i]
                        if (tmp.address == device.address) {
                            index = i
                            break
                        }
                    }

                    if (index > -1) {
                        if (device.name != null) {
                            mArrayAdapter?.insert(
                                (if (device.name != null) device.name else "Unknown") + "\n" + device.address,
                                index
                            )
                        }
                    } else {
                        devices.add(device)
                        // 	Add the name and address to an array adapter to show in a ListView
                        mArrayAdapter?.add((if (device.name != null) device.name else "Unknown") + "\n" + device.address)
                    }
                }
            }
        }
    }

}

class SelectDeviceDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.activity)
        builder.setTitle("Send message to")
        builder.setAdapter(mArrayAdapter) { _, which: Int ->
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            Log.i("client", devices[which].toString())
            BluetoothClient(devices[which]).start()
        }

        return builder.create()
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
    }
}

private class BluetoothClient(device: BluetoothDevice): Thread() {

    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        var bd = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(device.address)
        bd.createInsecureRfcommSocketToServiceRecord(uuid)
    }

    override fun run() {
        // Cancel discovery because it otherwise slows down the connection.
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        try{

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                Log.i("HERE", "HERE")
                val outputStream = mmSocket?.outputStream
                try {
                    outputStream?.write(message.toByteArray())
                    outputStream?.flush()
                    Log.i("client", "Sent")
                } catch (e: Exception) {
                    Log.e("client", "Cannot send", e)
                } finally {
                    outputStream?.close()
                    mmSocket?.close()
                }
            }
        }
        catch(e:Exception) {
            Log.e("error", e.message)
            mmSocket?.close()
        }
    }

    // Closes the client socket and causes the thread to finish.
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            Log.e("client", "Could not close the client socket", e)
        }
    }
}