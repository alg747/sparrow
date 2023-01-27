package com.sparrowwallet.sparrow.control;

import com.google.common.eventbus.Subscribe;
import com.sparrowwallet.drongo.psbt.PSBT;
import com.sparrowwallet.drongo.wallet.Wallet;
import com.sparrowwallet.drongo.wallet.WalletModel;
import com.sparrowwallet.sparrow.EventManager;
import com.sparrowwallet.sparrow.event.PSBTSignedEvent;
import com.sparrowwallet.sparrow.io.Device;
import com.sparrowwallet.sparrow.io.ckcard.CardApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import java.util.ArrayList;
import java.util.List;

public class DeviceSignDialog extends DeviceDialog<PSBT> {
    private static final Logger log = LoggerFactory.getLogger(DeviceSignDialog.class);

    private final Wallet wallet;
    private final PSBT psbt;

    public DeviceSignDialog(Wallet wallet, List<String> operationFingerprints, PSBT psbt) {
        super(operationFingerprints);
        this.wallet = wallet;
        this.psbt = psbt;
        EventManager.get().register(this);
        setOnCloseRequest(event -> {
            EventManager.get().unregister(this);
        });
        setResultConverter(dialogButton -> dialogButton.getButtonData().isCancelButton() ? null : psbt);
    }

    @Override
    protected List<Device> getDevices() {
        List<Device> devices = super.getDevices();

        if(CardApi.isReaderAvailable()) {
            devices = new ArrayList<>(devices);
            try {
                CardApi cardApi = new CardApi(null);
                if(cardApi.isInitialized()) {
                    Device cardDevice = new Device();
                    cardDevice.setType(WalletModel.TAPSIGNER.getType());
                    cardDevice.setModel(WalletModel.TAPSIGNER);
                    cardDevice.setNeedsPassphraseSent(Boolean.FALSE);
                    cardDevice.setNeedsPinSent(Boolean.FALSE);
                    cardDevice.setCard(true);
                    devices.add(cardDevice);
                }
            } catch(CardException e) {
                log.error("Error reading card", e);
            }
        }

        return devices;
    }

    @Override
    protected DevicePane getDevicePane(Device device, boolean defaultDevice) {
        return new DevicePane(wallet, psbt, device, defaultDevice);
    }

    @Subscribe
    public void psbtSigned(PSBTSignedEvent event) {
        if(psbt == event.getPsbt()) {
            setResult(event.getSignedPsbt());
        }
    }
}
