package com.mcmp.o11ymanager.manager.exception.agent;

import lombok.Getter;

@Getter
public class BeylaSystemRequirementException extends RuntimeException {

    private final String kernelVersion;
    private final boolean btfSupported;
    private final String requiredKernelVersion;

    public BeylaSystemRequirementException(
            String message,
            String kernelVersion,
            boolean btfSupported,
            String requiredKernelVersion) {
        super(message);
        this.kernelVersion = kernelVersion;
        this.btfSupported = btfSupported;
        this.requiredKernelVersion = requiredKernelVersion;
    }

    public BeylaSystemRequirementException(String message) {
        super(message);
        this.kernelVersion = null;
        this.btfSupported = false;
        this.requiredKernelVersion = null;
    }
}
