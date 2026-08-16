package com.ransomguard.api;

import com.ransomguard.store.DeviceStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private final DeviceStore deviceStore;

    public DeviceController(DeviceStore deviceStore) {
        this.deviceStore = deviceStore;
    }

    @GetMapping("/devices")
    public Object devices() {
        return deviceStore.list();
    }
}
