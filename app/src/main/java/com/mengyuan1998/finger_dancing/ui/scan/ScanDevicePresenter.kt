package com.mengyuan1998.finger_dancing.ui.scan

import android.bluetooth.BluetoothDevice
import com.mengyuan1998.finger_dancing.dagger.DeviceManager
import com.mengyuan1998.finger_dancing.ui.model.Device
import com.ncorti.myonnaise.Myonnaise
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class ScanDevicePresenter(
        override val view: ScanDeviceContract.View,
        private val myonnaise: Myonnaise,
        private val deviceManager: DeviceManager
) : ScanDeviceContract.Presenter(view) {

    internal lateinit var scanFlowable: Flowable<BluetoothDevice>

    internal var scanSubscription: Disposable? = null

    override fun create() {
        scanFlowable = myonnaise.startScan(10, TimeUnit.SECONDS)
    }

    override fun start() {
        view.wipeDeviceList()
        if (deviceManager.scannedDeviceList.isEmpty()) {
            view.showStartMessage()
        } else {
            view.populateDeviceList(deviceManager
                    .scannedDeviceList
                    .map { it -> Device(it.name, it.address) })
        }
    }

    override fun stop() {
        scanSubscription?.dispose()
        view.hideScanLoading()
    }
    override fun onScanToggleClicked() {

        if (scanSubscription?.isDisposed == false) {
            scanSubscription?.dispose()
            view.hideScanLoading()
            if (deviceManager.scannedDeviceList.isEmpty()) {
                view.showEmptyListMessage()
            }
        } else {
            view.hideEmptyListMessage()
            view.showScanLoading()
            scanSubscription = scanFlowable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        if (it !in deviceManager.scannedDeviceList) {
                            view.addDeviceToList(Device(it.name, it.address))
                            deviceManager.scannedDeviceList.add(it)
                        }
                    }, {
                        view.hideScanLoading()
                        view.showScanError()
                        if (deviceManager.scannedDeviceList.isEmpty()) {
                            view.showEmptyListMessage()
                        }
                    }, {
                        view.hideScanLoading()
                        view.showScanCompleted()
                        if (deviceManager.scannedDeviceList.isEmpty()) {
                            view.showEmptyListMessage()
                        }
                    })
        }
    }

    override fun onDeviceSelected(index: Int) {
        deviceManager.selectedIndex = index
        if (deviceManager.selectedIndex == -1) {
            return
        }
        else{
            val selectedDevice = deviceManager.scannedDeviceList[deviceManager.selectedIndex]
            if (deviceManager.myo == null) {
                deviceManager.myo = myonnaise.getMyo(selectedDevice)
            }
            deviceManager.myo?.apply {
                if (!this.isConnected()) {
                    this.connect(myonnaise.context)
                }
            }

        }
    }
}