package org.distributedlock.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class IOUtils {

    public String getHostDetail(final String owner) {
        if (StringUtils.isBlank(owner)) {
            return getHostname();
        }
        final String envOwner = System.getenv(owner);
        return StringUtils.isBlank(envOwner) ? owner : envOwner;
    }

    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Couldn't get the hostname through InetAddress.", e);
            return System.getenv("HOSTNAME");
        }
    }
}
